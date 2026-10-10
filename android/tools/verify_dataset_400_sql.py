#!/usr/bin/env python3
"""Comprobación de rendimiento y consistencia con dataset de 400 registros sintéticos (RNF02).
Ejecutar desde cualquier directorio con Python 3. No requiere dependencias externas.
"""
from pathlib import Path
import json
import sqlite3
import time
import uuid

ROOT = Path(__file__).resolve().parents[1]
SCHEMAS = ROOT / 'app/schemas/io.github.grupo6.capturaprospectos.data.local.CaptureDatabase'
schema_v3 = json.loads((SCHEMAS / '3.json').read_text())['database']['entities']

def create_schema(db, entities):
    for e in entities:
        db.execute(e['createSql'].replace('${TABLE_NAME}', e['tableName']))
        for index in e.get('indices', []):
            db.execute(index['createSql'].replace('${TABLE_NAME}', e['tableName']))

def run_dataset_benchmark():
    db = sqlite3.connect(':memory:')
    db.execute('PRAGMA foreign_keys=ON')
    create_schema(db, schema_v3)

    # Consentimiento
    db.execute("INSERT INTO consent_text VALUES ('demo-2026-v1', 'Consentimiento demo F2027')")

    # Catálogo y contextos
    db.execute("INSERT INTO demo_catalog_versions VALUES ('cat-1', 'SYNTHETIC', 'ref-1', 2027)")
    db.execute("INSERT INTO demo_sites VALUES ('site-1', 'Sede Viña del Mar', 'cat-1', 1)")
    db.execute("INSERT INTO demo_sites VALUES ('site-2', 'Sede Valparaíso', 'cat-1', 1)")
    db.execute("INSERT INTO demo_profiles VALUES ('prof-captor-1', 'Captador Juan', 'CAPTOR')")
    db.execute("INSERT INTO demo_profiles VALUES ('prof-captor-2', 'Captadora Maria', 'CAPTOR')")
    db.execute("INSERT INTO demo_profiles VALUES ('prof-super', 'Supervisor Carlos', 'SUPERVISOR')")
    db.execute("INSERT INTO demo_profile_sites VALUES ('prof-captor-1', 'site-1')")
    db.execute("INSERT INTO demo_profile_sites VALUES ('prof-captor-2', 'site-2')")
    db.execute("INSERT INTO demo_profile_sites VALUES ('prof-super', 'site-1')")
    db.execute("INSERT INTO demo_profile_sites VALUES ('prof-super', 'site-2')")
    db.execute("INSERT INTO demo_events VALUES ('event-1', 'site-1', 'Feria Regional 2026', '2026-10-10T09:00:00Z', '2026-10-10T18:00:00Z', 'America/Santiago', -33.02, -71.55, 'Liceo 1', 'Viña')")
    db.execute("INSERT INTO demo_events VALUES ('event-2', 'site-2', 'Visita Técnica 2026', '2026-10-11T09:00:00Z', '2026-10-11T18:00:00Z', 'America/Santiago', -33.04, -71.61, 'Colegio 2', 'Valpo')")
    db.execute("INSERT INTO demo_assignments VALUES ('event-1', 'prof-captor-1')")
    db.execute("INSERT INTO demo_assignments VALUES ('event-2', 'prof-captor-2')")
    db.execute("INSERT INTO demo_programs VALUES ('prog-1', 'Ingeniería en Informática', 'cat-1', 'DEMO_PHYSICAL', 1)")
    db.execute("INSERT INTO demo_programs VALUES ('prog-2', 'Analista Programador', 'cat-1', 'DEMO_PHYSICAL', 1)")
    db.commit()

    latencies = []
    num_records = 400

    print(f"Iniciando inserción controlada de {num_records} registros (RNF02)...")
    t0 = time.perf_counter()

    for i in range(num_records):
        rec_id = f"cap-{uuid.uuid4()}"
        event_id = "event-1" if i % 2 == 0 else "event-2"
        site_id = "site-1" if i % 2 == 0 else "site-2"
        owner_id = "prof-captor-1" if i % 2 == 0 else "prof-captor-2"
        
        # Distribución de estados: READY (75%), DRAFT (12.5%), NEEDS_CONTEXT (7.5%), CANCELLED (5%)
        if i % 20 in (0, 1):
            state = "CANCELLED"
        elif i % 20 in (2, 3, 4):
            state = "NEEDS_CONTEXT"
        elif i % 20 in (5, 6):
            state = "DRAFT"
        else:
            state = "READY"

        t_start = time.perf_counter()
        db.execute('BEGIN TRANSACTION')

        db.execute("""
            INSERT INTO captures (
                id, revision, state, ownerId, siteId, eventId, deviceId, capturedAt,
                eventName, eventTimeZone, fullName, rut, email, phone, educationLevel,
                establishment, contactChannel, notes, consentVersion, acceptedAt, recordedBy,
                acceptanceMode, locationSource, latitude, longitude, measuredAt, accuracyMeters,
                confirmedAt, confirmedBy, contractVersion, givenNames, familyNames, birthDate,
                residenceCommune, currentActivity, currentActivityOther, establishmentCommune,
                formPersonalOption, preferredSchedule, interestLevel, motivation, modality
            ) VALUES (
                ?, 1, ?, ?, ?, ?, 'dev-1', '2026-10-10T12:00:00Z',
                'Feria Demo', 'America/Santiago', 'Estudiante Perez Gonzalez', ?, 'prospecto@ejemplo.cl', '+56911223344', 'SECONDARY',
                'Liceo Demo', 'WHATSAPP', 'Sin notas', 'demo-2026-v1', '2026-10-10T12:00:00Z', ?,
                'ELECTRONIC', 'GPS', -33.02, -71.55, '2026-10-10T12:00:00Z', 15.0,
                '2026-10-10T12:00:00Z', ?, 'F2027', 'Estudiante', 'Perez Gonzalez', '2008-05-15',
                'Viña del Mar', 'SECONDARY', NULL, 'Viña del Mar',
                'DAY', 'DAY', 'HIGH', 'VOCATION', 'PRESENCIAL'
            )
        """, (rec_id, state, owner_id, site_id, event_id, f"1111111{i}-K", owner_id, owner_id))

        # Intereses (1 a 2 por prospecto)
        db.execute("""
            INSERT INTO capture_interests (captureId, programId, offeringId, catalogVersion, programName, modality, priority, section, mentionId, mentionName)
            VALUES (?, 'prog-1', 'off-1', 'cat-1', 'Ingeniería en Informática', 'PRESENCIAL', 1, 'DEMO_PHYSICAL', NULL, NULL)
        """, (rec_id,))

        # Llave activa para registros confirmados
        if state in ('READY', 'NEEDS_CONTEXT'):
            db.execute("""
                INSERT INTO active_registration_keys (eventId, rut, captureId)
                VALUES (?, ?, ?)
            """, (event_id, f"1111111{i}-K", rec_id))

        # Auditoría local
        db.execute("""
            INSERT INTO local_audit (id, captureId, actorId, occurredAt, action, resultingRevision)
            VALUES (?, ?, ?, '2026-10-10T12:00:00Z', 'CREATED', 1)
        """, (f"aud-{uuid.uuid4()}", rec_id, owner_id))

        db.commit()
        t_end = time.perf_counter()
        latencies.append((t_end - t_start) * 1000.0)

    total_time = (time.perf_counter() - t0) * 1000.0
    latencies.sort()
    p50 = latencies[int(num_records * 0.50)]
    p95 = latencies[int(num_records * 0.95)]
    p99 = latencies[int(num_records * 0.99)]
    max_lat = latencies[-1]

    print(f"Inserción completa de {num_records} registros:")
    print(f"  Tiempo total: {total_time:.2f} ms ({total_time/num_records:.3f} ms/transacción)")
    print(f"  p50: {p50:.3f} ms | p95: {p95:.3f} ms | p99: {p99:.3f} ms | max: {max_lat:.3f} ms")

    assert p95 < 150.0, f"Violación de RNF02: p95 ({p95:.2f} ms) supera meta de 150 ms"

    # Verificación de integridad relacional
    fk_errors = db.execute('PRAGMA foreign_key_check').fetchall()
    assert fk_errors == [], f"Errores de integridad referencial: {fk_errors}"

    # Verificación de conteo y consultas de métricas
    total_captures = db.execute("SELECT COUNT(*) FROM captures").fetchone()[0]
    assert total_captures == 400, f"Total esperado 400, obtenido {total_captures}"

    total_interests = db.execute("SELECT COUNT(*) FROM capture_interests").fetchone()[0]
    assert total_interests == 400

    total_audits = db.execute("SELECT COUNT(*) FROM local_audit").fetchone()[0]
    assert total_audits == 400

    # Benchmark Consulta Métricas Captador
    t_q0 = time.perf_counter()
    metrics_owner = db.execute("""
        SELECT state, COUNT(*) FROM captures
        WHERE ownerId = 'prof-captor-1' AND eventId = 'event-1'
        GROUP BY state
    """).fetchall()
    t_q_owner = (time.perf_counter() - t_q0) * 1000.0
    print(f"Consulta Métricas Captador: {t_q_owner:.3f} ms -> {dict(metrics_owner)}")

    # Benchmark Consulta Métricas Supervisor
    t_q1 = time.perf_counter()
    metrics_super = db.execute("""
        SELECT siteId, COUNT(*) FROM captures
        WHERE eventId = 'event-1' AND siteId IN ('site-1', 'site-2') AND state != 'DRAFT'
        GROUP BY siteId
    """).fetchall()
    t_q_super = (time.perf_counter() - t_q1) * 1000.0
    print(f"Consulta Métricas Supervisor: {t_q_super:.3f} ms -> {dict(metrics_super)}")

    # Benchmark Bandeja Paginada (Búsqueda + Orden)
    t_tray0 = time.perf_counter()
    tray_records = db.execute("""
        SELECT id, state, fullName, email, capturedAt FROM captures
        WHERE ownerId = 'prof-captor-1' AND eventId = 'event-1'
        ORDER BY capturedAt DESC, id DESC
        LIMIT 20
    """).fetchall()
    t_tray = (time.perf_counter() - t_tray0) * 1000.0
    print(f"Consulta Bandeja Paginada (20 items): {t_tray:.3f} ms (obtenidos {len(tray_records)} items)")

    db.close()
    print("OK: Dataset de 400 registros validado con éxito bajo todas las reglas de RNF02.")

if __name__ == '__main__':
    run_dataset_benchmark()
