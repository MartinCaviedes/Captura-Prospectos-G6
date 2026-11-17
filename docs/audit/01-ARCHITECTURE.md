# Arquitectura real del backend

Auditoría del 17/11/2026 sobre develop a489882a9d9893e4c2e329f9c5edd0f44ee6bbbb y cambios locales de audit/backend-produccion. El PR #19 de la implementación base está fusionado. Alcance exclusivo de backend. Las referencias de archivos son relativas al repositorio.

## Mapa de procesos

```text
Cliente HTTP → api-gateway :8080
                 ├→ HTTP/Bearer → users-service :8081 → base identidad
                 └→ HTTP/Bearer → prospects-service :8082 → base prospectos

Usuarios firma JWT RSA. Cada proceso verifica la pública.
Prospectos obtiene identidad/roles/sedes del token, sin consultar usuarios por petición.
```

| Módulo | Responsabilidad | Componentes principales |
|---|---|---|
| api-gateway | Enrutamiento, validación JWT, límite local de login y timeouts | GatewayRoutes, SecurityConfig, IdentityClaimsValidator, LoginRateLimitFilter, UpstreamErrorHandler |
| users-service | Identidad persistida, BCrypt, roles, login/JWT y administración | AuthController, StatusController, UsuarioService, UsuarioRepository, Usuario, UsuarioDataSeeder |
| prospects-service | Prospectos, propiedad, sedes, edición y anulación | ProspectController, ProspectService, ProspectRepository, Prospect, ApiErrors, SecurityConfig |

El agregador Maven produce tres JAR y cada módulo se empaqueta independientemente. Gateway no accede a BD. No hay entidades/repositorios compartidos, claves foráneas entre bases, mensajería ni llamadas de prospectos a usuarios. Destinos HTTP configurables, Bearer conservado y Cookie eliminado.

## Tecnologías verificadas

Java 21, Maven Wrapper 3.9.11 con checksum y Boot 3.5.16. Cloud BOM 2025.0.3/Gateway WebFlux 4.3.5 en gateway y Spring MVC/JPA en negocio. Hibernate y Hikari gestionados por Boot. Security 6.5.11 y Framework 6.2.19. Parches justificados en los POM para Jackson 2.21.7, Netty 4.1.139.Final, Tomcat 10.1.61 y pgJDBC 42.7.12; bcprov 1.85 se gestiona en gateway. Ver 03 para justificación, SCA y riesgo de soporte.

H2 está incluido para desarrollo/tests. PostgreSQL 17.12 se verificó en Docker local. Las imágenes Java usan Temurin JRE 21.0.13 por digest, UID/GID 10001 y ENTRYPOINT exec. `.dockerignore` limita contexto al JAR y Dockerfile. El script container-smoke valida filesystem de solo lectura y tmpfs. Cada imagen se construye desde el JAR previamente probado.

## Endpoints

Prefijo /api/v1 para API de negocio. Fuentes en los controladores y GatewayRoutes.

| Servicio | Operaciones |
|---|---|
| Los tres | GET /actuator/health, /actuator/health/liveness y /actuator/health/readiness públicos, detalles ocultos |
| Usuarios | POST /auth/login público, POST /auth/logout autenticado, GET /me autenticado |
| Usuarios | GET /users y /users/{id} ADMIN/SUPERVISOR; POST /users y PATCH /users/{id}/estado ADMIN |
| Usuarios | GET /admin/status ADMIN, también dirigido por gateway |
| Prospectos | POST/GET /prospects, GET/PUT /prospects/{id}, POST /prospects/{id}/cancel |
| Prospectos | GET /prospects/status autenticado; GET /admin/status ADMIN solo al servicio directo |

Gateway conserva familias históricas assignments/catalog/events/registrations/device-reports que carecen de controladores de negocio. No se consideran funciones implementadas. Sus probes son propias y no reenvían actuators internos.

## Persistencia y transacciones

Usuario se guarda en usuarios, ID string UUID, username/email únicos, BCrypt excluido de JSON, rol ADMIN/CAPTADOR/SUPERVISOR, CSV de sedes y estado. UsuarioRepository busca username o email ignorando mayúsculas. UsuarioService delimita transacciones. El seed de cinco cuentas demo solo se activa con local y queda excluido por prod. Contraseñas externas obligatorias.

