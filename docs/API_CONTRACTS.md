# Borrador de contratos y reglas de integración

Alcance 02/10/2026: contratos remotos de E2 según [ENTREGAS](ENTREGAS.md). No bloquean E1; sus interfaces/modelos locales se revisan por separado. Una conexión detectada en E1 no activa estas rutas ni acredita una integración. API externa de evaluación pendiente de selección y contrato separado de las APIs propias.

Versión de diseño 0.1, 30/09/2026. Backend todavía inexistente. Los siguientes son contratos propuestos, no endpoints comprobados. Antes de implementar cliente y servidor en paralelo, crear y revisar `openapi.yaml` con tipos, required/nullability, enums, ejemplos y errores. Este documento explica semántica; no será una segunda copia manual de los esquemas.

## Convenciones

Prefijo `/api/v1`; JSON en camelCase y nombres técnicos en inglés, mensajes al usuario en español. IDs UUID opacos; RUT como string normalizado, nunca número. Tiempos en UTC ISO 8601 y zona IANA del evento separada. Coordenadas numéricas validadas, null si ausentes; sin valores centinela. Cada lista paginada tiene orden estable, cursor y límite máximo documentados.

Errores: `code`, `message`, `fieldErrors`, `correlationId`; nunca stacktrace ni eco de datos sensibles. Un error no se clasifica por comparar su texto traducido. Documentar 400/401/403/404/409/422/429/5xx. Política de timeout/backoff se fija en código de infraestructura, no en cada pantalla.

## Operaciones propuestas

| Servicio | Método y ruta | Autorización y finalidad |
|---|---|---|
| Identidad | POST `/auth/login` | Validar credenciales del prototipo, devolver access/refresh y expiraciones |
| Identidad | POST `/auth/refresh` | Rotar refresh; revocar sesión si corresponde |
| Identidad | POST `/auth/logout` | Revocar refresh/sesión; no borrar capturas |
| Identidad | GET `/me` | Identidad y roles actuales |
| Captación | GET `/assignments` | Sólo asignaciones accesibles al usuario autenticado |
| Captación | GET `/catalog?siteId=...&version=...` | Snapshot/versionado de oferta por sede |
| Captación | POST/GET `/events` | Crear (supervisor) / listar con filtro de sedes autorizadas |
| Captación | PATCH `/events/{id}` | Supervisor y versión esperada; no reescribir snapshots históricos |
| Captación | PUT `/events/{id}/assignments/{userId}` | Supervisor de sede asigna/retira, con auditoría |
| Captación | POST `/registrations` | Captador asignado crea inscripción idempotente |
| Captación | GET `/registrations/by-client-id/{clientCaptureId}` | Reconciliación, sólo propietario/supervisor autorizado |
| Captación | GET `/registrations` | Captador sólo propias; supervisor sólo su alcance |
| Captación | PATCH `/registrations/{id}` | Supervisor, versión esperada, motivo y campos permitidos |
| Captación | POST `/registrations/{id}/correction-requests` | Captador solicita corrección de inscripción propia |
| Captación | POST `/registrations/{id}/cancel` | Supervisor; anulación auditada, no borrado físico arbitrario |
| Captación | POST `/device-reports` | Usuario reporta contadores de su dispositivo/evento, con fecha |
| Captación | GET `/events/{id}/summary` | Resumen autorizado, frescura y dispositivos desconocidos |
| Captación | POST `/events/{id}/close` | Supervisor; versión y motivo de excepción si hay desconocidos |

Rutas relativas a cada servicio; URLs configurables sin secretos embebidos. El servidor determina captador desde identidad autenticada; compara claims y permisos efectivos, no acepta un `captorId` arbitrario. Creación de usuarios de demo se hace mediante seed local, no un endpoint público de autoasignación de roles.

## Creación y duplicados

POST inscripción lleva `clientCaptureId`, `clientRevision`, `eventId`, snapshot de persona/contacto, IDs de intereses, consentimiento y contexto de ubicación. `Idempotency-Key` identifica usuario/captura/revisión y se asocia a hash del contenido.

Reglas del servidor:

1. Validar identidad, asignación, sede, consentimiento, campos y catálogo. Una carrera inactiva en versión nueva requiere una política explícita: devolver error corregible con motivo, conservando el pendiente; no sustituirla automáticamente.
2. Misma clave y mismo contenido: devolver el resultado previamente confirmado, sin una nueva fila. Misma clave con otro contenido: 409 `IDEMPOTENCY_PAYLOAD_MISMATCH`.
3. En la misma transacción persistir persona si procede, inscripción, intereses, auditoría mínima y resultado idempotente. Restricción única sobre inscripción activa persona/evento.
4. Mismo RUT/evento desde otro dispositivo: 409 `DUPLICATE_EVENT_REGISTRATION`; no enviar información del otro captador al que carece de permiso. Supervisor revisa y, si aplica, combina intereses con auditoría; no perder el intento local antes de resolverlo.
5. Misma persona en otro evento: permitir inscripción nueva. No actualizar globalmente los snapshots pasados.
6. ACK identifica ID remoto, clientCaptureId, revisión, version y fecha de recepción. Android sólo marca sincronizado el snapshot confirmado.

Para E2 se propone envío por registro (no batch): reduce ambigüedad de éxitos parciales. Si se añade batch, el contrato debe mantener resultados por elemento.

## Autenticación y autorización

Propuesta: access token JWT de corta duración emitido por identidad, verificado por captación mediante clave pública; refresh rotatorio guardado hashed en servidor. Fijar issuer/audience/algoritmos y expiraciones en la especificación de seguridad; no inventar un protocolo OAuth completo ni usar claves simétricas embebidas en Android.

La vigencia offline local de SRS no extiende validez de un token en servidor. Al reconectar, validar sesión y permisos antes de transmitir. Si una asignación cambió, devolver un error recuperable/revisión con contexto, no descartar el registro.

## Control de concurrencia y auditoría

PATCH requiere `expectedVersion`. Si otro supervisor modificó el dato, devolver 409 `VERSION_CONFLICT` y pedir refrescar/revisar. Actor, fecha y campos modificados quedan auditados en almacenamiento protegido. No guardar valores anteriores en logs de consola ni en notas Git.

El endpoint de reconciliación permite conocer si un envío de resultado incierto ya existe. No habilitar una edición que cambie su snapshot hasta confirmar el estado. RUT/evento son identidad de inscripción: corregirlos mediante flujo supervisado de anulación/recreación enlazada, no PATCH libre.

## Cierre del contrato antes de código

Tarea E2-BASE-02 del plan: OpenAPI validado; fixtures válidas/inválidas; enums alineados con SRS; longitudes; error por campo; timestamps; paginación; idempotencia; permisos; correcciones y versiones. Un revisor revisa el contrato y luego Android/backend trabajan contra esa versión. Ninguna ruta de esta tabla se declara implementada sólo por documentarla.
