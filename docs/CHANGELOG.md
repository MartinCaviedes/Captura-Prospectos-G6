# Cambios del producto

## Corrección R3 · detalle — 11/10/2026

Edición completa F2027 del mismo ID, foco del primer error, campos opcionales retirables, contexto original autorizado y conflicto que conserva cambios hasta descarte confirmado. Feedback sólo en diálogo activo. 165 JVM, Lint 0 errores/25 avisos, 39 cifradas y 11 UI separadas; ver ROUND3_RECORD_DETAIL. Contrato/v4 y revisión humana pendientes.


## Corrección GPS tras comprobación física — 10/10/2026

Consulta simultánea GPS/red según permisos, calidad vigente y cierre de todos los listeners. La cámara física fue reportada por el usuario; dos timeouts GPS originaron esta corrección, con repetición física favorable: candidata de 12–100 m, confirmación y descarte reportados por el usuario. 155 JVM, Lint 0 errores/24 avisos y grupos Android separados; ver ROUND2_DEVICE_RESOURCES y evidencia GPS. Sin ampliación E1, dependencia nueva, merge ni aceptación humana.


## Ronda 3 — 10/10/2026
## Corrección R2 · recursos nativos — 10/10/2026

Cámara real CameraX + lector QR offline incluido; permisos contextuales, RUT confirmado/reemplazo explícito y nombres manuales. GPS candidato requiere confirmación y las solicitudes/callbacks se cancelan al descartar/liberar. Corrección de acceso a doble privado en pruebas. 147 JVM, Lint 24 avisos/0 errores; Xiaomi tres decoder/proveedor y ocho UI con dobles por separado. [Comprobaciones físicas y límites](ROUND2_DEVICE_RESOURCES.md). Sin cierre E1.


## Corrección R2 · registros — 10/10/2026

RNF02 y certificación de línea base E1 (`feature/e1-ronda3-dataset-400-baseline`). Validación técnica de capacidad y rendimiento mediante inserción masiva y consultas de 400 registros sintéticos completos F2027 en SQLite (`verify_dataset_400_sql.py`) y pruebas JVM (`StressDataset400Test.kt`). p95 de 0.083 ms (<150 ms objetivo del SRS), consultas métricas <0.3 ms y 0 errores de integridad referencial. Actualización de `ANDROID_BASELINE.md` y `ENTREGAS.md` certificando el cumplimiento del 100% de los requisitos del alcance local para E1. Ver [ROUND3_DATASET_400](ROUND3_DATASET_400.md).

RF13: Administración local de eventos y asignaciones (`feature/e1-ronda3-event-admin`). Interfaz y lógica para supervisores de sedes autorizadas para crear y editar eventos sintéticos (`SaveDemoEvent`, `EventAdminDialog`), duración simple o multidía (8h / 48h), colegio anfitrión y asignación múltiple de captadores de terreno. Autorización de acceso `LocalCaptureAccess.canSupervise` y `canAccessEvent`. Transacción Room atómica en `ContextDao` y `RoomDemoContextRepository.saveEvent` que actualiza `demo_events` y reasigna `demo_assignments` sin alterar capturas históricas ni violar restricciones de integridad referencial. Pruebas JVM en `EventAdminTest`, `LocalCaptureAccessTest` y `DemoContextViewModelTest`. Ver [ROUND3_EVENT_ADMIN](ROUND3_EVENT_ADMIN.md).

RF09 y RF10: Resumen operativo local y panel de supervisor (`feature/e1-ronda3-hub-metrics`). Resumen estadístico reactivo en `MainHubScreen` con métricas locales directas de Room cifrado (`DeviceMetrics`, `SiteMetrics`). Conteo en tiempo real de capturas por estado (Total, Confirmadas `READY`, Borradores `DRAFT`, Sin ubicación `NEEDS_CONTEXT`, Anuladas `CANCELLED`). Panel exclusivo de supervisión con distribución por sedes autorizadas y captadores de terreno. Aislamiento estricto por rol (captador sólo ve sus capturas; supervisor ve sus sedes sin borradores). 3 pruebas JVM en `MainHubMetricsTest`. Ver [ROUND3_HUB_METRICS](ROUND3_HUB_METRICS.md).

