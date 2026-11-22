"""Valida respuestas reales contra OpenAPI. Solo fixture local efímera, nunca producción."""
import argparse
from copy import deepcopy
import json
from pathlib import Path
import sys
import urllib.error
import urllib.parse
import urllib.request
from uuid import uuid4

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--isolated-fixture',action='store_true',required=True)
parser.add_argument('--base-url',default='http://127.0.0.1:18080')
parser.add_argument('--deps',type=Path)
args=parser.parse_args()
url=urllib.parse.urlsplit(args.base_url)
if url.scheme!='http' or url.hostname not in {'127.0.0.1','localhost'} or url.port!=18080 or url.path not in {'','/'} or url.username or url.password or url.query or url.fragment:
    parser.error('Se requiere la fixture aislada HTTP de loopback en puerto 18080.')
if args.deps:sys.path.insert(0,str(args.deps.resolve()))
from jsonschema import Draft202012Validator, FormatChecker, ValidationError
spec=json.loads((Path(__file__).resolve().parents[1]/'docs/openapi/backend.openapi.json').read_text(encoding='utf-8'))
seen=set();calls=0

def call(method,template,body=None,token=None,expected=200,id=None,query=''):
    global calls
    path=template.replace('{id}',id) if id else template
    endpoint=args.base_url.rstrip('/')+('' if path.startswith('/actuator/') else '/api/v1')+path+query
    headers={'Accept':'application/json'}
    if body is not None:headers['Content-Type']='application/json'
    if token:headers['Authorization']='Bearer '+token
    request=urllib.request.Request(endpoint,data=json.dumps(body).encode() if body is not None else None,headers=headers,method=method.upper())
    try:response=urllib.request.urlopen(request,timeout=10)
    except urllib.error.HTTPError as error:response=error
    with response:status=response.status;raw=response.read()
    assert status==expected,(method,template,status,expected)
    operation=spec['paths'][template][method]
    documented=operation['responses'][str(status)]
    data=json.loads(raw) if raw else None
    if data is not None and 'application/json' in documented.get('content',{}):
        schema=documented['content']['application/json']['schema']
        try:
            Draft202012Validator(dict(schema,components=spec['components']),format_checker=FormatChecker()).validate(data)
        except ValidationError as error:
            raise AssertionError((method,template,status,'Respuesta fuera del esquema',list(error.absolute_path))) from None
    seen.add((method,template));calls+=1
    return data

for suffix in ('','/liveness','/readiness'):call('get','/actuator/health'+suffix)
admin=call('post','/auth/login',{'username':'demo-admin','password':'test-admin-password-only'})['accessToken']
captor=call('post','/auth/login',{'username':'captor-a','password':'test-captor-password-only'})
token=captor['accessToken']
supervisor=call('post','/auth/login',{'username':'supervisor','password':'test-admin-password-only'})['accessToken']
call('get','/me',token=admin);call('get','/admin/status',token=admin);call('get','/admin/sites',token=admin)
call('get','/prospects/status',token=token)
call('get','/users',token=admin)
registration={'displayName':'Persona ficticia API','email':str(uuid4())+'@example.invalid','password':'Clave-ficticia-2026'}
pending=call('post','/auth/register',registration,expected=201)['userId']
call('get','/users/{id}',token=admin,id=pending)
call('patch','/users/{id}/access',{'expectedVersion':0,'status':'ACTIVE','role':'CAPTADOR','siteIds':['site-1']},token=admin,id=pending)
call('patch','/users/{id}/estado',{'expectedVersion':1,'activo':False},token=admin,id=pending)
call('post','/users',{'username':'smoke-'+uuid4().hex,'email':str(uuid4())+'@example.invalid','displayName':'Cuenta ficticia administrada',
    'password':'Clave-ficticia-2026','rol':'CAPTADOR','sedesAsignadas':'site-1'},token=admin,expected=201)
sample=deepcopy(spec['components']['schemas']['CreateProspectRequest']['examples'][0]);sample['eventId']=str(uuid4())
prospect=call('post','/prospects',sample,token=token,expected=201)['id']
call('get','/prospects',token=token)
call('get','/prospects/{id}',token=token,id=prospect)
call('put','/prospects/{id}',{k:sample[k] for k in ('givenNames','familyNames','email','phone')},token=token,id=prospect)
call('post','/prospects/{id}/cancel',{'reason':'Motivo ficticio'},token=token,id=prospect,expected=403)
call('post','/prospects/{id}/cancel',{'reason':'Motivo ficticio'},token=supervisor,id=prospect)
sync=deepcopy(spec['components']['schemas']['CaptureSyncRequest']['examples'][0])
sync['registration']['eventId']=str(uuid4());sync['deviceId']=str(uuid4());sync['ownerId']=captor['userId']
local=str(uuid4())
ack=call('put','/prospects/sync/{id}',sync,token=token,id=local)
assert ack==call('put','/prospects/sync/{id}',sync,token=token,id=local)
changed=deepcopy(sync);changed['registration']['givenNames']='Otra persona ficticia'
error=call('put','/prospects/sync/{id}',changed,token=token,id=local,expected=409)
assert error['code']=='SYNC_REPLAY_CONFLICT'
sync.update(sourceRevision=2,expectedRevision=1,state='CANCELLED',reason='Anulación local ficticia')
call('put','/prospects/sync/{id}',sync,token=token,id=local)
call('get','/prospects',expected=401)
call('post','/auth/logout',token=token)
expected={(method,path) for path,item in spec['paths'].items() for method in item if method in {'get','post','put','patch'}}
assert seen==expected,(expected-seen,seen-expected)
print(f'Fixture aislada: {calls} respuestas verificadas; {len(seen)} operaciones OpenAPI recorridas. Tokens no registrados.')
