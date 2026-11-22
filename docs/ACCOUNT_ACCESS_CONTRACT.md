# Cuentas, registro y permisos · contrato de trabajo E2

Fecha: 21/11/2026. Base inspeccionada: `97e6e0e` de develop.
Estado: candidato local con [backend](ACCOUNT_BACKEND_VALIDATION.md), [integración Android](ACCOUNT_ANDROID_VALIDATION.md) y [sesión offline](OFFLINE_SESSION_VALIDATION.md). No representa entrega publicada ni aprobación docente. La tabla del diagnóstico y el orden original inferiores conservan la trazabilidad del estado previo.
Alcance: RF01, RF10 y RF13 del [SRS](SRS.md), identidad y administración de E2/examen.

Especificación de este incremento: [OpenAPI 3.1](openapi/accounts.openapi.json), [guía de compatibilidad y validación](openapi/README.md) y [fixtures](fixtures/accounts/README.md). OpenAPI es la referencia de tipos, campos y respuestas; este documento explica las reglas de negocio.

## Decisiones de alcance

Conservamos el formulario de registro incorporado por el equipo. El registro crea una cuenta remota pendiente; únicamente el administrador habilita el acceso y asigna rol y sedes. Mantenemos ADMIN, SUPERVISOR y CAPTADOR: no añadimos un cuarto rol. Pendiente es un estado de cuenta, no un rol.

La recuperación de contraseña queda aplazada por decisión de Martín. No se implementan recuperación por correo, contraseñas temporales ni preguntas de seguridad. Para la entrega se usan cuentas ficticias con credenciales conocidas por sus titulares. Microsoft/Duoc es una integración futura, no disponible ni simulada mediante validación del dominio del correo.

Trello queda fuera del trabajo solicitado. La distribución prevista es APK release firmada y descargable desde GitHub; no exige publicar en Play Store. Geocodificación, sincronización de fichas completas y personalización de apodo/imagen son incrementos separados. Este contrato no modifica sus modelos ni los implementa.

## Evidencia del código actual

| Archivo | Capacidad que se conserva / diferencia por resolver |
|---|---|
| `backend/users-service/.../controller/StatusController.java` | Ya ofrece GET/POST users y PATCH estado; no ofrece revisión de solicitudes ni cambio de permisos. |
| `backend/users-service/.../service/UsuarioService.java` | Alta con BCrypt; crea cuentas activas y aplica site-1 por defecto. El registro público no debe heredar esos valores. |
| `backend/users-service/.../controller/AuthController.java` | Emite JWT RS256 de 14400 segundos (4 horas); no hay refresh ni revocación de tokens en logout. |
| `backend/users-service/.../security/SecurityConfig.java` | GET users permite también SUPERVISOR; el listado administrativo completo se restringirá a ADMIN. |
| `backend/users-service/.../model/RolUsuario.java` | Tres roles existentes. |
| `backend/users-service/db/migrations/V001__initial.sql` | activo boolean, rol no nulo y sedes en texto. No representa explícitamente PENDING. |
| `android/.../ui/access/DemoAccessScreen.kt` y `DemoAccessViewModel.kt` | Registro local con elección de rol/sedes; se reutiliza UI/validación, reemplazando esa decisión por registro remoto pendiente. |
| `android/.../data/remote/GatewayApiClient.kt` | Sesión volátil y ADMIN convertido a SUPERVISOR; falta representación diferenciada del administrador. |

Las rutas abreviadas con `...` son orientativas; las rutas canónicas completas se pueden localizar por nombre bajo backend/users-service/src/main/java/io/github/grupo6/users/ y android/app/src/main/java/io/github/grupo6/capturaprospectos/.

## Estados y transiciones

| Estado API | Significado | Acceso |
|---|---|---|
| PENDING | Registro recibido; todavía sin asignación administrativa | Sin JWT operativo, sin eventos ni prospectos |
| ACTIVE | Cuenta habilitada con permisos válidos | Según rol y sedes |
| DISABLED | Acceso deshabilitado por administración | Sin nuevos inicios de sesión; conservación de datos |

Transiciones permitidas: registro → PENDING; PENDING → ACTIVE; PENDING → DISABLED; ACTIVE → DISABLED; DISABLED → ACTIVE. La habilitación y las asignaciones se confirman en una única operación transaccional. Rehabilitar requiere revisar los permisos efectivos. No se borra una cuenta ni sus capturas al desactivarla.

