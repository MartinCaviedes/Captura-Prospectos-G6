# Hallazgos iniciales — auditoría parcial

Registro de descubrimiento y correcciones posteriores. El estado consolidado siguiente prevalece sobre las notas históricas. Fuentes referidas al commit a489882. Severidad expresa impacto potencial en producción; no afirma exposición actual en Internet.

## Estado consolidado de cierre

| ID | Severidad/prioridad | Estado y límite |
|---|---|---|
| ARQ-01 | Alta | Corregido, seed local excluido de prod y contraseñas externas |
| ARQ-02 | Alta | Corregido para defaults productivos y esquema inicial; evolución de datos previos pendiente |
| ARQ-03 | Media | Corregido, JWT sin aud devuelve 401 en usuarios |
| ARQ-04 | Alta para exposición pública | Parcial, límite local probado; política de borde/proxy/réplicas pendiente |
| ARQ-05 | Media | Parcial, timeouts, probes y drenaje gateway probados; tracing/carga/MVC pendientes |
| ARQ-06 | Media | Parcial, estado/sedes corregidos; listado completo y diferencias de errores conservados por contrato actual |
| ARQ-07 | Media | Parcial, PostgreSQL/V001/duplicado concurrente probados; edición/anulación y restauración pendientes |
| ARQ-08 | Media | Decisión de negocio pendiente sobre revocación inmediata; JWT actual conserva vigencia |
| ARQ-09 | Baja | Hallazgo documental histórico del PR #19; se prepara descripción completa para esta entrega |
| CAL-01/CAL-02 | Media | Corregidos, PATCH vacío y longitud de sedes con regresiones |
| OPS-01 | Alta | Corregido localmente, bootstrap único/concurrente y login probados |
| SEC-01 | Alta | Corregido el log de duplicado observado; cobertura de otros errores pendiente |
| SEC-03 | Alta para aprobación productiva | Parcial, parches verificados; soporte y asociaciones residuales permanecen abiertos |

La revisión independiente final no encontró defectos funcionales importantes en los cambios leídos. Detectó contradicciones documentales, corregidas en README y prerrequisitos. No se interpreta como certificación de ausencia de defectos. Las validaciones y limitaciones se consolidan en 04 y 07.

## Hallazgos iniciales y avances históricos

## ARQ-01 — Alta — seed demo activo fuera de desarrollo

- Servicio/ubicación: users-service, `service/UsuarioDataSeeder.java:16`, `:29`, `:43` y `:47`.
- Evidencia estática: @Component + CommandLineRunner, sin @Profile ni condición; contraseñas demo con fallback en constructor; crea administrador, supervisor y captadores si no existen.
- Impacto: con claves configuradas y BD nueva se pueden crear cuentas demo con contraseñas predeterminadas en un ambiente futuro. Cambiar perfil por sí solo no lo evita.
- Recomendación: limitar seed al perfil local explícito y exigir contraseñas externas sin fallback; probar arranque no local sin cuentas demo. No editar credenciales de una BD real.
- Estado actual: corregido localmente. SeedIsolationTest verifica cero cuentas con prod y local simultáneos. Ver la reanudación del informe de pruebas.

## ARQ-02 — Alta — configuración de desarrollo puede usarse en producción sin advertencia

- Servicios/ubicación: users-service y prospects-service, ambos `src/main/resources/application.yml:6` y `:12`.
- Evidencia: H2 en memoria y ddl-auto=update son defaults; sin perfil productivo ni migraciones versionadas.
- Impacto: pérdida de datos tras reinicio si se usan defaults; esquema mutable en arranque si solo se sustituye JDBC URL.
- Recomendación: perfil productivo que exija BD externa/credenciales y política de esquema no mutante; definir migraciones con compatibilidad verificada antes de ejecutarlas. Mantener desarrollo sencillo separado.
- Estado: pendiente. La volatilidad del default se deriva de JDBC mem; la prueba de reinicio usa H2 file y no acredita este default ni PostgreSQL.

## ARQ-03 — Media — audiencia JWT ausente no está protegida en usuarios

