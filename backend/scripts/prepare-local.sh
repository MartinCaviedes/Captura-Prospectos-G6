#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
umask 077
mkdir -p .local
if [[ ! -e .local/private.pem && ! -e .local/public.pem ]]; then
  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out .local/private.pem
  openssl pkey -in .local/private.pem -pubout -out .local/public.pem
elif [[ ! -s .local/private.pem || ! -s .local/public.pem ]]; then
  echo 'Par de claves incompleto: conservar y revisar .local antes de continuar.' >&2
  exit 1
fi
# Detectar pares inconsistentes sin mostrar material privado.
openssl pkey -in .local/private.pem -pubout | cmp -s - .local/public.pem
if [[ ! -e .local/env.sh ]]; then
  {
    printf 'export SPRING_PROFILES_ACTIVE=local\n'
    printf 'export JWT_PRIVATE_KEY=%q\n' "file:$(pwd)/.local/private.pem"
    printf 'export JWT_PUBLIC_KEY=%q\n' "file:$(pwd)/.local/public.pem"
    printf 'export LOCAL_ADMIN_PASSWORD=%q\n' "$(openssl rand -hex 24)"
    printf 'export LOCAL_CAPTOR_PASSWORD=%q\n' "$(openssl rand -hex 24)"
  } > .local/env.sh
fi
echo 'Configuración local preparada. Ejecutar: source .local/env.sh'
