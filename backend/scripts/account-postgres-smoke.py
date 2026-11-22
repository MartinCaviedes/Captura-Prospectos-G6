#!/usr/bin/env python3
"""Disposable PostgreSQL migration + real users/gateway JAR test. Requires Docker, JDK21, Python cryptography."""
from pathlib import Path
import concurrent.futures
import json
import os
import secrets
import socket
import subprocess
import tempfile
import time
import urllib.request
import urllib.error
import uuid
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives import serialization

ROOT = Path(__file__).resolve().parents[1]
IMAGE = 'postgres:17.12-bookworm@sha256:40865bc877a289da75963d97d72b59633f639fc5af7a4e856bfa3714cae5b536'
NAME = 'g6-accounts-' + uuid.uuid4().hex[:12]

def docker(*args):
    return subprocess.check_output(['docker', *args], text=True, stderr=subprocess.PIPE).strip()

def sql(db, statement, success=True):
    result = subprocess.run(['docker','exec','-i',NAME,'psql','-U','postgres','-d',db,'-v','ON_ERROR_STOP=1','-Atq'], input=statement, text=True, capture_output=True)
    assert (result.returncode == 0) == success, 'Unexpected SQL result; statement/output intentionally omitted'
    return result.stdout.strip()

def free_port():
    with socket.socket() as sock:
        sock.bind(('127.0.0.1',0))
        return sock.getsockname()[1]