- Servicio/ubicación: users-service, `security/SecurityConfig.java:54`.
- Evidencia estática: jwt.getAudience().contains sin comprobación de null. El mismo patrón produjo 500/NPE en gateway/prospectos en la implementación previa y allí ya fue corregido; la prueba de usuarios actual cubre audiencia incorrecta, no ausente.
- Impacto: JWT firmado sin aud puede terminar en error interno al llamar directamente a usuarios. Gateway ya lo rechaza, por lo que el alcance es distinto al acceso por gateway. No equivale a bypass de autenticación.
- Recomendación: regresión firmada sin aud primero, devolver 401 y armonizar validaciones sin cambiar emisión ni contrato.
- Estado: pendiente de reproducción específica en usuarios; no presentar como explotación demostrada.

## ARQ-04 — Alta condicionada a exposición pública — login sin control de abuso en el repositorio

- Ubicación: usuarios AuthController/login y SecurityConfig; gateway SecurityConfig y application.yml.
- Evidencia: ruta pública con BCrypt; no política de rate limiting, bloqueo ni protección de abuso encontrada en el código/configuración versionados.
- Impacto: intentos masivos y consumo de CPU si se expone sin controles. Se desconoce si existe protección externa: no se inspeccionó cloud.
- Recomendación: acordar control proporcional y verificable antes de exponer login; no agregar infraestructura por suposición ni hacer pruebas invasivas.
- Estado: pendiente; riesgo de preparación, no incidente demostrado.

## ARQ-05 — Media — fallos parciales y operación sin política explícita

- Servicio/ubicación: api-gateway `config/GatewayRoutes.java:18`, `src/main/resources/application.yml`; application.yml de servicios de negocio.
- Evidencia: URLs externas sí configurables; no timeouts HTTP explícitos, pruebas de upstream lento/caído, política de logging sensible/correlación de solicitud o configuración explícita de readiness/shutdown encontrada. Frameworks pueden tener defaults; no se afirma ausencia total de timeout o apagado.
- Impacto: comportamiento y tiempos de fallo no controlados/documentados para operación futura.
- Recomendación: fijar tiempos razonables, probar indisponibilidad y timeout, health/readiness y cierre con señales sin reintentar escrituras automáticamente. Confirmar defaults antes de modificar.
- Estado: pendiente.

## ARQ-06 — Media — diferencias en errores y listados de usuarios

- Ubicación: usuarios `controller/StatusController.java:89`, `:129`, `service/UsuarioService.java:69`; prospectos `controller/ApiErrors.java`.
- Evidencia: usuarios devuelve listado completo via findAll y IllegalArgumentException como 400; prospectos pagina y distingue 409/404, con error DTO diferente.
- Impacto: listado sin límite crece con datos; errores inconsisten entre servicios. No demuestra fuga de password: Usuario usa @JsonIgnore para passwordHash.
- Recomendación: cubrir errores y límites de usuarios con pruebas, justificar cualquier cambio de contrato y no introducir DTOs/refactors cosméticos globales. Revisar validación de sedes y colisiones username/email en etapa 2.
- Estado: pendiente.

## ARQ-07 — Media — persistencia y concurrencia validadas parcialmente

- Ubicación: POM de usuarios/prospectos, entidad Prospect @Version/active_key, ProspectIntegrationTest.
- Evidencia: drivers y mecanismos presentes, pruebas con H2; sin integración PostgreSQL ni pruebas dedicadas de carreras concurrentes. Sin migraciones ni pipeline CI encontrado.
- Impacto: no se acreditan paridad PostgreSQL, evolución de esquema ni comportamiento concurrente real.
- Recomendación: validaciones locales aisladas sin BD real; agregar regresiones de riesgo y automatización antes de certificar.
- Estado: pendiente. No se diagnostica defecto de @Version o restricción única por ausencia de prueba.

## ARQ-08 — Media — política de revocación pendiente para operación

