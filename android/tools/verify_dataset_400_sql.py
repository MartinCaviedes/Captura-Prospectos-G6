#!/usr/bin/env python3
"""Control estructural de SQLite host, sin cifrado ni repositorios Android.

No certifica RNF02 ni mide rendimiento del teléfono. La prueba Android
Dataset400StorageTest comprueba el flujo real y registra sus latencias.
"""
from pathlib import Path
import json
import sqlite3
import uuid

ROOT = Path(__file__).resolve().parents[1]
SCHEMA = ROOT / 'app/schemas/io.github.grupo6.capturaprospectos.data.local.CaptureDatabase/4.json'

def ident(key):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, 'g6-host-structural/' + key))

def rut(index):
    body = str(10_000_000 + index)
    check = 11 - sum(int(digit) * (2 + i % 6) for i, digit in enumerate(reversed(body))) % 11
    return body + '-' + ('0' if check == 11 else 'K' if check == 10 else str(check))

def insert(db, table, values):
    columns = list(values)
    db.execute(f"INSERT INTO {table} ({','.join(columns)}) VALUES ({','.join('?' for _ in columns)})", list(values.values()))

def run_structural_check():
    with sqlite3.connect(':memory:') as db:
        db.execute('PRAGMA foreign_keys=ON')
        for entity in json.loads(SCHEMA.read_text(encoding='utf-8'))['database']['entities']:
            db.execute(entity['createSql'].replace('${TABLE_NAME}', entity['tableName']))
            for index in entity.get('indices', []):
                db.execute(index['createSql'].replace('${TABLE_NAME}', entity['tableName']))
        insert(db,'consent_text',dict(version='g6-host-fictitious-v1',text='Texto ficticio de control estructural'))
        timestamp='2026-10-10T15:00:00Z'
        for i in range(400):
            phase=i//200
            state='CANCELLED' if i%20 in (0,1) else 'NEEDS_CONTEXT' if i%20 in (2,3,4) else 'DRAFT' if i%20 in (5,6) else 'READY'
            row=dict(id=ident(f'capture-{i}'),revision=2 if state=='CANCELLED' else 1,state=state,
                ownerId=ident(f'captor-{phase}'),siteId=ident(f'site-{phase}'),eventId=ident(f'event-{phase}'),deviceId=ident('device'),
                capturedAt=timestamp,eventName='Evento ficticio',eventTimeZone='America/Santiago',
                fullName='Persona Ficticia Demostracion',givenNames='Persona Ficticia',familyNames='Demostracion',
                rut=rut(i%200),email=f'demo{i}@example.invalid',phone='+56900000000',birthDate='2000-01-01',
                residenceCommune='Comuna ficticia',currentActivity='SECONDARY_GRADUATE',
                consentVersion='g6-host-fictitious-v1',acceptedAt=timestamp,recordedBy=ident(f'captor-{phase}'),acceptanceMode='DIRECT',contractVersion='F2027_1')
            if state in ('READY','CANCELLED'):
                row.update(locationSource='DEVICE',latitude=-33.0,longitude=-71.0,measuredAt=timestamp,accuracyMeters=20.0)
            insert(db,'captures',row)
            for priority in range(1,3 if i%2==0 else 2):
                insert(db,'capture_interests',dict(captureId=row['id'],programId=ident(f'program-{priority}'),catalogVersion='g6-host-fictitious-catalog-v1',
                    programName=f'Programa ficticio {priority}',priority=priority,section='DEMO_PHYSICAL' if priority==1 else 'DEMO_ONLINE'))
            if state in ('READY','NEEDS_CONTEXT'):
                insert(db,'active_registration_keys',dict(eventId=row['eventId'],rut=row['rut'],captureId=row['id']))
            insert(db,'local_audit',dict(id=ident(f'audit-{i}-1'),captureId=row['id'],actorId=row['ownerId'],occurredAt=timestamp,action='CREATED',resultingRevision=1))
            if state=='CANCELLED':
                insert(db,'local_audit',dict(id=ident(f'audit-{i}-2'),captureId=row['id'],actorId=row['ownerId'],occurredAt=timestamp,
                    action='CANCELLED',previousRevision=1,resultingRevision=2,reasonText='Motivo ficticio',changedFields='["state"]'))
        assert db.execute('PRAGMA foreign_key_check').fetchall()==[]
        counts={table:db.execute(f'SELECT COUNT(*) FROM {table}').fetchone()[0] for table in ('captures','capture_interests','active_registration_keys','local_audit')}
        assert counts==dict(captures=400,capture_interests=600,active_registration_keys=320,local_audit=440),counts
        states=dict(db.execute('SELECT state,COUNT(*) FROM captures GROUP BY state'))
        assert states==dict(READY=260,NEEDS_CONTEXT=60,DRAFT=40,CANCELLED=40),states
        seen=[];after='~'
        while True:
            page=db.execute('SELECT id FROM captures WHERE ownerId=? AND id<? ORDER BY capturedAt DESC,id DESC LIMIT 30',(ident('captor-0'),after)).fetchall()
            if not page: break
            seen.extend(row[0] for row in page);after=page[-1][0]
        assert len(seen)==len(set(seen))==200
        assert seen==sorted(seen,reverse=True)
        assert db.execute("SELECT COUNT(*) FROM captures WHERE state!='DRAFT'").fetchone()[0]==360
        print(json.dumps(dict(control='host SQLite v4 structural smoke only',counts=counts,states=states,captor_cursor_rows=len(seen),foreign_key_errors=0),sort_keys=True))

if __name__=='__main__':
    run_structural_check()
