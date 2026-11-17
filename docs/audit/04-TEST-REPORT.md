# Informe final de validación local

Fecha 17/11/2026. Base develop a489882 y cambios de audit/backend-produccion. Las pruebas usan datos ficticios, loopback, Docker local y almacenamiento temporal. Las ejecuciones se completaron sin usar bases reales ni infraestructura AWS.

## Entorno

JDK 21.0.13, Maven Wrapper 3.9.11, Boot 3.5.16, Cloud 2025.0.3. PostgreSQL de prueba 17.12 y pgJDBC 42.7.12. Docker local por unix socket. Trivy 0.76.2 verificado por checksum, con bases fijadas a las descargadas el 17/11 para las comparaciones. Las propiedades efectivas y parches están en POM e informe 03.

## Validación final ejecutada

| Comando o recorrido | Resultado observado |
|---|---|
| backend/mvnw -o -f backend/pom.xml -Dmaven.repo.local=<cache-local> clean verify | BUILD SUCCESS, 33 pruebas, cero fallos/errores/saltos |
| mvnw -f backend/<servicio>/pom.xml -DskipTests package, por módulo | Tres BUILD SUCCESS independientes; las pruebas se ejecutaron en el agregador |
| scripts/smoke-test.py | Tres JAR, login/JWT/identidad, CRUD, permisos, duplicado, anulación, reinicio y SIGTERM correctos |
| scripts/drain-smoke.py | Solicitud activa por gateway llegó al upstream, se envió SIGTERM, terminó con HTTP 200 y completó cierre graceful |
| scripts/postgres-smoke.py | V001, roles separados, prod validate, bootstrap inválido/único/concurrente, login ADMIN, CRUD, reinicio y creación duplicada concurrente correctos |
| docker build por servicio | Tres imágenes construidas desde JAR finales con base por digest |
| scripts/container-smoke.py | Tres contenedores no root/read-only, tmpfs, probes, login/creación y SIGTERM correctos, limpieza final |
| Trivy sobre las tres imágenes finales | Completado, por imagen 2 CRITICAL y 1 MEDIUM Java; 0 HIGH Java; SO 35 MEDIUM/16 LOW |
| git diff --check | Limpio |
| py_compile de scripts nuevos/modificados y lectura YAML del workflow | Correctos |

Se seleccionó JAVA_HOME local antes de las ejecuciones y se cargaron las variables generadas por prepare-local.sh sin imprimirlas. El modo offline de Maven reutilizó dependencias descargadas previamente. Se autorizó ejecución local con sockets/attach JVM donde lo requería el sandbox.

## Inventario JUnit

| Clase | Pruebas |
|---|---:|
| Gateway ApiGatewayIntegrationTest | 7 |
| Gateway LoginRateLimitFilterTest | 4 |
| Usuarios SecurityIntegrationTest | 8 |
| Usuarios SeedIsolationTest | 1 |
| Usuarios ProductionPersistenceTest | 2 |
| Prospectos ProspectIntegrationTest | 4 |
| Prospectos SecurityIntegrationTest | 5 |
| Prospectos ProductionPersistenceTest | 2 |
| Total | 33 |

Los métodos agrupan varias entradas/aserciones. No equivalen a 33 casos de entrada individuales. No se midió porcentaje de cobertura y no se configuró una meta arbitraria.

## Regresiones y evidencia de fallos previos

- Seed podía operar fuera de local. Prueba con prod+local confirma ausencia de cuentas demo.
- Usuarios con JWT firmado sin aud disparaba NullPointerException. La regresión ahora obtiene 401.
- PATCH de estado vacío respondía 200 y asumía false. Ahora obtiene 400 y conserva cuenta activa.
- Sedes de 256 caracteres disparaban error de persistencia. Ahora obtiene 400 y acepta el límite de 255.
- Gateway lento devolvía 200 sin el límite esperado y conexión rechazada 500. Ahora devuelve 504/503.
- Probes exactas devolvían 401. Ahora readiness sigue disponibilidad y liveness permanece UP durante REFUSING_TRAFFIC.
- Prod sin configuración usaba defaults de desarrollo. Pruebas ahora fallan por configuración ausente o schema vacío y pasan con esquema preparado.
- Bootstrap inexistente iniciaba el servidor y no terminaba. Ahora termina con un único ADMIN, rechaza repeticiones y datos inválidos y permite login real.
- Rate limiter se desarrolló con pruebas de umbral, ventana, mapa y spoofing. Una variante con parámetros de segmento detectó una omisión que se corrigió con la semántica PathContainer.
- El log de duplicado contenía RUT/evento sintéticos. Desapareció tras desactivar SqlExceptionHelper, conservando 409.

Las pruebas de gateway usan backends controlados para rutas, latencia y rechazo de conexión. Los scripts complementan esa cobertura con servicios y driver reales en PostgreSQL local. Las pruebas concurrentes cubren dos creaciones idénticas y dos bootstraps. El smoke PostgreSQL usa seed local solo para fixtures y luego ejecuta prod/validate. Los procesos temporales cierran y el contenedor de base se elimina.

## SCA y comparación

El análisis inicial tenía 393 filas/183 combinaciones únicas entre imágenes; los parches redujeron a 162/56. Se conservan CSV inicial, posterior a mantenimiento y posterior a parches. El análisis final confirma el mismo inventario residual. Las clasificaciones del scanner y el triage por condiciones de uso están en 03. El resultado de ejecución 0 no representa un umbral de seguridad satisfecho.

La base general indica UpdatedAt 2026-11-17T06:59:44Z. Se deshabilitaron actualizaciones general/Java en los análisis comparativos. No se certifica la cobertura completa de la JVM ni alcanzabilidad de todos los avisos. No se enviaron artefactos a un scanner remoto.

## Registro de evidencia local

Bajo work/, fuera de Git, se guardaron audit-final-verify.log, audit-final-independent-<servicio>.log, audit-final-smoke.log, audit-final-drain.log, audit-final-postgres.log, audit-final-docker.log, audit-final-container.log y audit-final-trivy-<servicio>.json/.log. Los logs de desarrollo de regresiones también permanecen en work/audit-*.log. La evidencia seleccionada y sin secretos se consolida aquí y en los CSV de docs/audit.

## Revisión y límites

Un revisor independiente leyó el diff de código/config/scripts/Docker/CI y no identificó defectos funcionales importantes. Señaló contradicciones documentales sobre PostgreSQL, migraciones y Docker que se corrigieron. El revisor no volvió a ejecutar los tests. La evidencia de ejecución corresponde al agente implementador.

Pendientes para aprobación productiva: carga distribuida, política de abuso detrás de proxy/réplicas, edición/anulación concurrentes, drenaje MVC, caída de BD, TLS administrado y restauración. El workflow está preparado pero su ejecución alojada depende de la entrega/los permisos de GitHub. No se ejecutaron pentest externo, operación cloud o despliegue. La recomendación de preparación está en 07.
