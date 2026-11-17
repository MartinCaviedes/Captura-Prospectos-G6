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
        for _ in range(120):
            result = subprocess.run(["docker", "exec", NAME, "pg_isready", "-U", "postgres"], capture_output=True)
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
            sql(database, f"REVOKE CONNECT ON DATABASE {database} FROM PUBLIC; GRANT CONNECT ON DATABASE {database} TO {role}; GRANT USAGE ON SCHEMA public TO {role}; GRANT SELECT, INSERT, UPDATE ON {table} TO {role};")
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