En PENDING la respuesta pública lleva role=null y siteIds=[]. El modelo interno puede conservar CAPTADOR como valor provisional por compatibilidad, pero no concede facultades mientras status no sea ACTIVE. No se asigna site-1 implícitamente. CAPTADOR/SUPERVISOR activos requieren al menos una sede válida; ADMIN tiene alcance global explícito, no inferido de una lista vacía para otros roles.

Para datos anteriores: activo=true → ACTIVE y activo=false → DISABLED, conservando identidad, rol y sedes. No transformar cuentas existentes en pendientes ni fusionar cuentas de dispositivos por coincidencia de correo sin autenticación remota. Preparar migración incremental V002 y evolución Room por separado; no editar V001 ni resetear bases existentes. La migración comprobará cuentas activas sin asignaciones válidas y no inventará permisos para repararlas.

## Matriz de permisos objetivo

Todas las acciones requieren ACTIVE, salvo registro e inicio de sesión. El backend aplica permisos independientemente de los botones visibles.

| Acción | CAPTADOR | SUPERVISOR | ADMIN |
|---|---|---|---|
| Registrarse | Público, sin seleccionar permisos | Público, sin seleccionar permisos | No existe autorregistro ADMIN |
| Consultar identidad propia | Sí | Sí | Sí |
| Consultar/gestionar todas las cuentas | No | No | Sí |
| Habilitar/desactivar y asignar permisos | No | No | Sí, con restricciones inferiores |
| Seleccionar evento | Solo asignados y de sus sedes | De sus sedes | Todos, para administración |
| Crear/editar eventos y asignar captadores | No | Solo sus sedes y captadores autorizados | Global |
| Crear captura | Solo evento asignado | No por herencia implícita | No por herencia implícita |
| Consultar registros | Propios | De sus sedes | Global |
| Editar captura pendiente local | Propia, según reglas RF07 | Según alcance | Según alcance |
| Corregir captura ya sincronizada | Solicita corrección; no edición directa | De sus sedes, con motivo | Global, con motivo |
| Anular captura remota | No | De sus sedes, con motivo | Global, con motivo |
| Resumen | Propio | Por sedes autorizadas | Global |
| Personalizar apodo/imagen | Solo propia | Solo propia | Solo propia |

ADMIN no se convierte a SUPERVISOR en Android. No recibe automáticamente facultades de captador: la creación remota actual ya exige CAPTADOR. La edición remota actual admite captador propietario; debe alinearse con RF07 antes de declarar completa la integración. Gestión remota de eventos/asignaciones y solicitudes de corrección siguen pendientes en otros incrementos, no están implementadas por esta matriz.

La pantalla administrativa permite asignar CAPTADOR o SUPERVISOR; no permite crear/promover otros ADMIN desde registro o formulario general. La primera cuenta ADMIN usa el bootstrap existente. Se impide desactivar la propia cuenta administradora y se preserva al menos un ADMIN activo incluso ante solicitudes concurrentes. El procedimiento excepcional para nuevos administradores queda fuera del incremento.

## Contrato HTTP objetivo

Prefijo `/api/v1`. JSON camelCase. Esta tabla define cambios propuestos, no rutas operativas nuevas. El contrato existente de creación administrativa se conserva; el registro público usa un DTO independiente que no acepta rol, sedes ni estado. Antes de implementar, completar y validar OpenAPI y fixtures contra esta semántica.

| Método y ruta | Autorización | Petición / resultado |
|---|---|---|
| POST `/auth/register` (nuevo) | Pública, con limitación de frecuencia | displayName, email, password → 201 RegistrationReceipt; sin JWT |
| POST `/auth/login` (existente, adaptar) | Pública | username (usuario/correo) y password → 200 TokenResponse solo ACTIVE |
| GET `/admin/sites` (nuevo en el candidato) | ADMIN | Catálogo F2027 de sedes habilitables; misma fuente usada para validar asignaciones |
| GET `/me` (existente, adaptar) | Bearer | Identidad y estado/asignaciones actuales leídos de BD, no solo claims |
| GET `/users?status=PENDING&page=0&size=20` (adaptar) | ADMIN | Página de UserSummary, orden createdAt/id, size 1–100; filtro status opcional |
| GET `/users/{id}` (existente, adaptar) | ADMIN | UserSummary o 404 |
| PATCH `/users/{id}/access` (nuevo) | ADMIN | status, role, siteIds, expectedVersion → UserSummary actualizado |
| POST `/users` (existente) | ADMIN | Alta administrativa existente; alinear restricciones, validación y DTO de salida |
| PATCH `/users/{id}/estado` (existente) | ADMIN | Compatibilidad: delega en las mismas reglas; no puede saltarse habilitación ni protección de ADMIN |