- Ubicación: usuarios `controller/AuthController.java`, métodos login/logout; `service/UsuarioService.java:110`.
- Evidencia: JWT de 900 segundos, logout solo responde confirmación, cambio de activo no invalida JWT emitido; no revocación implementada.
- Impacto: desactivar cuenta no termina inmediatamente acceso con token previamente emitido. Es comportamiento documentado, no regresión.
- Recomendación: registrar decisión de negocio sobre ventana aceptable y no agregar llamadas por petición o almacenes nuevos sin justificación.
- Estado: pendiente de decisión; cambiar semántica requeriría acuerdo explícito.

## ARQ-09 — Baja — descripción del PR fusionado sin evidencia completada

- Ubicación: PR #19 en GitHub; descripción conserva plantilla vacía.
- Impacto: dificulta revisar qué fue probado; no afecta ejecución.
- Recomendación: usar informes de esta auditoría y descripción preparada como evidencia para próximos cambios. No se modifica retroactivamente el PR sin autorización.
- Estado: pendiente, documental.

## Controles positivos comprobados

Separación de procesos/BD, JWT RS256 y claims en gateway/prospectos, preservación Bearer, propietario del token, filtros en BD, límites de prospectos, consentimiento mínimo, restricciones de unicidad, anulación auditada, @Version, validación y pruebas de aislamiento. No se observaron llamadas de prospectos a usuarios ni entidades compartidas.

## Todavía no evaluado

Vulnerabilidades de dependencias, seguridad exhaustiva de logs/serialización, recursos ante carga, carreras y namespace de identidad, política completa de permisos de usuarios, hardening de contenedores, TLS/proxy y opciones AWS. No se asignan resultados inventados a estas revisiones.

## Actualización posterior al checkpoint inicial

ARQ-03 (usuarios, SecurityConfig.jwtDecoder) quedó reproducido directamente mediante MockMvc y corregido en el árbol local. SEC-01 (alta, prospectos, src/main/resources/application.yml) expuso en logs el valor de la clave única en una prueba de duplicados. Ver evidencia y mitigación en 03-SECURITY-REVIEW.md.

Validación de operaciones administrativas de usuarios:

- CAL-01, media, corregido localmente. En StatusController, PATCH con objeto vacío se deserializaba con activo=false y respondía 200. El cliente podía desactivar una cuenta omitiendo el campo. EstadoUsuarioRequest exige ahora un Boolean no nulo y el controlador valida el DTO. La regresión comprueba rechazo 400 y que la cuenta sigue activa. Los clientes que envían un booleano mantienen su contrato.
- CAL-02, media, corregido localmente. StatusController.CrearUsuarioRequest admitía sedesAsignadas de 256 caracteres y la escritura fallaba con DataIntegrityViolationException. Se añadió @Size(max = 255), coincidente con la columna existente. La prueba assignedSitesRespectStorageLimit acepta 255 caracteres y rechaza 256 con 400. Se conserva el campo opcional y su comportamiento por defecto.


## ARQ-02 — actualización del 17/11/2026

Estado corregido para los valores predeterminados de persistencia del perfil prod. Ambos servicios JPA incorporan application-prod.yml, requieren sus variables externas de conexión y usan validación de esquema con inicialización SQL desactivada. Las pruebas previas fallaron porque el perfil usaba H2 de desarrollo y generaba tablas. Tras el cambio, fallan los arranques sin configuración o sin tablas y funciona el arranque sobre esquema ficticio preparado. La prueba usa H2 aislado. Compatibilidad PostgreSQL, migraciones y revisión de sobrescrituras del entorno siguen pendientes. Un operador conserva la capacidad de sobrescribir propiedades Spring explícitamente.


## ARQ-05 — avance del 17/11/2026

Estado parcialmente corregido. Gateway ahora configura connect-timeout (2000 ms) y response-timeout (5 s) mediante variables externas, usando las propiedades vigentes comprobadas en los metadatos del JAR 4.3.0. Los valores iniciales son una decisión de implementación ajustable y requieren revisión según objetivos de latencia futuros. La regresión del upstream lento devolvía 200 antes y ahora 504 con un límite de prueba de 200 ms. La conexión rechazada devolvía 500 antes y ahora 503 con code UPSTREAM_UNAVAILABLE. El mensaje genérico evita comunicar el destino interno. UpstreamErrorHandler solo maneja ConnectException en la cadena causal y delega el resto al framework.

