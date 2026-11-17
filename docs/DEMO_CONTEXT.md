# Contexto, catálogo y cuentas locales

Actualización 17/11/2026 (`10ba22d`):
- `DemoSeed` provee el catálogo base de Admisión Duoc UC 2027 (`OFFICIAL_REVIEWED`): 8 sedes reales (`Sede Viña del Mar`, `Sede Valparaíso`, `Sede Alameda`, `Sede Antonio Varas`, `Sede Maipú`, `Sede San Joaquín`, `Sede Concepción`, `Sede Puerto Montt`), 10 programas reales por escuela (incluyendo `Duoc Online` y `Administración en Turismo y Hospitalidad` con sus 4 menciones) y el texto de consentimiento de admisión.
- En el arranque normal de la aplicación se invoca `RoomDemoContextRepository.initializeCatalog()`, que instala únicamente el catálogo base y el texto de consentimiento, **sin precargar perfiles ni eventos ficticios**.
- Los usuarios crean su cuenta o inician sesión mediante `registerAccount(...)` y `authenticate(...)` (persistidos en `local_user_accounts` dentro de la misma base SQLite cifrada, además de sincronizar sesiones autenticadas con JWT desde el `api-gateway`).
- Tanto `CAPTOR` como `SUPERVISOR` pueden crear eventos en sus sedes autorizadas mediante `saveEvent(...)`.

Antecedente E1 del 06/10/2026, código `2298220`, Room v3.

## Contrato para el consumidor

| Operación | Comportamiento |
|---|---|
| `initializeCatalog()` | Inicializa de forma transaccional e idempotente únicamente el catálogo de sedes, carreras, menciones y versión de consentimiento (uso normal de la app). |
| `initialize()` | Inicialización completa incluyendo las filas de prueba para compatibilidad con los tests instrumentados existentes. |
| `registerAccount(command)` | Registra o actualiza una cuenta real con hash SHA-256 + salt en `local_user_accounts`, crea su perfil y sedes en `demo_profiles` / `demo_profile_sites` y la selecciona como actor activo. |
| `authenticate(emailOrUsername, password)` | Valida las credenciales contra `local_user_accounts` y selecciona el perfil correspondiente en `demo_context`. |
| `snapshot` / `observe` | `NotInitialized`, `Ready` o `StorageUnavailable`; `Ready` contiene catálogo, perfiles registrados, selección actual, eventos autorizados y texto de consentimiento. |
| `selectProfile(id)` | Exige perfil existente, limpia evento; no modifica capturas ni selecciona preferencias del prospecto. |
| `selectEvent(id)` | Exige perfil seleccionado, asignación y ámbito; rechaza evento desconocido/ajeno conservando selección válida. |
| `saveEvent(command)` | Permite a supervisores y captadores crear o editar eventos en sus sedes autorizadas (autoasignando al captador creador). |
| `clearSelection` | Limpia perfil/evento únicamente; conserva `deviceId`, catálogo y capturas. |
| `currentActor` | Actor derivado del perfil persistido, no de campos libres del formulario. |
| `environment(eventId)` | Devuelve `AdmissionEnvironment` sólo para evento actualmente seleccionado y autorizado; incluye programas/sedes independientes y consentimiento. |

## Comprobaciones y límites

58 JVM pasan (56 anteriores y dos de coherencia de fixture); build demoDebug y APK instrumental compilados, Lint 17 avisos/0 errores. Ocho pruebas DemoContextTest ejecutadas con éxito: inicialización concurrente/idempotencia, reapertura, permisos/cambio de perfil, guardado con contexto persistido y cierre sin pérdida, consentimiento conflictivo, rollback/reintento, observación y migración cifrada v2→v3 conservando captura.

`python tools/verify_context_migration_sql.py` pasó en SQLite del host: cadena 1→2→3, preservación de filas v2, esquema/FK/índices, ausencia de seed automático y rollback. No sustituye Android. La suite instrumental actual de 24 pruebas pasó sobre 2298220/Room v3 en 23.178 s. Las 16 de 91b4983/Room v2 permanecen como antecedente.

Siguiente: revisar diff/evidencia y continuar BASE-03/FORM-01 con selección explícita → consentimiento → formulario → guardado → detalle. Sin cambios de negocio F2027, dependencias nuevas, UI conectada, administración de eventos ni RF completo. Evidencia en [ANDROID_BASELINE](ANDROID_BASELINE.md).
