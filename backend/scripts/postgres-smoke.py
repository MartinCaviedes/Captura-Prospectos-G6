#!/usr/bin/env python3
"""PostgreSQL efímero, migraciones iniciales y tres JAR con perfil prod."""
import concurrent.futures
import importlib.util
import json
import os
from pathlib import Path
import secrets
import subprocess
import tempfile
import threading
import time
import urllib.error
import urllib.request
import uuid

ROOT = Path(__file__).resolve().parents[1]
IMAGE = "postgres:17.12-bookworm@sha256:40865bc877a289da75963d97d72b59633f639fc5af7a4e856bfa3714cae5b536"
NAME = "g6-pg-test-" + uuid.uuid4().hex[:12]
spec = importlib.util.spec_from_file_location("smoke", ROOT / "scripts/smoke-test.py")
smoke = importlib.util.module_from_spec(spec)
spec.loader.exec_module(smoke)
original_start = smoke.start


def docker(*args):
    return subprocess.check_output(["docker", *args], text=True).strip()


def sql(database, statement, expect_success=True):
    result = subprocess.run(["docker", "exec", "-i", NAME, "psql", "-U", "postgres", "-d", database,
        "-v", "ON_ERROR_STOP=1", "-Atq"], input=statement, text=True, capture_output=True)
    assert (result.returncode == 0) == expect_success, "Resultado inesperado al aplicar SQL de prueba"
    return result.stdout.strip()