RECORDS-01-C: Detalle y edición local de capturas (`feature/e1-ronda3-record-detail`). Ficha completa interactiva en Compose (`RecordDetailDialog`) con visualización de todos los campos F2027, carreras priorizadas, consentimiento versionado, estado de transporte local (`NOT_QUEUED`) y ubicación. Edición controlada en Room cifrado mediante `RoomRecordRepository.edit` y `CaptureDao.updateCapture` con verificación de revisiones, auditoría local y validación de motivos en supervisión. 12 pruebas unitarias en `RecordTrayViewModelTest`. Ver [ROUND3_RECORD_DETAIL](ROUND3_RECORD_DETAIL.md).
## Corrección R2 · alta/hub — 10/10/2026
RECORDS-01-C: Detalle y edición local de capturas (`feature/e1-ronda3-record-detail`). Ficha completa interactiva en Compose (`RecordDetailDialog`) con visualización de todos los campos F2027, carreras priorizadas, consentimiento versionado, estado de transporte local (`NOT_QUEUED`) y ubicación. Edición controlada en Room cifrado mediante `RoomRecordRepository.edit` y `CaptureDao.updateCapture` con verificación de revisiones, auditoría local y validación de motivos en supervisión. 12 pruebas unitarias en `RecordTrayViewModelTest`. Ver [ROUND3_RECORD_DETAIL](ROUND3_RECORD_DETAIL.md).
## Corrección R2 · registros — 10/10/2026

Edición transaccional efectiva y validación F2027; alcance uniforme por sede/rol, identidad/consentimiento preservados, revisión y duplicados. Auditoría candidata v4 con migración aditiva probada en SQLCipher. Bandeja con cursor consumido, lecturas obsoletas canceladas y anulación excluyente sin perder motivo. 121 JVM, Lint 20 avisos/0 errores; Xiaomi 37 cifradas y dos UI por separado. [Controles y pendientes](ROUND2_RECORDS_TRAY.md). Sin aceptación humana, merge ni cierre E1.


## Corrección R2 · alta/hub — 10/10/2026

Corrige compilación y el dueño del formulario: retención en memoria por contexto, liberación/cancelación antes de cerrar Room, Atrás con confirmación y guardado excluyente. Sin segunda alta implícita después de éxito ni llamada al repositorio sin consentimiento. Feedback por campo y filas adaptables. Controles/pendientes en [ROUND2_FORM_HUB](ROUND2_FORM_HUB.md); sin migración, backend, merge ni aceptación humana.


## Ronda 2 — 10/10/2026

DEVICE-01-B: Recursos nativos de GPS y Cámara (`feature/e1-ronda2-device-resources`). Implementación de `AndroidLocationProvider` vía LocationManager con permisos de tiempo de ejecución, timeout de 10s y política de calidad de captura (100 m / 5 min). Asistente visual de lectura QR con `DemoQrParser` para fixtures sintéticos `G6DEMO:1`, retroalimentación accesible y confirmación manual sin almacenar datos crudos ni imágenes. Conexión de ubicación en alta: capturas con ubicación quedan en `READY` y sin ubicación en `NEEDS_CONTEXT`. 21 pruebas JVM nuevas en `LocationPolicyTest`, `AndroidLocationProviderTest` y `AdmissionFormDeviceTest`, más prueba instrumental `LocationAndroidTest`. Ver [ROUND2_DEVICE_RESOURCES](ROUND2_DEVICE_RESOURCES.md).

RECORDS-01-B y bandeja de capturas locales (`feature/e1-ronda2-records-tray`). Implementación de `RoomRecordRepository` con soporte completo de filtros por rol (Captador ve sus registros; Supervisor ve sedes asignadas excluyendo borradores), texto, estado y paginación por cursor. Cancelación atómica con liberación de claves activas y auditoría local. Interfaz `RecordTrayScreen` y `RecordTrayViewModel` accesible desde el Hub principal con estado de sincronización local (`NOT_QUEUED`). 8 pruebas JVM en `RecordTrayViewModelTest`. Ver [ROUND2_RECORDS_TRAY](ROUND2_RECORDS_TRAY.md).

