# Arquitectura propuesta del prototipo

Actualización de evidencia 08/10: A06 en 978ca91 mantiene Room v3; 58 JVM y 28 instrumentadas aprobadas. Revisión humana/integración pendientes. Los conteos de 24 en el antecedente del 06/10 identifican la suite anterior; ver [ANDROID_BASELINE](ANDROID_BASELINE.md) para resultados por incremento.

Incremento 06/10: Room v3 y contexto/catálogo sintético descritos en [DEMO_CONTEXT](DEMO_CONTEXT.md); inicialización explícita, sin autenticación real ni seed automático al migrar. Suite completa de 24 pruebas Android aprobada sobre v3; ver evidencia en ANDROID_BASELINE.

Actualizado 02/10/2026. Diseño del producto completo; implementación parcial de dominio y base Android, no del sistema completo. Complementa SRS 1.4 y [ENTREGAS](ENTREGAS.md). E1 implementa Android/local; E2 agrega fuentes remotas. Las secciones de servidor/cola describen el destino E2 y no obligan a añadirlo a E1.

## Construcción progresiva

E1: UI → ViewModel → dominio/repositorio → Room cifrado; GPS, cámara y observador de conectividad separados. Una interfaz se añade cuando define un límite usado, no para multiplicar capas vacías. E2 conserva ese recorrido e incorpora Retrofit, contratos remotos y outbox. El cliente no accede directamente a PostgreSQL.

Los perfiles de demo E1 están identificados y aislados de integración E2. No emitir tokens ficticios ni enviar automáticamente registros de perfiles sintéticos al activar backend. La evolución de Room debe preservar datos y contexto; no usar migración destructiva para resolver incompatibilidades. Ver límites de importación en ENTREGAS.

## Estructura y límites

Identidad técnica definida el 03/10 por delegación del propietario: proyecto `captura-prospectos-g6`, applicationId base y namespace `io.github.grupo6.capturaprospectos`. Flavor demo E1 añade `.demo`; debug añade `.debug`, manteniendo el namespace. demoDebug ya compila y sus metadatos confirman esa identidad; no hay flavor remoto todavía. Módulo Android en `android/` dentro de la carpeta del producto, con Git y publicación en una rama de revisión. Cambiar la raíz local no cambia package ni rutas relativas.

Propuesta de un repositorio de producto con `android/`, `backend/`, `docs/` e `infra/`. La ruta relativa Android vigente es android/; backend/ e infra/ se incorporarán al necesitarlos en E2. Android tendrá inicialmente un módulo `app`; paquetes por funcionalidad dentro de `ui`, `domain` y `data`, sin convertir cada RF en un módulo Gradle.

Para conservar la referencia del caso a microservicios Spring Boot, se proponen dos servicios desplegables: `identity-service` (usuarios, credenciales y emisión de sesión) y `recruitment-service` (sedes, eventos, asignaciones, catálogo, capturas, reportes y auditoría). No dividir un servicio por RF. PostgreSQL con bases/usuarios separados por servicio; en desarrollo pueden residir en el mismo servidor. No añadir broker, Kubernetes, discovery ni gateway obligatorio para P2.

Si el grupo/docente prefiere un único servicio modular, registrarlo como adaptación explícita antes de cambiar esta propuesta; no presentar un monolito como varios microservicios. Android y cada servicio tienen build independiente y contratos compartidos de API; no acceden directamente a la base de otro servicio.

## Android

Flujo: evento UI → ViewModel → regla/caso de uso cuando aporta valor → repositorio → Room y cola. La UI observa StateFlow inmutable derivado de datos locales. Las respuestas remotas actualizan Room transaccionalmente; no son una segunda lista que compite con la bandeja local.

- UI: pantallas, componentes, ViewModels, mapeo de errores y navegación. Estado local puramente visual permitido.
- Dominio: validadores, reglas de elegibilidad, modelos y cálculo de métricas; sin dependencias de Android.
- Datos: repositorios, DAO, entidades, DTO, mappers, cliente REST y sincronización. No reutilizar una entidad Room como contrato REST.
- Plataforma: cámara, ubicación, conectividad, reloj y almacenamiento de claves detrás de interfaces testeables. No concentrar todo en un `util` genérico.
- Inyección inicial por constructor y un contenedor pequeño explícito. Adoptar otro mecanismo sólo si resuelve un problema concreto; no introducir dos sistemas simultáneos.

