"""Valida OpenAPI, ejemplos, composición e inventario de controladores; sin HTTP."""
import argparse
import json
from pathlib import Path
import re
import sys

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--deps',type=Path)
args=parser.parse_args()
if args.deps:
    sys.path.insert(0,str(args.deps.resolve()))
from openapi_spec_validator import validate_spec
from jsonschema import Draft202012Validator, FormatChecker
from build_backend_contract import build

root=Path(__file__).resolve().parents[1]
specs={name:json.loads((root/f'docs/openapi/{name}.openapi.json').read_text(encoding='utf-8'))
       for name in ('accounts','prospects','backend')}
for name,spec in specs.items():
    validate_spec(spec)
    ids=[]
    for path,item in spec['paths'].items():
        for method,operation in item.items():
            if method not in {'get','post','put','patch','delete'}: continue
            ids.append(operation['operationId'])
            public=path in {'/auth/register','/auth/login'} or path.startswith('/actuator/health')
            assert operation['security']==([] if public else [{'bearerAuth':[]}]), (name,path,'security')
    assert len(ids)==len(set(ids)), (name,'duplicate operationId')
    examples=0
    for schema in spec['components']['schemas'].values():
        validator=Draft202012Validator(dict(schema,components=spec['components']),format_checker=FormatChecker())
        for example in schema.get('examples',[]):
            validator.validate(example);examples+=1
    print(f'{name}: OpenAPI 3.1 válido, {len(ids)} operaciones, {examples} ejemplos.')
assert specs['backend']==build(), 'Regenerar contrato unificado con tools/build_backend_contract.py'

# Inventario acotado al estilo de anotaciones usado en los controladores actuales.
# Fallará si se añade una ruta o cambia el estilo; revisar el contrato en ese caso.
implemented=set()
for path in (root/'backend').glob('*/src/main/java/**/controller/*Controller.java'):
    source=path.read_text(encoding='utf-8')
    base=re.search(r'@RequestMapping\("([^\"]*)"\)',source)
    prefix=base.group(1) if base else ''
    for match in re.finditer(r'@(Get|Post|Put|Patch|Delete)Mapping(?:\("([^\"]*)"\))?',source):
        route=prefix+(match.group(2) or '')
        assert route.startswith('/api/v1/'), (path,route,'revisar inventario')
        implemented.add((match.group(1).lower(),route.removeprefix('/api/v1')))
documented={(method,path) for path,item in specs['backend']['paths'].items()
            for method in item if method in {'get','post','put','patch','delete'} and not path.startswith('/actuator/')}
assert implemented==documented, dict(missing=sorted(implemented-documented),future=sorted(documented-implemented))
print(f'Inventario: {len(implemented)} operaciones de controladores coinciden; 3 sondas del gateway adicionales.')
print('La ruta interna /admin/status de prospectos está eclipsada por usuarios en el gateway; se documenta una sola operación pública.')
print('Sin llamadas HTTP; validación funcional del backend y del navegador por separado.')