RegistrationReceipt: userId, status=PENDING, message. UserSummary: id, username, email, displayName, status, role (null en PENDING y en solicitudes desactivadas sin asignaciones), siteIds, version, createdAt, updatedAt. Nunca devolver password/hash. Para registro público username se deriva del email normalizado; el login admite los usernames anteriores. Ampliar el límite actual de username de 120 a 160 donde corresponda para no aceptar un correo que luego no pueda usarse para entrar. Nombre 2–120, email válido hasta 160, password 6–128 con el límite real de BCrypt validado en bytes (máximo 72 bytes UTF-8); no truncar contraseñas silenciosamente.

PATCH access acepta ACTIVE o DISABLED, role CAPTADOR/SUPERVISOR y siteIds sin duplicados; permite role=null y siteIds=[] solo al desactivar una solicitud sin permisos. ACTIVE siempre requiere rol y sedes. IDs de sede deben existir y estar habilitados; no basta con comprobar su forma. Se envía el conjunto completo, no adiciones implícitas. ADMIN objetivo no es editable por esta operación. expectedVersion se comprueba incluso para una operación sin cambios y se incrementa una vez por cambio. Si versión y contenido coinciden, devuelve la misma versión, sin auditoría duplicada. Dos aprobaciones con la misma versión no pueden sobrescribirse silenciosamente.

PATCH estado añade expectedVersion obligatorio: conserva la ruta, no compatibilidad completa con cuerpos antiguos. No aprueba pendientes ni rehabilita cuentas sin asignaciones válidas; esas operaciones deben usar access. GET /me responde con el estado actual de BD incluso si la cuenta está DISABLED y el token anterior todavía es válido, para que Android bloquee operaciones. Esto no revoca el token en otros servicios.

Errores mínimos: 400 INVALID_REQUEST (incluye campos de permisos inyectados en registro), 401 INVALID_CREDENTIALS / TOKEN_EXPIRED, 403 ACCOUNT_PENDING / ACCOUNT_DISABLED / ACCESS_DENIED, 404 USER_NOT_FOUND, 409 ACCOUNT_EXISTS / VERSION_CONFLICT / ADMIN_PROTECTED, 429 RATE_LIMITED, 503 SERVICE_UNAVAILABLE. Formato code, message y fieldErrors opcional; sin claves, tokens, contraseñas ni stacktrace. El estado de cuenta en login solo se revela después de comprobar contraseña; las credenciales incorrectas devuelven siempre 401. La respuesta de duplicado hace visible la existencia de una cuenta: decisión acotada a la demo académica, revisar antes de uso institucional.

La contraseña solo se envía por HTTPS fuera del entorno local, se guarda con BCrypt en backend y no se registra en logs. Registro con falta de conexión falla de forma recuperable sin crear una cuenta local activa. La política de rate-limit cubrirá tanto login como registro.

## Sesión y acceso sin conexión

El JWT actual dura 14400 segundos (4 horas), ampliado desde 900 para permitir continuidad durante la jornada; los servicios confían en sus claims. Cambiar rol/sede/estado no revoca tokens ya emitidos. Un token anterior podría seguir autorizado hasta su expiración, más la tolerancia de reloj de 60 segundos. No se declara desactivación inmediata ni cierre remoto de sesiones. Android consulta /me al restaurar/volver a primer plano y antes de sincronizar; esa medida no implementa revocación en todos los servicios. El almacenamiento cifrado, límites y pruebas están en [sesión recordada](REMEMBERED_SESSION.md). No se añade refresh ni se almacena la contraseña.