El componente que realiza una operación bloqueante se responsabiliza de moverla al dispatcher adecuado. No ejecutar siempre todo en IO ni exigirlo alrededor de APIs ya no bloqueantes. Inyectar reloj/dispatchers cuando sea necesario para pruebas deterministas.

### Patrón de pantalla para la implementación E1

Concretado el 08/10/2026, compatible con la progresión docente de Compose a MVVM. Separar `FeatureRoute`, que obtiene el ViewModel y observa su estado con `collectAsStateWithLifecycle`, de `FeatureScreen`, que recibe UiState y callbacks. La pantalla puede previsualizarse con datos ficticios sin abrir Room ni acceder a servicios. El ViewModel expone StateFlow de sólo lectura, recibe dependencias por constructor y delega reglas al dominio/repositorio. Un contenedor y una fábrica explícita resuelven su creación; no copiar acceso global a repositorios en cada pantalla ni introducir Hilt por costumbre.

La navegación conecta callbacks y transporta identificadores mínimos; no llevar RUT ni formularios personales completos en rutas. Los identificadores se resuelven contra el repositorio y se maneja el registro inexistente. MainActivity instala tema y punto de entrada; no concentra validaciones o persistencia. Cada acción visible tiene comportamiento comprobable o una explicación explícita de que aún no está disponible.

El estado visual local puede permanecer en Compose. Los ejemplos de `rememberSaveable` no autorizan guardar datos personales sin consentimiento ni sustituir Room por estado de pantalla; revisar también SavedStateHandle y argumentos de navegación conforme a [SECURITY](SECURITY.md). Las dependencias para UI/navegación se incorporan sólo cuando la tarea las necesite, con versiones compatibles en el catálogo del proyecto. Estos patrones guían código nuevo, sin ordenar una reestructuración del dominio/persistencia ya probado.

## Modelo conceptual

Para implementar E1 prevalece la concreción local de [LOCAL_MODEL](LOCAL_MODEL.md), v0.1 pendiente de revisión cruzada. Person/outbox y modelos remotos de esta tabla describen el producto final; no exigir todas esas tablas en E1. [ANDROID_BASELINE](ANDROID_BASELINE.md) registra build, 58 JVM y 24 instrumentadas aprobadas sobre Room v3 en Android 14/API 34. Acredita esas comprobaciones; UI y RF completos siguen pendientes.

| Entidad | Responsabilidad |
|---|---|
| User / Role / UserSiteScope | Identidad y alcance de autorización |
| Site / Event / Assignment | Contexto autorizado y vigencia |
| CatalogVersion / Program / Offering | Programas/versiones con IDs estables; Offering remoto sólo cuando exista mapa de oferta verificado, sin exigirlo para preferencias E1 |
| Person | Identidad por RUT normalizado; sin exponer coincidencias a usuarios no autorizados |
| Registration | Inscripción por persona/evento; snapshot de contacto, consentimiento, captador y contexto |
| RegistrationInterest / SitePreference | 1–3 programas únicos priorizados y sedes opcionales independientes según F2027; no convertir preferencias en oferta confirmada |
| LocalDraft | Registro consentido pero incompleto; nunca enviado como inscripción |
| OutboxOperation | Snapshot inmutable, clave idempotente, revisión e intentos |
| DeviceReport | Última comunicación, contadores y contexto; puede quedar obsoleto |
| AuditEvent / CorrectionRequest | Correcciones y anulaciones supervisadas |

Una inscripción de otro evento conserva su snapshot; actualizar un teléfono no modifica retrospectivamente todas las inscripciones. El RUT no es un endpoint público de búsqueda ni una credencial. No usarlo como PK visible en URLs.

## Estados separados

La validez y el transporte son dimensiones diferentes:

- Estado de captura: `DRAFT`, `NEEDS_CONTEXT`, `READY`, `CANCELLED`.
- Estado de transporte: `NOT_QUEUED`, `PENDING`, `SENDING`, `RETRY_WAIT`, `REJECTED`, `CONFLICT`, `SYNCED`.