def run(directory):
    processes=[]
    started=False
    try:
        envfile=directory/'pg.env'
        envfile.write_text('POSTGRES_PASSWORD='+secrets.token_hex(24)+'\n')
        docker('run','--detach','--name',NAME,'--publish','127.0.0.1::5432','--tmpfs','/var/lib/postgresql/data:rw,nosuid','--env-file',str(envfile),IMAGE)
        started=True
        for _ in range(120):
            result=subprocess.run(['docker','exec',NAME,'pg_isready','-h','127.0.0.1','-U','postgres'],capture_output=True)
            if result.returncode==0: break
            time.sleep(.5)
        else: raise RuntimeError('Temporary PostgreSQL unavailable')
        port=json.loads(docker('inspect','--format','{{json .NetworkSettings.Ports}}',NAME))['5432/tcp'][0]['HostPort']
        initial=(ROOT/'users-service/db/migrations/V001__initial.sql').read_text()
        upgrade=(ROOT/'users-service/db/migrations/V002__account_access.sql').read_text()
        sql('postgres','CREATE DATABASE upgrade_test; CREATE DATABASE runtime_test;')
        sql('upgrade_test',initial)
        sql('upgrade_test',"INSERT INTO usuarios VALUES ('11111111-1111-4111-8111-111111111111','LegacyOne','one@example.invalid','Legacy One','not-a-login-hash','CAPTADOR','site-1',true),('22222222-2222-4222-8222-222222222222','LegacyTwo','two@example.invalid','Legacy Two','not-a-login-hash','SUPERVISOR','site-2',false);")
        snapshot="SELECT json_agg(x ORDER BY id)::text FROM (SELECT id,username,email,display_name,password_hash,rol,sedes_asignadas,activo FROM usuarios) x;"
        before=sql('upgrade_test',snapshot)
        sql('upgrade_test',upgrade)
        assert sql('upgrade_test',snapshot)==before, 'Migration changed original account data'
        assert sql('upgrade_test',"SELECT string_agg(status,',' ORDER BY id) FROM usuarios;")=='ACTIVE,DISABLED'
        assert sql('upgrade_test','SELECT count(*) FROM usuarios WHERE version=0 AND created_at IS NOT NULL AND updated_at IS NOT NULL;')=='2'
        sql('upgrade_test',upgrade,False)
        assert sql('upgrade_test',snapshot)==before
        print('PASS: V001 to V002 preserves identity, hashes, roles, sites and active flags; reapply fails safely',flush=True)

        # Ambiguous legacy identifiers must fail transactionally rather than merge accounts.
        sql('postgres','CREATE DATABASE conflict_test;')
        sql('conflict_test',initial)
        sql('conflict_test',"INSERT INTO usuarios VALUES ('a','CaseUser','a@example.invalid','Case One','not-a-login-hash','CAPTADOR','site-1',true),('b','caseuser','b@example.invalid','Case Two','not-a-login-hash','CAPTADOR','site-2',true);")
        sql('conflict_test',upgrade,False)
        assert sql('conflict_test','SELECT count(*) FROM usuarios;')=='2'
        assert sql('conflict_test','SELECT max(version) FROM schema_version;')=='1'
        print('PASS: duplicate legacy names roll back V002 without data loss',flush=True)

        sql('postgres','CREATE DATABASE assignment_test;')
        sql('assignment_test',initial)
        sql('assignment_test',"INSERT INTO usuarios VALUES ('a','invalid-scope','scope@example.invalid','Scope Test','not-a-login-hash','CAPTADOR','',true);")
        sql('assignment_test',upgrade,False)
        assert sql('assignment_test','SELECT count(*) FROM usuarios;')=='1'
        assert sql('assignment_test','SELECT max(version) FROM schema_version;')=='1'
        print('PASS: active legacy account without valid assignments blocks migration without granting permissions',flush=True)

        sql('runtime_test',initial); sql('runtime_test',upgrade)
        dbpassword=secrets.token_hex(24)
        sql('runtime_test',f"CREATE ROLE account_runtime LOGIN PASSWORD '{dbpassword}'; GRANT CONNECT ON DATABASE runtime_test TO account_runtime; GRANT USAGE ON SCHEMA public TO account_runtime; GRANT SELECT,INSERT,UPDATE ON usuarios TO account_runtime; GRANT SELECT,INSERT ON account_access_audit TO account_runtime;")
        keys=rsa.generate_private_key(public_exponent=65537,key_size=2048)
        private=directory/'private.pem'; public=directory/'public.pem'
        private.write_bytes(keys.private_bytes(serialization.Encoding.PEM,serialization.PrivateFormat.PKCS8,serialization.NoEncryption()))
        public.write_bytes(keys.public_key().public_bytes(serialization.Encoding.PEM,serialization.PublicFormat.SubjectPublicKeyInfo))
        adminpassword=secrets.token_hex(16)
        userport,gatewayport=free_port(),free_port()
        settings=dict(os.environ,SPRING_PROFILES_ACTIVE='prod',IDENTITY_JDBC_URL=f'jdbc:postgresql://127.0.0.1:{port}/runtime_test',
            IDENTITY_DB_USER='account_runtime',IDENTITY_DB_PASSWORD=dbpassword,JWT_PRIVATE_KEY=private.as_uri(),JWT_PUBLIC_KEY=public.as_uri(),
            BOOTSTRAP_ADMIN_USERNAME='account-admin',BOOTSTRAP_ADMIN_EMAIL='admin@example.invalid',BOOTSTRAP_ADMIN_DISPLAY_NAME='Account Admin',BOOTSTRAP_ADMIN_PASSWORD=adminpassword,
            USERS_SERVICE_URL=f'http://127.0.0.1:{userport}',LOGIN_RATE_LIMIT_REQUESTS='500')
        java=str(Path(os.environ['JAVA_HOME'])/'bin/java.exe') if os.name=='nt' and 'JAVA_HOME' in os.environ else 'java'
        jar=ROOT/'users-service/target/users-service-0.0.1-SNAPSHOT.jar'
        with (directory/'bootstrap.log').open('wb') as log:
            result=subprocess.run([java,'-jar',str(jar),'--bootstrap-admin'],env=settings,stdout=log,stderr=subprocess.STDOUT,timeout=40)
            assert result.returncode==0, 'V002 bootstrap failed'
        for key in list(settings):
            if key.startswith('BOOTSTRAP_'): settings.pop(key)

        def start(service,serviceport):
            env=dict(settings,PORT=str(serviceport))
            if service=='api-gateway':env.pop('JWT_PRIVATE_KEY',None)
            with (directory/(service+'.log')).open('ab') as log:
                proc=subprocess.Popen([java,'-jar',str(ROOT/service/'target'/f'{service}-0.0.1-SNAPSHOT.jar')],env=env,stdout=log,stderr=subprocess.STDOUT)
            processes.append(proc)
            for _ in range(120):
                assert proc.poll() is None, service+' stopped during startup'
                try:
                    with urllib.request.urlopen(f'http://127.0.0.1:{serviceport}/actuator/health/readiness',timeout=1) as response:
                        if response.status==200:return proc
                except (urllib.error.URLError,TimeoutError):pass
                time.sleep(.5)
            raise RuntimeError(service+' readiness timeout')

        userprocess=start('users-service',userport); start('api-gateway',gatewayport)
        def request(method,path,body=None,token=None,expected=200):
            headers={'Content-Type':'application/json'}
            if token:headers['Authorization']='Bearer '+token
            req=urllib.request.Request(f'http://127.0.0.1:{gatewayport}/api/v1'+path,data=json.dumps(body).encode() if body is not None else None,headers=headers,method=method)
            try:response=urllib.request.urlopen(req,timeout=15)
            except urllib.error.HTTPError as error:response=error
            with response:
                assert response.status==expected,f'{method} {path}: {response.status}, expected {expected}'
                return json.loads(response.read())

        admin=request('POST','/auth/login',{'username':'account-admin','password':adminpassword})['accessToken']
        payload={'email':'pending@example.invalid','displayName':'Persona ficticia','password':'ClaveDemo-2026'}
        pending=request('POST','/auth/register',payload,expected=201)
        assert pending['status']=='PENDING' and 'accessToken' not in pending
        request('POST','/auth/login',{'username':payload['email'],'password':payload['password']},expected=403)
        id=pending['userId']; path='/users/'+id+'/access'
        access={'status':'ACTIVE','role':'CAPTADOR','siteIds':['site-1'],'expectedVersion':0}
        with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
            # Capture codes explicitly so both concurrent outcomes are verified.
            def approve():
                req=urllib.request.Request(f'http://127.0.0.1:{gatewayport}/api/v1'+path,data=json.dumps(access).encode(),headers={'Content-Type':'application/json','Authorization':'Bearer '+admin},method='PATCH')
                try:response=urllib.request.urlopen(req,timeout=15)
                except urllib.error.HTTPError as error:response=error
                with response:return response.status
            codes=sorted(pool.map(lambda _:approve(),range(2)))
        assert codes==[200,409],codes
        assert sql('runtime_test',f"SELECT count(*) FROM account_access_audit WHERE target_id='{id}';")=='1'
        token=request('POST','/auth/login',{'username':payload['email'],'password':payload['password']})['accessToken']
        request('GET','/users',token=token,expected=403)
        assert request('GET','/me',token=token)['id']==id
        assert request('GET','/admin/sites',token=admin)[0]['id']=='site-1'
        print('PASS: real gateway, prod/validate, admin bootstrap, pending registration, approval race, login and authorization',flush=True)
        userprocess.terminate(); userprocess.wait(timeout=20); processes.remove(userprocess)
        start('users-service',userport)
        assert request('GET','/users/'+id,token=admin)['version']==1
        request('PATCH','/users/'+id+'/estado',{'activo':False,'expectedVersion':1},admin)
        request('POST','/auth/login',{'username':payload['email'],'password':payload['password']},expected=403)
        assert request('GET','/me',token=token)['status']=='DISABLED'
        assert sql('runtime_test',f"SELECT count(*) FROM account_access_audit WHERE target_id='{id}';")=='2'
        print('PASS: PostgreSQL persistence after JAR restart and disabled account without deleting data',flush=True)
    finally:
        for proc in reversed(processes):
            if proc.poll() is None:
                proc.terminate()
                try:proc.wait(timeout=20)
                except subprocess.TimeoutExpired:proc.kill();proc.wait()
        if started:docker('rm','--force',NAME)

if __name__=='__main__':
    with tempfile.TemporaryDirectory(prefix='g6-account-pg-') as temp:
        run(Path(temp))