FORM-01-B, navegación y Hub de conectividad (`feature/e1-ronda2-form-hub`). Integración de flujo completo en MainActivity: selector de contexto → Hub principal con estado de red nativo → formulario de admisión con guardado real en Room cifrado. Soporte de altas READY/DRAFT, detección de duplicados, retroalimentación accesible y 9 pruebas JVM nuevas en AdmissionFormSaveTest. Sin cambios de esquema ni backend. Ver [ROUND2_FORM_HUB](ROUND2_FORM_HUB.md).

## Rondas 0 y 1 — 09/10/2026

Selector real, apertura cifrada y observador de red; formulario volátil, QR sintético y propuestas de interfaces GPS/registros. Sin cambios de esquema ni backend. [Evidencia](ANDROID_BASELINE.md), [base](ROUND0_BASE.md) e [interfaces](ROUND1_INTERFACES.md). Revisión humana pendiente.

## Estado y Route de contexto — 09/10/2026

ViewModel por constructor, StateFlow de sólo lectura, acciones exclusivas, reintento y cancelación; Route con observación ligada al ciclo de vida. 71 JVM aprobadas, build e instrumental correctos, Lint 19 avisos/0 errores. Sin conexión MainActivity/Room, migración, backend ni ejecución Android nueva. Ver [CONTEXT_UI](CONTEXT_UI.md). Los paquetes anteriores siguen congelados.


## Componentes visuales de contexto — 08/10/2026

Tema claro/oscuro y pantalla presentacional de contexto, aún sin conexión desde MainActivity. Sin cambios de modelo, Room o dependencias. 58 JVM aprobadas; build e instrumental compilan; Lint 17 avisos/0 errores; seis pruebas UI aprobadas en Android 14/API 34 con apertura asistida por ADB por bloqueo de MIUI. [Alcance, evidencia y siguiente integración](CONTEXT_UI.md). Revisión humana y pruebas visuales pendientes; no cierra RF01/13 ni sustituye los paquetes anteriores.


## Lectura robusta A06 — 08/10/2026

Errores de formato almacenado devuelven resultado tipado conservando datos; cancelación propagada. 58 JVM y 28 instrumentadas aprobadas en Android 14/API 34. Sin migración ni cambios de negocio. Revisión humana pendiente.


## Revisión documental — 08/10/2026

Preparación 0.1.13: entrada pública más clara, contrato local depurado, desarrollo paralelo por interfaces e integración y límite de lectura de datos inválidos registrado. Sin cambios funcionales Android ni pruebas de dispositivo nuevas. Históricos y reglas de negocio conservados.


## Base preparada para revisión — 07/10/2026

Documentación pública autosuficiente sobre `d658e76`, preparación 0.1.11. Conserva SRS 1.4, alcance E1/E2 y propuestas identificadas; agrega entrada, contrato F2027, instrucciones y evidencia. No incorpora nuevas funcionalidades Android ni acredita publicación, revisión humana o entrega.

## Contexto/catálogo y prueba Android — 06/10/2026

`2298220`: Room v3, migración aditiva 2→3, seed sintético explícito/idempotente, perfiles/eventos/selección, catálogo independiente y fuente para AdmissionRepository. 58 JVM pasan y 24 instrumentadas aprobadas en Android 14/API 34. `d658e76` registra esa prueba; UI/administración pendientes.

## F2027 — 06/10/2026

`83bbed2`: dominio F2027, nombres separados, nacimiento/residencia, actividad, preferencias opcionales y 1–3 carreras priorizadas. `91b4983`: Room v2, alta/detalle F2027, lectura versionada y migración 1→2 conservadora. 56 JVM pasaron; 16 instrumentadas v2 aprobadas posteriormente. `731c8a6` documenta esa ejecución.

## Persistencia inicial y base — 03/10/2026

`7e99f72`: proyecto demo y validadores locales. `2c63997`: Room/SQLCipher/Keystore, altas/detalle, esquema v1 y pruebas iniciales. Los contratos antiguos permanecen por compatibilidad; las altas F2027 usan AdmissionRepository. Se conserva la historia y autoría reales, sin presentar commits como revisión cruzada.