Sólo `READY` y consentido puede entrar en cola. Borrador/anulado no significa falla de red. Los estados desconocidos de otros dispositivos tampoco son estados de una inscripción.

Transiciones mínimas:

| Situación | Acción |
|---|---|
| Guardado válido | Transacción de captura READY + operación PENDING |
| Worker obtiene trabajo | Claim transaccional; SENDING y snapshot/version fijos |
| Servidor confirma commit | ACK de ID/revisión esperado → SYNCED |
| Timeout tras enviar | RETRY_WAIT, conservar mismo snapshot y clave; no habilitar edición hasta reconciliar |
| Worker muere | Lease vencido recuperable → reintento del mismo snapshot; nunca perder trabajo |
| Rechazo definitivo por campos | REJECTED; mostrar errores; editar y crear nueva revisión sólo después de rechazo confirmado |
| Coincidencia de negocio | CONFLICT; resolver explícitamente sin sobrescribir |
| Anulación antes de envío | CANCELLED y cancelar operación en transacción; si hay envío incierto, reconciliar primero |

La tabla de transiciones corresponde a E2. En E1, sólo una captura completa, consentida y con contexto válido confirma READY + NOT_QUEUED, sin outbox ni Worker ficticio. Los borradores consentidos explícitos usan DRAFT y las capturas completas sin contexto válido NEEDS_CONTEXT, ambos sin transporte activo. Conectividad sólo modifica información de red en UI; no genera SYNCED.

La idempotencia requiere transacción/índice en servidor; no basta con un UUID en Android. Prueba obligatoria E2: caída después del commit remoto y antes del ACK. La unicidad de negocio persona/evento se aplica además, y no reemplaza idempotencia.

## Dependencias candidatas

| Necesidad | Selección propuesta |
|---|---|
| UI/estado/navegación | Kotlin, Compose, Material 3, Navigation Compose, ViewModel y StateFlow |
| Datos locales | Room y SQLCipher `sqlcipher-android`, verificando integración exacta |
| Red | Retrofit + OkHttp + un único convertidor JSON |
| Segundo plano | WorkManager |
| Ubicación | FusedLocationProviderClient detrás de interfaz; disponibilidad de servicios comprobada; estado no disponible como fallback |
| Cámara | CameraX + ML Kit Barcode Scanning con modelo incluido en app |
| Conectividad E1 | Observador de estado de red de Android detrás de interfaz; no equivale a salud de API |
| Backend | Kotlin, Spring Boot, Spring Security, validación, persistencia PostgreSQL y migraciones Flyway |
| Contrato | OpenAPI como fuente ejecutable; ejemplos sintéticos y tests de contrato |
| Calidad | Android Lint, pruebas de Kotlin/Room/UI/backend y un formateador único; propuesto ktlint |

El esqueleto fija versiones de Android/Compose/Gradle verificadas por build/JVM en ANDROID_BASELINE; Room 2.8.5, SQLCipher 4.19.1, SQLite 2.7.0 y KSP 2.3.10 resuelven y compilan; la suite de persistencia/contexto pasó en Android 14/API 34. Spring/JDK y dependencias remotas se fijan al iniciar E2. Retrofit/WorkManager no se agregan a E1 sin funcionalidad que los necesite. No utilizar `latest`, rangos dinámicos, alpha o betas por conveniencia. No cambiar la matriz generada sin justificación y prueba.

SQLCipher cubre la base; Keystore protege claves criptográficas, no sustituye por sí solo la base cifrada. El modelo de escaneo incluido evita depender de una descarga en la primera jornada. Ninguna biblioteca conoce automáticamente el significado del QR de una cédula específica.

Fuentes consultadas: [offline-first y WorkManager](https://developer.android.com/topic/architecture/data-layer/offline-first), [integración SQLCipher](https://www.zetetic.net/sqlcipher/sqlcipher-for-android-migration/), [ML Kit Android](https://developers.google.com/ml-kit/vision/barcode-scanning/android), [corrutinas](https://developer.android.com/kotlin/coroutines/coroutines-best-practices).