Ubicaciones afectadas: api-gateway/src/main/resources/application.yml, gateway/error/UpstreamErrorHandler.java y ApiGatewayIntegrationTest. Se documentan las dos variables en backend/.env.example y README. Las rutas conservan la ausencia de reintentos automáticos. No hay una prueba de conexión que agote su timeout, de interrupción de cuerpos, de carga o de reintentos de escrituras. Readiness, cierre controlado y correlación operativa siguen pendientes dentro de ARQ-05.

## ARQ-05 — salud y cierre controlado

Readiness/liveness habilitados y públicos solo en rutas exactas en los tres servicios. Las pruebas de rechazo de tráfico fallaban inicialmente con 401. Ahora comprueban readiness 503 al publicar REFUSING_TRAFFIC, liveness 200 y recuperación de readiness 200 sin componentes internos. En servicios JPA la readiness incluye db, en gateway incluye readinessState. No se simuló la caída de la BD en esta ronda.

server.shutdown=graceful queda explícito y SHUTDOWN_TIMEOUT controla el límite por fase con default 30s. El smoke usa readiness al arrancar, SIGTERM al detener y falla si necesita kill o no encuentra el mensaje de cierre. La prueba de señales cubre procesos sin solicitudes activas. El drenaje de peticiones largas y la coordinación con un balanceador siguen pendientes. Correlación y observabilidad operativa mantienen ARQ-05 parcialmente abierto.

## Contenedores y automatización — avance del 17/11/2026

Se añadieron Dockerfiles por servicio con JRE 21.0.13 fijado por digest, UID/GID 10001 y ENTRYPOINT exec. Los contextos se restringen con .dockerignore al JAR esperado. El build consume el artefacto Maven previamente probado. El test container-smoke.py valida imágenes locales con filesystem de solo lectura, tmpfs, red exclusiva y claves ficticias externas, y limpia sus recursos.

Se preparó backend-quality.yml con acciones oficiales fijadas a SHA, permiso contents:read, compilación/tests, smoke de procesos, build y smoke de imágenes. No contiene publicación de imágenes ni despliegue. La ejecución de GitHub Actions todavía no se ha realizado. No se incorpora Compose, dado que el script temporal ya prueba el enrutamiento entre contenedores y no requiere servicios adicionales. Queda pendiente análisis de vulnerabilidades tanto de dependencias Java como de las capas del runtime.

## OPS-01 — alta — aprovisionamiento inicial productivo pendiente

- Servicio y ubicación: usuarios, UsuarioDataSeeder, UsuarioService.crearUsuario y SecurityConfig de POST /api/v1/users.
- Evidencia de código: el seed está excluido del perfil prod y la creación por API requiere ADMIN. No se encontró otro procedimiento versionado de aprovisionamiento inicial.
- Impacto: una base productiva recién preparada carecerá de un administrador para iniciar la administración por API.
- Recomendación: implementar y probar un procedimiento de una sola ejecución, controlado por el operador, con hash BCrypt y sin credenciales en Git. Coordinarlo con migraciones y deshabilitarlo después de usarlo. Evitar un endpoint público de bootstrap.
- Estado pendiente. El bloqueo se deriva del diseño actual; no se intentó inicializar una base productiva. El procedimiento se recoge en 06-DEPLOYMENT-PREREQUISITES.md.

## SEC-03 — dependencia y runtime, 17/11/2026

Estado pendiente de remediación. El análisis local Trivy 0.76.2 encuentra avisos críticos/altos en bibliotecas de ejecución. El caso Spring Security 6.5.6 requiere evaluación prioritaria de actualización por CVE-2026-22732. La coincidencia Gateway CVE-2025-41243 tiene su precondición de Actuator gateway excluida en esta configuración. Ver 03-SECURITY-REVIEW.md y dependency-findings.csv para conteos, fuentes primarias, alcance y plan de actualización compatible. Esta ronda no modificó POM, versión de Java o imagen base.