def run(temp):
    password = secrets.token_hex(24)
    env_file = Path(temp) / "postgres.env"
    env_file.write_text("POSTGRES_PASSWORD=" + secrets.token_hex(24) + "\n")
    env_file.chmod(0o600)
    started = False
    try:
        docker("run", "--detach", "--name", NAME, "--publish", "127.0.0.1::5432",
            "--tmpfs", "/var/lib/postgresql/data:rw,nosuid", "--env-file", str(env_file), IMAGE)
        started = True
        # initdb usa un servidor temporal solo por socket que luego se reinicia.
        # Esperar TCP evita preparar roles/esquemas durante ese reinicio.
        for _ in range(120):
            result = subprocess.run(["docker", "exec", NAME, "pg_isready", "-h", "127.0.0.1", "-U", "postgres"], capture_output=True)
            if result.returncode == 0:
                break
            time.sleep(0.5)
        else:
            raise RuntimeError("PostgreSQL temporal no inició")
        mapping = json.loads(docker("inspect", "--format", "{{json .NetworkSettings.Ports}}", NAME))
        port = int(mapping["5432/tcp"][0]["HostPort"])
        for service, database, role, table in (
            ("users-service", "identity_test", "captura_users", "usuarios"),
            ("prospects-service", "prospects_test", "captura_prospects", "prospects")):
            sql("postgres", f"CREATE ROLE {role} LOGIN PASSWORD '{password}'; CREATE DATABASE {database};")
            migration = (ROOT / service / "db/migrations/V001__initial.sql").read_text()
            sql(database, migration)
            sql(database, migration, expect_success=False)
            assert sql(database, "SELECT version FROM schema_version;") == "1"
            if service == "users-service":
                sql(database, (ROOT / service / "db/migrations/V002__account_access.sql").read_text())
                assert sql(database, "SELECT max(version) FROM schema_version;") == "2"
            else:
                prior_id=str(uuid.uuid4())
                sql(database,f"INSERT INTO prospects(id,version,rut,event_id,site_id,captor_id,given_names,family_names,email,phone,consent_version,consent_recorded_at,created_at,updated_at,active_key) VALUES ('{prior_id}',0,'12345678-5','{uuid.uuid4()}','site-1','{uuid.uuid4()}','Anterior','Ficticia','anterior@example.invalid','+56912345678','v1',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,NULL);")
                before=sql(database,f"SELECT row_to_json(p) FROM prospects p WHERE id='{prior_id}';")
                upgrade=(ROOT/service/'db/migrations/V002__capture_sync.sql').read_text()
                sql(database,upgrade)
                assert before==sql(database,f"SELECT row_to_json(p) FROM prospects p WHERE id='{prior_id}';")
                sql(database,upgrade,expect_success=False)
            sql(database, f"REVOKE CONNECT ON DATABASE {database} FROM PUBLIC; GRANT CONNECT ON DATABASE {database} TO {role}; GRANT USAGE ON SCHEMA public TO {role}; GRANT SELECT, INSERT, UPDATE ON {table} TO {role};")
            if service == "users-service":
                sql(database, f"GRANT SELECT, INSERT ON account_access_audit TO {role};")
            else:
                sql(database, f"GRANT SELECT, INSERT, UPDATE ON capture_sync TO {role};")
        assert sql("postgres", "SELECT has_database_privilege('captura_users','prospects_test','CONNECT');") == "f"
        settings = {
            "IDENTITY_JDBC_URL": f"jdbc:postgresql://127.0.0.1:{port}/identity_test",
            "IDENTITY_DB_USER": "captura_users", "IDENTITY_DB_PASSWORD": password,
            "PROSPECTS_JDBC_URL": f"jdbc:postgresql://127.0.0.1:{port}/prospects_test",
            "PROSPECTS_DB_USER": "captura_prospects", "PROSPECTS_DB_PASSWORD": password,
            "JPA_DDL_AUTO": "validate", "PROSPECTS_DDL_AUTO": "validate", "SPRING_PROFILES_ACTIVE": "prod",
        }
        bootstrap_env = dict(os.environ, **settings,
            BOOTSTRAP_ADMIN_USERNAME="bootstrap-admin", BOOTSTRAP_ADMIN_EMAIL="bootstrap@example.com",
            BOOTSTRAP_ADMIN_DISPLAY_NAME="Administrador ficticio",
            BOOTSTRAP_ADMIN_PASSWORD=os.environ["LOCAL_ADMIN_PASSWORD"], BOOTSTRAP_ADMIN_SITES="site-1,site-2")
        jar = ROOT / "users-service/target/users-service-0.0.1-SNAPSHOT.jar"

        def bootstrap(environment=bootstrap_env):
            return subprocess.run([smoke.JAVA, "-jar", str(jar), "--bootstrap-admin"],
                env=environment, capture_output=True, text=True, timeout=45).returncode

        assert bootstrap(dict(bootstrap_env, BOOTSTRAP_ADMIN_PASSWORD="short")) == 1
        assert bootstrap(dict(bootstrap_env, BOOTSTRAP_ADMIN_EMAIL="invalid")) == 1
        assert sql("identity_test", "SELECT count(*) FROM usuarios;") == "0"
        with concurrent.futures.ThreadPoolExecutor(max_workers=2) as executor:
            codes = list(executor.map(lambda _: bootstrap(), range(2)))
        assert sorted(codes) == [0, 1], "El bootstrap concurrente debe aceptar solo una ejecución"
        assert sql("identity_test", "SELECT count(*) FROM usuarios;") == "1"
        assert sql("identity_test", "SELECT rol FROM usuarios;") == "ADMIN"
        assert bootstrap() == 1, "El bootstrap debe rechazar una base con usuarios"
        # Fixtures ficticias mediante el seed local existente, sobre tablas ya migradas.
        original_start("users-service", smoke.BASE_PORT + 1, temp,
            dict(settings, SPRING_PROFILES_ACTIVE="local"))
        smoke.stop("users-service")

        def start(service, service_port, directory):
            original_start(service, service_port, directory, settings)

        smoke.start = start
        smoke.run(temp)
        admin = smoke.login("bootstrap-admin", os.environ["LOCAL_ADMIN_PASSWORD"])
        assert admin["rol"] == "ADMIN"
        smoke.request("GET", "/api/v1/admin/status", token=admin["accessToken"])
        login = smoke.login("captor-a", os.environ["LOCAL_CAPTOR_PASSWORD"])
        event = str(uuid.uuid4())
        payload = {"rut": "12345678-5", "eventId": event, "siteId": "site-1", "givenNames": "Concurrente",
            "familyNames": "Ficticio", "email": "ficticio@example.com", "phone": "+56912345678",
            "consentAccepted": True, "consentVersion": "v1"}
        barrier = threading.Barrier(2)

        def create():
            req = urllib.request.Request(smoke.BASE + "/api/v1/prospects", data=json.dumps(payload).encode(),
                headers={"Content-Type": "application/json", "Authorization": "Bearer " + login["accessToken"]})
            barrier.wait(timeout=10)
            try:
                response = urllib.request.urlopen(req, timeout=10)
            except urllib.error.HTTPError as error:
                response = error
            with response:
                response.read()
                return response.status

        with concurrent.futures.ThreadPoolExecutor(max_workers=2) as executor:
            responses = list(executor.map(lambda _: create(), range(2)))
        assert sorted(responses) == [201, 409], "La creación concurrente debe producir 201 y 409"
        assert sql("prospects_test", f"SELECT count(*) FROM prospects WHERE event_id='{event}' AND active_key IS NOT NULL;") == "1"
        local_id,device_id=str(uuid.uuid4()),str(uuid.uuid4())
        sync_event=str(uuid.uuid4())
        sync_payload=dict(deviceId=device_id,ownerId=login['userId'],sourceRevision=1,expectedRevision=0,
            registration=dict(payload,eventId=sync_event),state='NEEDS_CONTEXT',snapshotJson=json.dumps({'schema':'F2027_1','interests':[{'priority':1}]}))
        sync_path='/api/v1/prospects/sync/'+local_id
        ack=smoke.request('PUT',sync_path,sync_payload,login['accessToken'])
        assert smoke.request('PUT',sync_path,sync_payload,login['accessToken'])==ack
        smoke.stop('prospects-service');smoke.start('prospects-service',smoke.BASE_PORT+2,temp)
        assert smoke.request('PUT',sync_path,sync_payload,login['accessToken'])==ack
        sync_payload.update(sourceRevision=2,expectedRevision=1,state='CANCELLED',reason='Anulacion de prueba')
        cancelled=smoke.request('PUT',sync_path,sync_payload,login['accessToken'])
        assert smoke.request('PUT',sync_path,sync_payload,login['accessToken'])==cancelled
        assert sql('prospects_test',f"SELECT count(*) FROM prospects WHERE event_id='{sync_event}';")=='1'
        assert sql('prospects_test',f"SELECT source_revision FROM capture_sync WHERE local_capture_id='{local_id}';")=='2'
        concurrent_id=str(uuid.uuid4());concurrent_event=str(uuid.uuid4())
        concurrent_payload=dict(sync_payload,sourceRevision=1,expectedRevision=0,state='NEEDS_CONTEXT',registration=dict(payload,eventId=concurrent_event))
        barrier=threading.Barrier(2)
        def sync_concurrently():
            barrier.wait(timeout=10)
            return smoke.request('PUT','/api/v1/prospects/sync/'+concurrent_id,concurrent_payload,login['accessToken'])
        with concurrent.futures.ThreadPoolExecutor(max_workers=2) as executor:
            acks=list(executor.map(lambda _:sync_concurrently(),range(2)))
        assert acks[0]==acks[1]
        assert sql('prospects_test',f"SELECT count(*) FROM prospects WHERE event_id='{concurrent_event}';")=='1'
        print('PASS: concurrent identical sync requests return one shared ACK and one prospect')
        print('PASS: sync migration preserves prior row; replay/create/cancel and restart retain one remote capture')
        print("PostgreSQL probado:", sql("postgres", "SHOW server_version;"))
    finally:
        errors = []
        for service in list(smoke.processes):
            try:
                smoke.stop(service)
            except Exception as error:
                errors.append(error)
        if started:
            docker("rm", "--force", "--volumes", NAME)
        if errors:
            raise AssertionError("Falló el cierre de procesos de prueba") from errors[0]


if __name__ == "__main__":
    with tempfile.TemporaryDirectory(prefix="g6-pg-") as directory:
        run(directory)
    print("OK: migraciones, roles separados, bootstrap único/concurrente, prod validate, CRUD, reinicio y duplicado concurrente")
