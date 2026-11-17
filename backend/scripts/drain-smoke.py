#!/usr/bin/env python3
"""Comprueba drenaje real del gateway con un upstream HTTP local controlado."""
import concurrent.futures
import http.server
import importlib.util
from pathlib import Path
import tempfile
import threading
import time

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("smoke", ROOT / "scripts/smoke-test.py")
smoke = importlib.util.module_from_spec(spec)
spec.loader.exec_module(smoke)
started = threading.Event()
release = threading.Event()


class Backend(http.server.BaseHTTPRequestHandler):
    def do_POST(self):
        self.rfile.read(int(self.headers.get("Content-Length", "0")))
        started.set()
        if not release.wait(timeout=20):
            self.send_error(504)
            return
        body = b'{"status":"completed"}'
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, *_):
        pass


def run():
    backend = http.server.ThreadingHTTPServer(("127.0.0.1", 0), Backend)
    thread = threading.Thread(target=backend.serve_forever, daemon=True)
    thread.start()
    try:
        with tempfile.TemporaryDirectory(prefix="g6-drain-") as temp:
            try:
                smoke.start("api-gateway", smoke.BASE_PORT, temp, {
                    "USERS_SERVICE_URL": f"http://127.0.0.1:{backend.server_port}",
                    "GATEWAY_RESPONSE_TIMEOUT": "15s", "SHUTDOWN_TIMEOUT": "15s"})
                process, log = smoke.processes["api-gateway"]
                with concurrent.futures.ThreadPoolExecutor(max_workers=1) as executor:
                    future = executor.submit(smoke.request, "POST", "/api/v1/auth/login",
                        {"username": "ficticio", "password": "ficticia"})
                    assert started.wait(timeout=5), "La solicitud no llegó al upstream"
                    process.terminate()
                    deadline = time.monotonic() + 5
                    while "Commencing graceful shutdown" not in log.read_text():
                        assert process.poll() is None, "Gateway terminó antes de drenar"
                        assert time.monotonic() < deadline, "No comenzó el cierre controlado"
                        time.sleep(0.05)
                    assert not future.done(), "La prueba requiere una solicitud aún activa"
                    release.set()
                    assert future.result(timeout=10)["status"] == "completed"
                    process.wait(timeout=15)
                    assert "Graceful shutdown complete" in log.read_text()
                    smoke.processes.pop("api-gateway")
            finally:
                release.set()
                if "api-gateway" in smoke.processes:
                    smoke.stop("api-gateway")
    finally:
        backend.shutdown()
        backend.server_close()
        thread.join(timeout=5)
    print("OK: solicitud activa completada con 200 durante SIGTERM y cierre del gateway")


if __name__ == "__main__":
    run()