## SEC-03 — remediación parcial y riesgo de soporte

El parent Boot 3.5.16 y Cloud BOM 2025.0.3 quedan verificados con 29 pruebas, ambos smokes y reanálisis de imágenes. Desaparecen del inventario CVE-2026-22732 y CVE-2025-41243; persisten otras asociaciones críticas/altas. Véase 03 y el CSV posterior para resultados comparables. La línea 3.5/2025.0 terminó soporte OSS según los avisos oficiales; aprobar una estrategia de soporte/migración sigue siendo un bloqueo productivo. Estado parcialmente corregido, pendiente de decisión y triage restante.

## SEC-03 — parches y evaluación contextual

Se verificaron overrides de Jackson 2.21.7, Netty 4.1.139.Final, Tomcat 10.1.61, pgJDBC 42.7.12 y bcprov 1.85. Después de 29 pruebas y smokes de JAR/contenedores, Trivy con las mismas bases reporta 0 HIGH Java en los tres servicios. Permanecen 2 CRITICAL Java por imagen asociados a funciones de Framework con precondiciones no encontradas en el código revisado y 1 MEDIUM log4j-api pendiente. Se conservaron los reportes y la clasificación original. El riesgo de soporte de la generación actual permanece abierto. Ver 03 para referencias primarias, condiciones y límites.

## ARQ-07 — PostgreSQL inicial y concurrencia de creación

Verificado localmente con PostgreSQL 17.12 y driver 42.7.12. Se agregaron scripts V001 transaccionales por base y postgres-smoke.py, incluido en el workflow preparado. Los tres JAR arrancan con prod/validate sobre ese esquema. El recorrido HTTP reproduce permisos, CRUD, anulación y datos conservados tras reiniciar prospectos. Dos POST simultáneos para RUT/evento idénticos devuelven 201/409 y dejan una fila activa. Roles runtime separados no reciben permisos DDL ni acceso de conexión a la base del otro servicio.

La repetición de V001 se rechaza y la versión registrada permanece. Las migraciones se aplican manualmente a una base vacía mediante psql. No existe runner con checksums automáticos ni conversión desde esquemas previos. Validación de restauración, TLS administrado y carreras de actualización/anulación sigue pendiente. Estado parcialmente corregido, con evidencia concreta para esquema inicial y creación concurrente.

## OPS-01 — bootstrap inicial corregido y verificado

Se añadió el modo explícito --bootstrap-admin al JAR de usuarios, antes de iniciar Spring HTTP. AdminBootstrap usa variables externas, las validaciones del DTO existente, BCrypt y una transacción JDBC con bloqueo exclusivo de usuarios. Fija ADMIN, exige tabla sin cuentas y rechaza repetición. No crea esquema ni expone endpoint. Espera de bloqueo limitada a 10s, mensajes de error genéricos y recursos cerrados.

postgres-smoke.py primero reprodujo la ausencia del modo CLI (el arranque anterior no terminaba). Tras implementarlo, validó entradas inválidas sin filas, dos ejecuciones simultáneas con códigos 0/1 y una sola cuenta ADMIN, rechazo posterior y login/autoridad administrativa por gateway. Suite JUnit 29/29. El control solo inicializa una base vacía y no sirve para recuperación de cuentas existentes. Documentado en README y prerrequisitos. Estado corregido localmente, pendiente de revisión y publicación final.

## ARQ-04 — protección básica local implementada

Gateway incorpora cuota configurable por peer y ventana, mapa acotado, 429 y Retry-After. Pruebas de unidad e integración cubren umbral, recuperación, memoria, spoofing de X-Forwarded-For y variantes de path. Los avisos y limitaciones por proxy/réplicas se documentan en 03 y README. Estado parcialmente corregido; la política de borde y las pruebas de carga quedan pendientes para exposición pública.
