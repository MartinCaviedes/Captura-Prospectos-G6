"""Validate a design contract and fixtures, without contacting an application server."""
from pathlib import Path
import argparse
import json
import sys

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--deps', type=Path, help='Optional directory containing openapi-spec-validator==0.7.2 and dependencies')
args = parser.parse_args()
if args.deps:
    sys.path.insert(0, str(args.deps.resolve()))

from openapi_spec_validator import validate_spec
from jsonschema import Draft202012Validator, FormatChecker

root = Path(__file__).resolve().parents[1]
spec = json.loads((root / 'docs/openapi/accounts.openapi.json').read_text(encoding='utf-8'))
fixtures = json.loads((root / 'docs/fixtures/accounts/cases.json').read_text(encoding='utf-8'))
validate_spec(spec)
operations = [op for path in spec['paths'].values() for method, op in path.items() if method in {'get', 'post', 'patch'}]
ids = [op['operationId'] for op in operations]
assert len(ids) == len(set(ids)), 'Duplicate operationId'
for path, item in spec['paths'].items():
    for method, op in item.items():
        if method not in {'get', 'post', 'patch'}:
            continue
        public = path in {'/auth/register', '/auth/login'}
        assert op['security'] == ([] if public else [{'bearerAuth': []}]), (path, 'security')
        if path.startswith(('/users', '/admin')):
            assert op['x-required-roles'] == ['ADMIN']

checker = FormatChecker()
def validator(name):
    schema = {'$ref': '#/components/schemas/' + name, 'components': spec['components']}
    return Draft202012Validator(schema, format_checker=checker)

names = set()
business = 0
for case in fixtures['cases']:
    assert case['name'] not in names, 'Duplicate fixture'
    names.add(case['name'])
    errors = list(validator(case['schema']).iter_errors(case['payload']))
    assert (not errors) == case['validSchema'], (case['name'], [e.message for e in errors])
    if case['kind'] == 'business':
        business += 1
        assert case['expectedStatus'] >= 400 and case['expectedCode'] and case['rule']

examples = 0
for name, schema in spec['components']['schemas'].items():
    for example in schema.get('examples', []):
        validator(name).validate(example)
        examples += 1
print(f'OpenAPI 3.1: valid; {len(operations)} operations; {examples} schema examples verified.')
print(f'Fixtures: {len(names)} schema expectations verified, including {business} business scenarios with structurally valid input.')
print('No HTTP calls. Business authorization, DB state, UTF-8 byte limits, concurrency and actual service responses are NOT verified.')
