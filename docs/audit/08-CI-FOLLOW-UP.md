# Verificación posterior al merge — 17/11/2026

El PR #20 quedó fusionado en develop mediante 922c769bfd2a5724b22f394b3984d31afc285ab4. Los siete documentos y los 49 archivos de la auditoría están integrados. La primera ejecución alojada de Backend quality, [44105685231](https://github.com/MartinCaviedes/Captura-Prospectos-G6/actions/runs/44105685231), aprobó clean verify (33 pruebas), preparación local, smoke HTTP y drenaje activo; falló al preparar PostgreSQL. Las etapas de imágenes/contenedores quedaron omitidas en esa ejecución.

## CI-01 — Carrera en la disponibilidad de PostgreSQL

- Severidad: media; afecta la reproducibilidad del control de calidad.
- Archivo: backend/scripts/postgres-smoke.py, espera pg_isready anterior al SQL inicial.
- Evidencia: la imagen oficial arranca un servidor temporal solo por socket para initdb y después lo reinicia. La sonda sin host aceptaba ese servidor; el SQL podía competir con su apagado. Una regresión real con un hook initdb demorado reprodujo el inicio prematuro de SQL (pg_isready TCP devolvió 2).
- Corrección: exigir pg_isready -h 127.0.0.1 para esperar el servidor definitivo. Sin cambios de aplicación, contratos ni dependencias.
- Prevención: scripts/test_postgres_readiness.py ejecuta una base efímera con initdb demorado y comprueba que no empieza la preparación SQL antes de disponer de TCP; añadido a CI.
- Estado: corregido y validado localmente; falta confirmar el workflow alojado con este cambio.

## Validación de la corrección

- python3 scripts/test_postgres_readiness.py: falló antes del cambio por disponibilidad TCP prematura; pasó después (1 prueba).
- Con JAVA_HOME apuntando al JDK 21 y source .local/env.sh, python3 scripts/postgres-smoke.py: aprobado en PostgreSQL 17.12; incluye roles, V001, bootstrap concurrente, prod validate, CRUD, reinicio y duplicado concurrente.
- Una primera ejecución local sin seleccionar JAVA_HOME utilizó Java 17 y falló en el bootstrap; se repitió correctamente con Java 21, como exige el proyecto. No se atribuye ese resultado al código del backend.
- python3 -m py_compile y git diff --check: aprobados.
- No se modificó Java: las 33 pruebas y el build ya pasaron en la ejecución alojada del mismo contenido de aplicación.

La entrega de auditoría y su merge están confirmados. No se declara CI completamente aprobada hasta observar una ejecución verde del cambio. Se mantiene la recomendación de 07: no listo para producción por los riesgos allí documentados. No se realizaron despliegues, operaciones sobre bases reales ni infraestructura cloud. El seguimiento se entrega por PR separado hacia develop, sin merge automático.
