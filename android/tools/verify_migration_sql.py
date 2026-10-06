"""Comprobación complementaria en SQLite del host; no sustituye Room/SQLCipher en Android.
Ejecutar desde cualquier directorio con Python 3, sin dependencias externas.
"""
from pathlib import Path
import json
import re
import sqlite3

ROOT = Path(__file__).resolve().parents[1]
SCHEMAS = ROOT / 'app/schemas/io.github.grupo6.capturaprospectos.data.local.CaptureDatabase'
old = json.loads((SCHEMAS / '1.json').read_text())['database']['entities']
new = json.loads((SCHEMAS / '2.json').read_text())['database']['entities']
source = (ROOT / 'app/src/main/java/io/github/grupo6/capturaprospectos/data/local/CaptureMigration1To2.kt').read_text(encoding='utf-8')
statements = [json.loads(line.strip()[11:-1]) for line in source.splitlines() if line.strip().startswith('db.execSQL(')]
assert statements and all('TEMP TABLE' not in s for s in statements), 'Copias deben permanecer dentro de la base cifrada'

def create_schema(db, entities):
    for e in entities:
        db.execute(e['createSql'].replace('${TABLE_NAME}', e['tableName']))
        for index in e.get('indices', []):
            db.execute(index['createSql'].replace('${TABLE_NAME}', e['tableName']))

def fixture():
    db = sqlite3.connect(':memory:')
    db.execute('PRAGMA foreign_keys=ON')
    create_schema(db, old)
    db.execute("INSERT INTO consent_text VALUES ('demo-v1', 'Consentimiento ficticio')")
    entity = next(e for e in old if e['tableName'] == 'captures')
    columns = [f['columnName'] for f in entity['fields']]
    for i, state in enumerate(['DRAFT', 'READY', 'NEEDS_CONTEXT']):
        values = {f['columnName']: (7 if f['affinity'] == 'INTEGER' else 'dato-ficticio') if f.get('notNull', False) else None for f in entity['fields']}
        values.update(id=f'capture-{i}', eventId=f'event-{i}', state=state, consentVersion='demo-v1', fullName='Nombre histórico sin dividir', educationLevel='SECONDARY')
        db.execute('INSERT INTO captures (' + ','.join(columns) + ') VALUES (' + ','.join('?' for _ in columns) + ')', [values[c] for c in columns])
        for j in range(4):
            db.execute('INSERT INTO capture_interests VALUES (?,?,?,?,?,?)', (f'capture-{i}', f'program-{j}', f'offering-{j}', 'old-demo', 'Programa ficticio', None))
        if i:
            db.execute('INSERT INTO active_registration_keys VALUES (?,?,?)', (f'event-{i}', 'dato-ficticio', f'capture-{i}'))
        db.execute('INSERT INTO local_audit VALUES (?,?,?,?,?,?)', (f'audit-{i}', f'capture-{i}', 'actor-demo', '2026-10-06T15:00:00Z', 'CREATED', 7))
    db.commit()
    return db

def snapshot(db):
    return {e['tableName']: db.execute('SELECT ' + ','.join('`'+f['columnName']+'`' for f in e['fields']) + ' FROM `' + e['tableName'] + '` ORDER BY 1,2').fetchall() for e in old}

def shape(db, table):
    indices = []
    for idx in db.execute(f'PRAGMA index_list(`{table}`)'):
        if idx[3] != 'pk':
            indices.append((idx[1], idx[2], tuple(row[2] for row in db.execute(f'PRAGMA index_info(`{idx[1]}`)'))))
    return (db.execute(f'PRAGMA table_info(`{table}`)').fetchall(), db.execute(f'PRAGMA foreign_key_list(`{table}`)').fetchall(), sorted(indices))

with fixture() as db:
    before = snapshot(db)
    db.execute('BEGIN')
    for sql in statements: db.execute(sql)
    assert db.execute('PRAGMA foreign_key_check').fetchall() == []
    db.commit()
    assert snapshot(db) == before, 'Pérdida o alteración de datos v1'
    assert db.execute('SELECT contractVersion, givenNames, birthDate, currentActivity FROM captures').fetchall() == [('LEGACY_1', None, None, None)] * 3
    assert db.execute('SELECT COUNT(*) FROM capture_interests WHERE priority IS NULL AND offeringId IS NOT NULL').fetchone()[0] == 12
    assert db.execute('SELECT COUNT(*) FROM capture_site_preferences').fetchone()[0] == 0
    expected = sqlite3.connect(':memory:')
    create_schema(expected, new)
    for e in new: assert shape(db, e['tableName']) == shape(expected, e['tableName']), e['tableName']
    expected.close()
    assert not db.execute("SELECT name FROM sqlite_master WHERE name LIKE 'g6_migration_%'").fetchall()

with fixture() as db:
    before = snapshot(db)
    db.execute('BEGIN')
    try:
        for sql in statements[:len(statements)//2]: db.execute(sql)
        db.execute('INSERT INTO nonexistent_for_failure_test VALUES (1)')
    except sqlite3.DatabaseError:
        db.rollback()
    else: raise AssertionError('Fallo no inyectado')
    assert snapshot(db) == before, 'Rollback no conservó v1'
    assert not db.execute("SELECT name FROM sqlite_master WHERE name LIKE 'g6_migration_%'").fetchall()

print('OK: SQLite host; conservación v1, esquema v2/FK/índices, campos desconocidos y rollback. Android/SQLCipher pendiente.')