Prospect almacena UUID, RUT, evento UUID, sede, captador UUID, nombres/contacto, evidencia mínima de consentimiento, fechas y anulación. active_key impone unicidad del RUT/evento activo y se libera al anular. @Version habilita concurrencia optimista. Los filtros de propietario/sede se aplican en BD antes de paginar (size 1–100, orden createdAt/id). Sin asociaciones JPA que generen N+1 por relaciones.

CAPTADOR crea en sus sedes y consulta/edita propios. SUPERVISOR opera en sedes de su token y puede anular; ADMIN tiene alcance global. Propietario proviene de userId. Identidad, sede y consentimiento son inmutables por PUT. Evento/catálogo/asignación real e idempotencia quedan fuera de este incremento. Las entidades de prospectos cancelados permanecen en BD.

Los defaults de desarrollo son H2 en memoria y ddl-auto=update. Perfil prod exige JDBC/credenciales externas y usa validate con scripts SQL automáticos deshabilitados. Cada servicio tiene V001 transaccional y tabla schema_version, aplicadas por un operador. La repetición falla. No hay runner automático de checksums ni conversión de esquemas previos. En PostgreSQL temporal se validaron roles separados, CRUD/reinicio y duplicados concurrentes.

## Autenticación, errores y operación

Login consulta usuarios persistidos mediante DaoAuthenticationProvider y BCrypt. JWT RS256 con issuer/audience, 900 segundos, sub=username, userId UUID, roles/siteIds y datos de identidad. Gateway/prospectos validan estructura de claims además de firma/emisor/audiencia/vigencia. Usuarios rechaza audiencia ausente. Logout no revoca JWT y cambios de estado/rol no invalidan de inmediato tokens emitidos. Esta semántica permanece documentada.

La API MVC es stateless y usa Bearer sin CSRF de sesión. No hay CORS abierto. Gateway limita login por peer/instancia, configurable, con mapa acotado y 429/Retry-After. Usa forwarded-headers none. Detrás de proxies/réplicas se requiere una política adicional de confianza/control de borde. Usuarios debe permanecer privado.

El bootstrap explícito --bootstrap-admin opera sin HTTP sobre tabla usuarios vacía, con variables externas y lock transaccional. Solo una ejecución crea un ADMIN. Se probaron entradas inválidas, concurrencia, repetición y login administrativo. No permite recuperar cuentas de una base poblada.

Prospectos tiene errores code/message/fieldErrors/correlationId para formato, permisos, inexistencia y conflictos. Usuarios conserva sus respuestas existentes y validación ampliada de estado/sedes. Gateway traduce conexión rechazada a 503 y timeout de respuesta a 504. El correlationId de errores no es trazabilidad distribuida. Se silencia el logger SQL que registraba RUT/evento en duplicados; queda revisar otros escenarios bajo PostgreSQL/carga.

Timeouts gateway iniciales 2s conexión/5s respuesta. Cierre graceful y timeout por fase 30s configurables. Readiness incluye DB en negocio y estado de disponibilidad en gateway. El smoke de drenaje comprueba una petición activa durante SIGTERM en gateway. No hay prueba de carga, drenaje MVC o caída de BD.

## Ejecución y automatización

Requisitos locales JDK21, Bash/OpenSSL, Python3 y Docker para contenedores/PostgreSQL. prepare-local.sh genera claves/contraseñas en .local excluido de Git. Solo usuarios recibe privada. La guía backend/README.md describe variables y comandos. Los smokes usan ficticios, puertos loopback y almacenamiento temporal; no aceptan una base real del operador.

GitHub Actions preparado para PR/push develop y ejecución manual, con acciones oficiales por SHA, contents:read, Maven verify, smokes de JAR/drenaje/PostgreSQL y build/smoke de imágenes. Carece de publicación de imágenes, credenciales AWS y despliegue. La ejecución alojada sigue pendiente hasta la entrega autorizada. Los scripts sustituyen la necesidad de Compose en este alcance.

## Obstáculos productivos restantes

Consultar 02/03/05/06/07. Permanecen soporte OSS finalizado, triage residual de dependencias, política de abuso detrás de proxy/réplicas, adopción de datos previos, backups/TLS administrado, concurrencia de edición/anulación, observabilidad y dimensionamiento. No se inspeccionó ni modificó infraestructura AWS. La preparación local no constituye una aprobación de producción.