Android consulta /me al volver a primer plano o recuperar conexión, si conserva un JWT vigente. Cambio de estado, rol o sedes requiere nuevo acceso; mientras revalida mantiene bloqueada la cabecera de transmisión. Sin JWT requiere login. Un 401/403 de revalidación no se transforma en acceso local exitoso. La comprobación antes de cada envío de prospectos y de una futura cola sigue pendiente en el incremento de sincronización. Logout limpia la sesión del cliente y no implica revocación criptográfica en otros servicios.

El primer login requiere conexión. El candidato conserva una autorización local de CAPTADOR/SUPERVISOR hasta 12 horas desde la verificación del login, separada del JWT online de 4 horas. Su archivo offline se cifra con AES-GCM/Android Keystore sin guardar contraseña, hash ni JWT; usa tiempo monotónico e identificador de arranque. Reabrir la app durante el mismo arranque permite restauración explícita si no hay red; reiniciar el dispositivo o no poder verificar el arranque exige login con conexión. La restauración online utiliza otro archivo cifrado con JWT y requiere validar /me antes de autorizar. No se renueva ninguna ventana al consultar /me. Vencimiento y logout bloquean el acceso sin borrar Room. No hay administración ni registro offline y no puede conocerse una desactivación remota hasta reconectar. El alcance y las pruebas offline están en OFFLINE_SESSION_VALIDATION.md.

## Flujo de pantallas

1. Acceso → Crear cuenta: reutilizar formulario, retirar selectores de rol/sede y enviar al servidor.
2. Registro correcto → Solicitud pendiente: explicar que el administrador habilitará la cuenta y ofrecer Volver al acceso. Sin entrar a la aplicación operativa.
3. Login pendiente/desactivado → mensaje específico después de validar credenciales. Sin pestañas de captación.
4. Login activo → navegación según rol; ADMIN incluye Usuarios con filtros Pendientes/Activas/Desactivadas.
5. Detalle de usuario → rol/sedes/estado; confirmar cambios, mostrar carga/errores y refrescar ante conflicto de versión.
6. Perfil → apodo/imagen en incremento posterior; no permite editar identidad, rol ni asignaciones.

## Criterios verificables antes de cerrar el incremento

- Registro crea exactamente una cuenta PENDING, sin JWT ni sede asignada y sin selección de rol; rechaza campos privilegiados, duplicados y contraseña fuera de límites.
- Registro no concede acceso aunque el cliente altere la petición. Pendientes/desactivados no reciben token operativo; claves incorrectas no revelan el estado.
- ADMIN lista y habilita solicitudes con asignaciones válidas. CAPTADOR/SUPERVISOR reciben 403 al invocar directamente rutas administrativas.
- Activación y permisos son atómicos; dos peticiones con la misma versión causan un único cambio y un conflicto. Se protege la propia cuenta ADMIN y el último administrador.
- Un token válido, vencido, con otro rol o con otra sede produce los resultados definidos. Se mide/documenta el límite de revocación, incluida tolerancia de reloj.
- No se pierde Room, borrador ni cola ante logout, desactivación, fallo de red o vencimiento. No se permite elevar privilegios mediante fallback local.
- Migraciones conservan cuentas previas y relaciones; no crean cuentas de demostración en prod ni cambian permisos silenciosamente.
- Respuestas, auditoría y logs no contienen contraseñas/hash. Auditoría administrativa registra actor, objetivo, transición, asignaciones y fecha sin duplicar datos personales innecesarios.
- Verificación HTTP real y recorrido Android de registro → habilitación → login con cada rol. Las pruebas existentes siguen pasando; nueva evidencia separada de los resultados históricos.

## Orden del siguiente incremento

1. Revisar este contrato, cerrar política offline frente a SRS y completar OpenAPI/fixtures (válidas e inválidas) antes de implementar consumidores.
2. Backend: migración incremental, estado/versionado y DTO; registro pendiente, permisos administrativos y pruebas de autorización/concurrencia.
3. Android: DTO y repositorio remoto, registro sin permisos, pantalla pendiente y ADMIN diferenciado, gestión de usuarios.
4. Validación de extremo a extremo y actualización de API_CONTRACTS/SRS/SECURITY para que no queden dos contratos contradictorios. Luego continuar sincronización completa y restantes etapas.

El primer paso fue documental; los incrementos posteriores implementan backend/gateway/V002, cuentas Android y sesión offline según los informes enlazados. El orden anterior se conserva como trazabilidad. La sincronización completa continúa pendiente.
