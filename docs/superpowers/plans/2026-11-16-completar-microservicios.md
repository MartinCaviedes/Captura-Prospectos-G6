# Completar microservicios — plan de implementación

**Objetivo:** base funcional de prospectos y gateway compatible con usuarios de Jeremy.
**Arquitectura:** usuarios conserva autenticación/JPA/JWT; prospectos posee su tabla y permisos; gateway solo valida y enruta. Sin llamadas de prospectos a usuarios.
**Tecnologías:** Java 21, Spring Boot 3.5.7, Maven Wrapper 3.9.11, JPA, H2/PostgreSQL, JWT RS256.
**Especificación:** solicitud adjunta del usuario y `docs/API_CONTRACTS.md`; este incremento implementa un subconjunto explícito bajo `/api/v1/prospects`, no el contrato completo de sincronización `/registrations`.

## Decisiones

- Partir de upstream/develop a7b1675, que ya incluye 9cd979b y el servicio de Jeremy.
- Reutilizar seguridad/rutas/tests existentes; no reintroducir usuarios en memoria.
- Propietario: claim `userId` UUID; `sub` sigue siendo username. Roles ADMIN, SUPERVISOR y CAPTADOR. Supervisor consulta/edita/anula solo sedes en `siteIds`; captador consulta/edita solo propios. Creación solo CAPTADOR y en su sede. ADMIN consulta/edita/anula todo.
- Modelo básico: UUID, RUT normalizado con dígito verificador, evento UUID, sede, nombres/apellidos, email/teléfono, consentimiento con versión/fecha y captador, timestamps y anulación con motivo/actor/fecha. Identidad/consentimiento/propietario inmutables en actualización.
- Duplicado activo RUT/evento: 409 protegido por restricción BD; al anular se libera la clave activa. Anulado no editable, repetir anulación devuelve 409. Control optimista de concurrencia JPA.
- Lista acotada con page/size (1–100), orden createdAt/id estable. No catálogos/asignaciones/idempotencia ni sincronización Android.
- JWT: preservar issuer captura-g6-users, audience captura-g6-api, RS256, 900 segundos, tolerancia 60s. Gateway y prospectos rechazan claims de identidad/roles/sedes incompatibles. No duplicar entidades entre servicios.
- Errores de prospectos code/message/fieldErrors/correlationId; 400/401/403/404/409 sin datos personales.
- Mantener el servicio de Jeremy sin cambios; usar siempre las contraseñas generadas por prepare-local.sh en la prueba funcional.

## Foco de revisión

Claims ausentes o mal tipados deben dar 401, no 500. Cruce de captadores/sedes no filtra datos. PUT no permite cambiar dueño/identidad. Duplicados concurrentes deben depender de BD. Cancelación conserva auditoría y libera unicidad.

## Tareas (ejecución directa autorizada por el usuario)

1. [x] Ejecutar baseline clean verify sin cambios; registrar resultado.
2. [x] Agregar pruebas HTTP de CRUD, propietario, sedes, validación, duplicados y anulación; comprobar fallo 404 antes de implementar.
3. [x] Implementar entidad Prospect/repository/service/controller, DTOs y errores; añadir JPA/H2/PostgreSQL con base independiente. Ejecutar toda la suite.
4. [x] Probar rechazo de claims inválidos en gateway/prospectos antes de implementar validadores; preservar pruebas criptográficas existentes. Ejecutar suite.
5. [x] Actualizar guía, preparar prueba funcional reproducible con tres JAR reales, login por gateway, CRUD/permisos/anulación. Compilar cada módulo independiente, clean verify y diff --check.
6. [ ] Revisar diff completo, commits en español, publicar rama al fork y PR a upstream/develop sin merge.

## Registro

- Working tree inicial limpio; copia nueva dentro de work/. Remotos origin=fork, upstream=original. Ambos fetch completados. Rama nueva creada desde upstream/develop.
- JDK 21 preparado localmente; no se modifica versión del repositorio.

- Baseline: 10/10 pruebas, BUILD SUCCESS. CRUD RED: 4 pruebas fallan 404; GREEN: 4/4. Claims RED: gateway/prospectos aceptaban token sin userId (200); GREEN: suite completa 16/16.
- Funcional: tres JAR, login y CRUD por gateway, permisos, duplicado, anulación y persistencia tras reinicio: OK.

- Compilación/pruebas independientes: usuarios 4/4, prospectos 8/8, gateway 4/4; los tres BUILD SUCCESS.
- Revisión independiente del diff: sin defectos introducidos bloqueantes. Detectó fallo preexistente de audiencia ausente.
- Corrección final: JWT sin aud reproducido (gateway 500/prospectos NPE), comprobación null añadida; prueba incompatibleIdentityClaimsAreRejected RED→GREEN en ambos servicios. clean verify 16/16 y funcional final OK.
- Observación menor diferida: no hay pruebas dedicadas de concurrencia; constraint única y @Version sí implementadas y conflictos secuenciales probados. PostgreSQL pendiente de validación de entorno.
- Usuarios de Jeremy y Android sin cambios; git diff --check limpio. Publicación y enlace de PR se registran en la entrega del chat.
