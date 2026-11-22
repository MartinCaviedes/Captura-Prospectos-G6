#!/usr/bin/env python3
"""Fresh-host installation. Never deletes an existing database or rotates secrets."""
import json
import os
from pathlib import Path
import secrets
import subprocess
import time
import urllib.request

ROOT = Path(__file__).resolve().parent
DATA = Path('/srv/g6')
STATE = DATA / 'config'
PG = 'postgres:17.12-bookworm@sha256:40865bc877a289da75963d97d72b59633f639fc5af7a4e856bfa3714cae5b536'
SERVICES = ('users-service', 'prospects-service', 'api-gateway')


def run(*args, **kwargs):
    return subprocess.run(args, check=True, **kwargs)


def docker(*args):
    return run('docker', *args)


def envfile(name, values):
    target = STATE / name
    if target.exists():
        raise RuntimeError('Configuration already exists; use the documented restart procedure')
    with target.open('x') as stream:
        os.chmod(target, 0o600)
        stream.write(''.join(key + '=' + value + '\n' for key, value in values.items()))


def sql(database, text):
    # SQL contains generated credentials: never echo the statement or psql diagnostics.
    result = subprocess.run(['docker', 'exec', '-i', 'g6-postgres', 'psql', '-U',
        'postgres', '-d', database, '-v', 'ON_ERROR_STOP=1', '-q'],
        input=text, text=True, capture_output=True)
    if result.returncode:
        raise RuntimeError('Database initialization failed; existing data preserved')


def readiness():
    for _ in range(180):
        try:
            with urllib.request.urlopen('http://127.0.0.1:8080/actuator/health/readiness', timeout=3) as response:
                if json.load(response)['status'] == 'UP':
                    return
        except (OSError, ValueError):
            pass
        time.sleep(2)
    raise RuntimeError('Gateway readiness timed out')


