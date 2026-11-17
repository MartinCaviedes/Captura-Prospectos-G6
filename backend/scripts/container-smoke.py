#!/usr/bin/env python3
"""Valida imágenes locales quality con datos ficticios, red y secretos temporales."""
import importlib.util
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import time
import urllib.error
import urllib.request
import uuid

ROOT = Path(__file__).resolve().parents[1]
PREFIX = "g6-test-" + uuid.uuid4().hex[:12]
SERVICES = ("users-service", "prospects-service", "api-gateway")


def docker(*args):
    return subprocess.check_output(["docker", *args], text=True).strip()


def http(port, path, data=None, token=None, expected=200):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(f"http://127.0.0.1:{port}" + path,
        data=json.dumps(data).encode() if data is not None else None, headers=headers)
    try:
        response = urllib.request.urlopen(req, timeout=10)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        assert response.status == expected, f"{path}: {response.status}, esperado {expected}"
        raw = response.read()
        return json.loads(raw) if raw else None


def run():
    started = []
    network_created = False
    with tempfile.TemporaryDirectory(prefix="g6-container-") as temp:
        temp = Path(temp)
        for variable, name in (("JWT_PUBLIC_KEY", "public.pem"), ("JWT_PRIVATE_KEY", "private.pem")):
            source = os.environ[variable]
            if not source.startswith("file:"):
                raise ValueError("La prueba requiere claves PEM locales del setup")
            target = temp / name
            shutil.copyfile(source[5:], target)
            # Solo se montan archivos individuales. El directorio temporal padre es 0700.
            target.chmod(0o444)
        try:
            docker("network", "create", PREFIX)
            network_created = True
            for service in SERVICES:
                name = PREFIX + "-" + service
                variables = {
                    "PORT": "8080", "SPRING_PROFILES_ACTIVE": "local",
                    "SHUTDOWN_TIMEOUT": "5s", "JWT_PUBLIC_KEY": "file:/run/secrets/public.pem",
                    "USERS_SERVICE_URL": f"http://{PREFIX}-users-service:8080",
                    "PROSPECTS_SERVICE_URL": f"http://{PREFIX}-prospects-service:8080",
                }
                args = ["run", "--detach", "--name", name, "--network", PREFIX,
                    "--publish", "127.0.0.1::8080", "--read-only", "--tmpfs", "/tmp:rw,nosuid,size=128m",
                    "--mount", f"type=bind,src={temp / 'public.pem'},dst=/run/secrets/public.pem,readonly"]
                if service == "users-service":
                    variables.update(JWT_PRIVATE_KEY="file:/run/secrets/private.pem",
                        LOCAL_ADMIN_PASSWORD=os.environ["LOCAL_ADMIN_PASSWORD"],
                        LOCAL_CAPTOR_PASSWORD=os.environ["LOCAL_CAPTOR_PASSWORD"])
                    args.extend(["--mount", f"type=bind,src={temp / 'private.pem'},dst=/run/secrets/private.pem,readonly"])
                env_file = temp / (service + ".env")
                env_file.write_text("".join(f"{key}={value}\n" for key, value in variables.items()))
                env_file.chmod(0o600)
                docker(*args, "--env-file", str(env_file), "g6-" + service + ":quality")
                started.append(name)
                assert docker("inspect", "--format", "{{.Config.User}}", name) == "10001:10001"
                mapping = json.loads(docker("inspect", "--format", "{{json .NetworkSettings.Ports}}", name))
                port = int(mapping["8080/tcp"][0]["HostPort"])
                for _ in range(180):
                    if docker("inspect", "--format", "{{.State.Running}}", name) != "true":
                        raise RuntimeError(service + " terminó durante el arranque")
                    try:
                        if http(port, "/actuator/health/readiness")["status"] == "UP":
                            break
                    except (urllib.error.URLError, TimeoutError, ConnectionError, AssertionError):
                        time.sleep(0.5)
                else:
                    raise RuntimeError(service + " no alcanzó readiness")
            http(port, "/api/v1/prospects", expected=401)
            login = http(port, "/api/v1/auth/login", {"username": "captor-a",
                "password": os.environ["LOCAL_CAPTOR_PASSWORD"]})
            record = http(port, "/api/v1/prospects", {"rut": "12345678-5", "eventId": str(uuid.uuid4()),
                "siteId": "site-1", "givenNames": "Persona", "familyNames": "Ficticia",
                "email": "ficticia@example.com", "phone": "+56912345678", "consentAccepted": True,
                "consentVersion": "v1"}, login["accessToken"], 201)
            assert record["captorId"] == login["userId"]
            for name in reversed(started):
                docker("stop", "--time", "15", name)
                assert "Graceful shutdown complete" in docker("logs", name)
        finally:
            for name in reversed(started):
                docker("rm", "--force", name)
            if network_created:
                docker("network", "rm", PREFIX)
    print("OK: imágenes no root, filesystem de solo lectura, readiness, login/JWT, creación y SIGTERM")


if __name__ == "__main__":
    run()
