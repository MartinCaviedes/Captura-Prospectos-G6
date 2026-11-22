#!/usr/bin/env python3
"""Prueba real de tres JAR; ejecutar tras prepare-local.sh, source y clean verify."""
import json
import os
from pathlib import Path
import subprocess
import tempfile
import time
import urllib.error
import urllib.request
import uuid

ROOT = Path(__file__).resolve().parents[1]
BASE_PORT = int(os.environ.get("SMOKE_BASE_PORT", "18080"))
BASE = f"http://127.0.0.1:{BASE_PORT}"
JAVA = str(Path(os.environ["JAVA_HOME"]) / "bin/java") if "JAVA_HOME" in os.environ else "java"
processes = {}


def request(method, path, data=None, token=None, expected=200):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(BASE + path, data=json.dumps(data).encode() if data is not None else None,
                                 headers=headers, method=method)
    try:
        response = urllib.request.urlopen(req, timeout=10)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        assert response.status == expected, f"{method} {path}: {response.status}, esperado {expected}"
        raw = response.read()
        return json.loads(raw) if raw else None


def start(service, port, temp, settings=None):
    env = dict(os.environ, PORT=str(port), SHUTDOWN_TIMEOUT="5s")
    if service != "users-service":
        for key in ("JWT_PRIVATE_KEY", "LOCAL_ADMIN_PASSWORD", "LOCAL_CAPTOR_PASSWORD"):
            env.pop(key, None)
    env["USERS_SERVICE_URL"] = f"http://127.0.0.1:{BASE_PORT + 1}"
    env["PROSPECTS_SERVICE_URL"] = f"http://127.0.0.1:{BASE_PORT + 2}"
    # Bases separadas y temporales, nunca usar datos compartidos para esta prueba.
    env["IDENTITY_JDBC_URL"] = "jdbc:h2:mem:smoke_identity;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
    env["IDENTITY_DB_USER"] = env["PROSPECTS_DB_USER"] = "sa"
    env["IDENTITY_DB_PASSWORD"] = env["PROSPECTS_DB_PASSWORD"] = ""
    env["PROSPECTS_JDBC_URL"] = f"jdbc:h2:file:{temp}/prospects;MODE=PostgreSQL"
    env["JPA_DDL_AUTO"] = env["PROSPECTS_DDL_AUTO"] = "update"
    env.update(settings or {})
    jar = ROOT / service / "target" / f"{service}-0.0.1-SNAPSHOT.jar"
    with open(Path(temp) / f"{service}.log", "ab") as log:
        process = subprocess.Popen([JAVA, "-jar", str(jar)], env=env, stdout=log, stderr=subprocess.STDOUT)
    processes[service] = (process, Path(temp) / f"{service}.log")
    for _ in range(180):
        if process.poll() is not None:
            raise RuntimeError(f"{service} terminó; revisar su configuración")
        try:
            with urllib.request.urlopen(f"http://127.0.0.1:{port}/actuator/health/readiness", timeout=1) as response:
                if response.status == 200:
                    return
        except (urllib.error.URLError, TimeoutError):
            pass
        time.sleep(0.5)
    raise RuntimeError(f"{service} no inició a tiempo")


def stop(service):
    process, log_path = processes.pop(service)
    process.terminate()
    try:
        process.wait(timeout=15)
    except subprocess.TimeoutExpired:
        process.kill()
        process.wait()
        raise AssertionError(f"{service} excedió el tiempo de cierre")
    assert "Graceful shutdown complete" in log_path.read_text(), f"{service}: falta evidencia de cierre controlado"


def login(username, password):
    return request("POST", "/api/v1/auth/login", {"username": username, "password": password})


def run(temp):
    for service, offset in (("users-service", 1), ("prospects-service", 2), ("api-gateway", 0)):
        start(service, BASE_PORT + offset, temp)
    request("GET", "/api/v1/prospects", expected=401)
    request("POST", "/api/v1/auth/login", {"username": "captor-a", "password": "incorrecta"}, expected=401)
    a = login("captor-a", os.environ["LOCAL_CAPTOR_PASSWORD"])
    b = login("captor-b", os.environ["LOCAL_CAPTOR_PASSWORD"])
    admin = login("demo-admin", os.environ["LOCAL_ADMIN_PASSWORD"])
    supervisor = login("supervisor", os.environ["LOCAL_ADMIN_PASSWORD"])
    token = a["accessToken"]
    identity = request("GET", "/api/v1/me", token=token)
    assert identity["id"] == a["userId"]
    parts = token.split(".")
    parts[2] = ("A" if parts[2][0] != "A" else "B") + parts[2][1:]
    request("GET", "/api/v1/prospects", token=".".join(parts), expected=401)
    payload = {"rut": "12.345.678-5", "eventId": str(uuid.uuid4()), "siteId": "site-1",
               "givenNames": "Persona", "familyNames": "Ficticia", "email": "ficticia@example.com",
               "phone": "+56912345678", "consentAccepted": True, "consentVersion": "local-v1"}
    prospect = request("POST", "/api/v1/prospects", payload, token, 201)
    assert prospect["captorId"] == a["userId"]
    path = "/api/v1/prospects/" + prospect["id"]
    request("GET", path, token=b["accessToken"], expected=403)
    assert request("GET", "/api/v1/prospects", token=b["accessToken"])["items"] == []
    request("GET", path, token=admin["accessToken"])
    request("GET", path, token=supervisor["accessToken"])
    request("POST", "/api/v1/prospects", payload, token, 409)
    update = {key: payload[key] for key in ("givenNames", "familyNames", "email", "phone")}
    update["givenNames"] = "Persona actualizada"
    request("PUT", path, update, b["accessToken"], 403)
    request("PUT", path, update, token)
    # Confirma persistencia real tras reiniciar el servicio, conservando gateway/usuarios.
    stop("prospects-service")
    start("prospects-service", BASE_PORT + 2, temp)
    assert request("GET", path, token=token)["givenNames"] == "Persona actualizada"
    request("POST", path + "/cancel", {"reason": "Prueba"}, token, 403)
    cancelled = request("POST", path + "/cancel", {"reason": "Prueba"}, supervisor["accessToken"])
    assert cancelled["status"] == "CANCELLED" and cancelled["cancelledBy"] == supervisor["userId"]
    request("POST", "/api/v1/prospects", payload, token, 201)


if __name__ == "__main__":
    for required in ("JWT_PUBLIC_KEY", "JWT_PRIVATE_KEY", "LOCAL_ADMIN_PASSWORD", "LOCAL_CAPTOR_PASSWORD"):
        if not os.environ.get(required):
            raise SystemExit(f"Falta {required}: ejecutar prepare-local.sh y source .local/env.sh")
    with tempfile.TemporaryDirectory(prefix="g6-smoke-") as temp:
        try:
            run(temp)
        finally:
            cleanup_errors = []
            for name in list(processes):
                try:
                    stop(name)
                except Exception as error:
                    cleanup_errors.append(error)
            if cleanup_errors:
                raise AssertionError("Falló el cierre de procesos de prueba") from cleanup_errors[0]

    print("OK: tres procesos, login, JWT, identidad, CRUD, permisos, duplicado, anulación, persistencia, readiness y cierre controlado")