def main():
    os.umask(0o077)
    if not os.path.ismount(DATA):
        raise RuntimeError('/srv/g6 must be the mounted persistent data volume')
    if STATE.exists():
        raise RuntimeError('Existing installation detected; refusing to overwrite configuration')
    STATE.mkdir(mode=0o700)
    (DATA / 'backups').mkdir(mode=0o700)
    (DATA / 'postgres').mkdir(mode=0o700)
    postgres_password, identity_password, prospects_password, admin_password = [secrets.token_hex(24) for _ in range(4)]
    envfile('postgres.env', {'POSTGRES_PASSWORD': postgres_password})
    run('openssl', 'genpkey', '-algorithm', 'RSA', '-pkeyopt', 'rsa_keygen_bits:2048', '-out', str(STATE / 'private.pem'), stderr=subprocess.DEVNULL)
    run('openssl', 'pkey', '-in', str(STATE / 'private.pem'), '-pubout', '-out', str(STATE / 'public.pem'))
    for name in ('private.pem', 'public.pem'):
        os.chown(STATE / name, 0, 10001)
        os.chmod(STATE / name, 0o440)
    docker('network', 'create', 'g6-backend')
    docker('run', '-d', '--name', 'g6-postgres', '--restart', 'unless-stopped',
        '--network', 'g6-backend', '--memory', '1536m', '--env-file', str(STATE / 'postgres.env'),
        '--mount', 'type=bind,src=/srv/g6/postgres,dst=/var/lib/postgresql/data',
        '--log-opt', 'max-size=10m', '--log-opt', 'max-file=3', PG)
    for _ in range(120):
        ready = subprocess.run(['docker', 'exec', 'g6-postgres', 'pg_isready', '-h', '127.0.0.1', '-U', 'postgres'], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        if ready.returncode == 0:
            break
        time.sleep(1)
    else:
        raise RuntimeError('PostgreSQL readiness timed out')
    for service, database, role, password, table in (
        ('users-service', 'identity', 'identity_runtime', identity_password, 'usuarios'),
        ('prospects-service', 'prospects', 'prospects_runtime', prospects_password, 'prospects')):
        sql('postgres', f"CREATE ROLE {role} LOGIN PASSWORD '{password}'; CREATE DATABASE {database};")
        for migration in sorted((ROOT / service / 'migrations').glob('V*.sql')):
            sql(database, migration.read_text())
        sql(database, f'REVOKE CONNECT ON DATABASE {database} FROM PUBLIC; GRANT CONNECT ON DATABASE {database} TO {role}; GRANT USAGE ON SCHEMA public TO {role}; GRANT SELECT, INSERT, UPDATE ON {table} TO {role};')
        if database == 'identity':
            sql(database, 'GRANT SELECT, INSERT ON account_access_audit TO identity_runtime;')
        else:
            sql(database, 'GRANT SELECT, INSERT, UPDATE ON capture_sync TO prospects_runtime;')
    common = {'SPRING_PROFILES_ACTIVE': 'prod', 'JWT_PUBLIC_KEY': 'file:/run/secrets/public.pem',
        'JWT_ISSUER': 'captura-g6-users', 'JWT_AUDIENCE': 'captura-g6-api',
        'JAVA_TOOL_OPTIONS': '-XX:MaxRAMPercentage=60.0 -XX:+ExitOnOutOfMemoryError'}
    users = dict(common, IDENTITY_JDBC_URL='jdbc:postgresql://g6-postgres:5432/identity',
        IDENTITY_DB_USER='identity_runtime', IDENTITY_DB_PASSWORD=identity_password,
        JWT_PRIVATE_KEY='file:/run/secrets/private.pem')
    envfile('users-service.env', users)
    envfile('prospects-service.env', dict(common, PROSPECTS_JDBC_URL='jdbc:postgresql://g6-postgres:5432/prospects',
        PROSPECTS_DB_USER='prospects_runtime', PROSPECTS_DB_PASSWORD=prospects_password))
    envfile('api-gateway.env', dict(common, USERS_SERVICE_URL='http://g6-users-service:8081',
        PROSPECTS_SERVICE_URL='http://g6-prospects-service:8082', API_DOCS_ENABLED='true'))
    envfile('bootstrap.env', dict(users, BOOTSTRAP_ADMIN_USERNAME='g6-admin',
        BOOTSTRAP_ADMIN_EMAIL='g6-admin@example.invalid', BOOTSTRAP_ADMIN_DISPLAY_NAME='Administrador Grupo 6',
        BOOTSTRAP_ADMIN_PASSWORD=admin_password, BOOTSTRAP_ADMIN_SITES='site-1,site-2'))
    for service in SERVICES:
        docker('build', '-t', 'g6-' + service + ':e2', str(ROOT / service))
    mounts = ['--mount', 'type=bind,src=/srv/g6/config/public.pem,dst=/run/secrets/public.pem,readonly']
    private = ['--mount', 'type=bind,src=/srv/g6/config/private.pem,dst=/run/secrets/private.pem,readonly']
    docker('run', '--rm', '--network', 'g6-backend', '--env-file', str(STATE / 'bootstrap.env'),
        *mounts, *private, 'g6-users-service:e2', '--bootstrap-admin')
    (STATE / 'bootstrap.env').unlink()
    envfile('admin-access.txt', {'username': 'g6-admin', 'password': admin_password})
    for service in SERVICES:
        args = ['run', '-d', '--name', 'g6-' + service, '--restart', 'unless-stopped',
            '--network', 'g6-backend', '--memory', '1536m' if service != 'api-gateway' else '1024m',
            '--read-only', '--tmpfs', '/tmp:rw,nosuid,size=128m', '--cap-drop', 'ALL',
            '--security-opt', 'no-new-privileges:true', '--stop-timeout', '45',
            '--log-opt', 'max-size=10m', '--log-opt', 'max-file=3',
            '--env-file', str(STATE / (service + '.env')), *mounts]
        if service == 'users-service':
            args.extend(private)
        if service == 'api-gateway':
            args.extend(['--publish', '127.0.0.1:8080:8080'])
        docker(*args, 'g6-' + service + ':e2')
    readiness()
    (STATE / 'installed').write_text('E2\n')
    print('Installation complete; credentials stay in /srv/g6/config/admin-access.txt (root only).')


if __name__ == '__main__':
    main()
