"""Combine los dos contratos fuente sin duplicar su mantenimiento. --check no escribe."""
import argparse
from copy import deepcopy
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def build():
    folder = ROOT / 'docs/openapi'
    accounts = json.loads((folder / 'accounts.openapi.json').read_text(encoding='utf-8'))
    prospects = json.loads((folder / 'prospects.openapi.json').read_text(encoding='utf-8'))
    result = deepcopy(accounts)
    result['info'] = dict(title='Captura de Prospectos G6 · API E2', version='0.3.0-e2',
        description='API implementada de identidad, administración, prospectos y sincronización. '
        'Para probar: POST /auth/login con una cuenta habilitada, copiar accessToken y pegar solo el token en Authorize. '
        'Las operaciones modifican los datos del entorno seleccionado. La autorización se conserva solo en memoria de esta página. '
        'No hay registro automático de administrador, refresh, recuperación de contraseña, SSO institucional ni CRUD remoto de eventos. '
        'Las rutas futuras del SRS no se declaran disponibles. Ejemplos exclusivamente ficticios.')
    for path, item in prospects['paths'].items():
        if path in result['paths']:
            raise ValueError('Ruta duplicada: ' + path)
        result['paths'][path] = item
    for category, entries in prospects['components'].items():
        target = result['components'].setdefault(category, {})
        for name, value in entries.items():
            if name in target and target[name] != value:
                raise ValueError('Componente incompatible: ' + name)
            target[name] = value
    result['tags'] += prospects['tags'] + [dict(name='Diagnóstico')]
    result['components']['schemas']['TechnicalStatus'] = {
        'type':'object', 'additionalProperties':False,
        'properties':{'service':{'type':'string'},'status':{'type':'string','const':'ok'}},
        'required':['service','status']}
    result['components']['schemas']['ProspectsIdentityStatus'] = {
        'type':'object', 'additionalProperties':False,
        'properties':{'service':{'type':'string','const':'prospects-service'},
            'subject':{'type':'string'},'roles':{'type':'array','items':{'type':'string'}}},
        'required':['service','subject','roles']}
    for path, operation, schema, roles in [('/admin/status','getAdminStatus','TechnicalStatus',['ADMIN']),
            ('/prospects/status','getProspectsStatus','ProspectsIdentityStatus',['CAPTADOR','SUPERVISOR','ADMIN'])]:
        result['paths'][path]={'get':dict(operationId=operation,summary='Comprobar acceso técnico',tags=['Diagnóstico'],
            description='En el gateway /admin/status corresponde a users-service; la ruta homónima interna de prospects-service queda oculta por el enrutamiento.',
            security=[{'bearerAuth':[]}], **{'x-required-roles':roles}, responses={
                '200':{'description':'Acceso correcto','content':{'application/json':{'schema':{'$ref':'#/components/schemas/'+schema}}}},
                '401':{'description':'JWT requerido o inválido; gateway puede responder sin cuerpo.'},
                '403':{'description':'Rol no autorizado.'},'503':{'description':'Servicio no disponible.'},'504':{'description':'Timeout del servicio.'}})}
    for suffix, operation in [('', 'getGatewayHealth'),('/liveness','getGatewayLiveness'),('/readiness','getGatewayReadiness')]:
        result['paths']['/actuator/health'+suffix]={'get':dict(operationId=operation,summary='Consultar salud del gateway',
            description='Estado del proceso del gateway, sin detalles; no acredita disponibilidad de todas las dependencias.',
            tags=['Diagnóstico'],servers=[{'url':'/'}],security=[],responses={
                code:{'description':description,'content':{'application/json':{'schema':{
                    'type':'object','properties':{'status':{'type':'string'}},'required':['status']}}}}
                for code,description in [('200','Proceso disponible'),('503','Proceso no disponible para esta sonda')]})}
    return result


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check',action='store_true')
    args=parser.parse_args()
    rendered=json.dumps(build(),ensure_ascii=False,indent=2)+'\n'
    path=ROOT/'docs/openapi/backend.openapi.json'
    if args.check:
        if not path.is_file() or path.read_text(encoding='utf-8') != rendered:
            parser.exit(1,'backend.openapi.json desactualizado; ejecutar tools/build_backend_contract.py\n')
        print('Contrato unificado coincide con sus fuentes.')
    else:
        path.write_text(rendered,encoding='utf-8')
        print('Generado docs/openapi/backend.openapi.json')


if __name__=='__main__':
    main()
