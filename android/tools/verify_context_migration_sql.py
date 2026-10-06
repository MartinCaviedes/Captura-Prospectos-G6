"""Verifica migración aditiva de contexto en SQLite del host; Android sigue siendo necesario."""
from pathlib import Path
import json
import runpy
import sqlite3

ROOT = Path(__file__).resolve().parents[1]
previous = runpy.run_path(str(ROOT / 'tools/verify_migration_sql.py'))
schema = json.loads((ROOT / 'app/schemas/io.github.grupo6.capturaprospectos.data.local.CaptureDatabase/3.json').read_text())['database']['entities']
source = (ROOT / 'app/src/main/java/io/github/grupo6/capturaprospectos/data/local/CaptureMigration2To3.kt').read_text(encoding='utf-8')
statements = [json.loads(line.strip()[11:-1]) for line in source.splitlines() if line.strip().startswith('db.execSQL(')]
assert statements and all(sql.startswith('CREATE ') for sql in statements)

def fixture_v2():
    db = previous['fixture']()
    db.execute('BEGIN')
    for sql in previous['statements']: db.execute(sql)
    db.execute("INSERT INTO capture_site_preferences VALUES ('capture-1','demo-site','Sede ficticia','demo-v2')")
    db.commit()
    return db

def all_old_rows(db):
    return {e['tableName']: db.execute('SELECT * FROM `' + e['tableName'] + '` ORDER BY 1,2').fetchall() for e in previous['new']}

with fixture_v2() as db:
    before = all_old_rows(db)
    db.execute('BEGIN')
    for sql in statements: db.execute(sql)
    db.commit()
    assert all_old_rows(db) == before
    assert not db.execute('PRAGMA foreign_key_check').fetchall()
    expected = sqlite3.connect(':memory:')
    previous['create_schema'](expected, schema)
    for e in schema:
        assert previous['shape'](db, e['tableName']) == previous['shape'](expected, e['tableName']), e['tableName']
        if e['tableName'].startswith('demo_'):
            assert db.execute('SELECT COUNT(*) FROM `' + e['tableName'] + '`').fetchone()[0] == 0
    expected.close()

with fixture_v2() as db:
    before = all_old_rows(db)
    db.execute('BEGIN')
    try:
        for sql in statements[:len(statements)//2]: db.execute(sql)
        db.execute('INSERT INTO nonexistent_context_failure_test VALUES (1)')
    except sqlite3.DatabaseError: db.rollback()
    else: raise AssertionError('Fallo no inyectado')
    assert all_old_rows(db) == before
    assert not db.execute("SELECT name FROM sqlite_master WHERE name LIKE 'demo_%'").fetchall()

print('OK: SQLite host 1->2->3; filas v2 intactas, esquema v3/FK/indices, sin seed automatico, rollback. Android v3 pendiente.')
