#!/usr/bin/env python3
"""Regresión real: no preparar SQL contra el servidor temporal de initdb."""
import importlib.util
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location(
    "postgres_smoke", Path(__file__).with_name("postgres-smoke.py"))
postgres = importlib.util.module_from_spec(spec)
spec.loader.exec_module(postgres)


class SetupReady(Exception):
    pass


class PostgresReadinessTest(unittest.TestCase):
    def test_sql_setup_waits_for_final_tcp_server(self):
        original_docker = postgres.docker
        with tempfile.TemporaryDirectory(prefix="g6-pg-readiness-") as directory:
            # Amplía la ventana del servidor temporal, que solo escucha por socket.
            hook = Path(directory) / "delay.sql"
            hook.write_text("SELECT pg_sleep(3);\n")
            hook.chmod(0o644)

            def docker(*args):
                if args[0] == "run":
                    args = (*args[:-1], "--mount",
                        f"type=bind,src={hook},dst=/docker-entrypoint-initdb.d/delay.sql,readonly",
                        args[-1])
                return original_docker(*args)

            def sql(*args, **kwargs):
                ready = subprocess.run(["docker", "exec", postgres.NAME, "pg_isready",
                    "-h", "127.0.0.1", "-U", "postgres"], capture_output=True)
                self.assertEqual(ready.returncode, 0,
                    "El SQL de preparación comenzó antes de que PostgreSQL escuchara por TCP")
                # Se detiene antes de migraciones/JAR; run() limpia su contenedor.
                raise SetupReady()

            with patch.object(postgres, "docker", docker), patch.object(postgres, "sql", sql):
                with self.assertRaises(SetupReady):
                    postgres.run(directory)


if __name__ == "__main__":
    unittest.main()
