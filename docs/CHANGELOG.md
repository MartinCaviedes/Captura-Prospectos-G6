# Cambios del producto

## 1.0.1 · acción de eventos adaptable · 19/10/2026

Se corrige el botón «Nuevo evento» que quedaba en una columna de letras en vertical. Se distribuye 1.0.1 (2), misma firma e identificador, sin cambios de negocio/datos/permisos/dependencias. 191 JVM y siete Android dirigidas aprobadas, Lint 30 avisos/0 errores; base demo conservada durante pruebas. [Método, alcance y comprobación física](UI_EVENTOS_RESPONSIVE.md). La evidencia previa conserva su APK/fecha; no cierre E1.

11/10/2026 · Candidato visual E1 sobre 161f635: bienvenida, credenciales demo explícitas, menú compacto/ampliado, perfil, carga voluntaria de 24 ficticios y gráficos con métricas reales. Google/Microsoft se anuncian para el futuro sin conexión. Mejora contraste, lectura del consentimiento y autocorrección de correo/teléfono. Sin migraciones ni dependencias nuevas. 187 JVM, 5 + 22 Android separados y Lint 25 avisos/0 errores; usuario confirma acceso/formulario y giro en Xiaomi sin recortes ni pérdida de texto. [Guía y pendientes](UI_REFINEMENT_E1.md). Sin integración ni cierre humano de rondas.

## Comprobación acumulada y dataset real · 11/10/2026

187 JVM, cero fallos/errores/omitidas; ambas APK y Lint 0 errores/25 avisos. Xiaomi 11T Android 14/API 34: **51 de datos/nativos (93.926 s)** y **22 UI (180.681 s)** en grupos separados; apertura asistida MIUI en UI. Mismas APK verificadas por SHA256, identificadas con fuentes y runners en [evidencia](evidencias/dataset-correction-2026-10-11.json). Las 51 incluyen 48 cifradas (el ensayo nuevo de 400 y 47 regresiones) y tres decoder/proveedor. Las 22 reúnen selector/contexto, formulario/dispositivo, bandeja/detalle, métricas y administración. No se suman ejecuciones históricas como si fueran una suite actual.

Dataset400StorageTest creó 400 admisiones mediante AdmissionRepository y anuló 40 mediante RecordRepository, sobre contexto sintético seleccionado y SQLCipher/Room v4 reales. Estados finales: READY 260, NEEDS_CONTEXT 60, DRAFT 40, CANCELLED 40; 600 intereses, 320 claves activas y 440 auditorías. Reapertura conserva filas/claves/auditoría; encabezado cifrado e integridad FK comprobados. Cada captador recupera 200 filas mediante cursor real sin repetición/omisión; supervisor 360, sin borradores ajenos. Por evento: 160 inscripciones activas, 160 personas y conteos de programas 160/80; consulta fuera de alcance denegada.

Latencia de llamada completa al repositorio (incluye validación, contexto, dispatcher y transacción; excluye inicialización, selección de contexto y GPS/red), elapsedRealtimeNanos y percentiles nearest-rank: altas n=400 p95 **114.626 ms**, anulaciones n=40 p95 **100.073 ms**, conjunto n=440 p95 **113.457 ms**. Altas p99 153.829 ms y máximo 195.897 ms. Meta PT p95 <150 ms observada en este ensayo debug; no significa que toda llamada dure menos de 150 ms. Sin perfilado de frames, no acredita ausencia de jank ni fugas de memoria.

StressDataset400Test comprueba distribución y aislamiento con LocalCaptureAccess, incluyendo realmente 50 borradores ajenos en su fixture específico de 350 visibles. Sus lotes en memoria no prueban un cursor ni rendimiento. verify_dataset_400_sql.py usa esquema v4, UUID/RUT/enums coherentes y verifica estructura/FK/conteos/cursor del host; no mide ni certifica Android.


## Corrección y comprobación de administración · 11/10/2026

185 JVM, ambas APK, Lint 0 errores/25 avisos. Xiaomi 11T Android 14/API 34: 47 cifradas (60.545 s) y 11 UI (76.858 s), en grupos separados y con apertura asistida MIUI en UI. Cuatro nuevas de almacenamiento verifican autorización de origen/destino/captadores, preservación histórica, rollback y revocación de asignación. Dos nuevas UI prueban edición desde el hub, fechas multidía explícitas, metadatos, validación y confirmación de descarte. Cuatro nuevas JVM prueban preparación y dos estado/bloqueo de guardado; se conservan las pruebas originales. [Evidencia y APK](evidencias/events-correction-2026-10-11.json).

Se corrigieron una expectativa histórica que omitía la selección tras guardar y un selector UI ambiguo entre la sede del hub y el diálogo. El grupo UI final completo pasó; sin pruebas omitidas ni reducción de controles.


## Corrección y verificación de métricas · 11/10/2026

**172 JVM**, ambas APK y Lint **0 errores/25 avisos**. Incluye cuatro regresiones nuevas de fechas/zona/DST, rango inválido que cancela una lectura, error con último cálculo y cancelación por dueño. Xiaomi 11T Android 14/API 34: **43 cifradas (55.885 s)** y **9 UI (54.032 s)** en grupos separados; asistencia MIUI en UI. Cuatro nuevas de Room verifican conteos/rango/eventos/intereses, alcance de captador/supervisor, almacenamiento corrupto y cancelación. Dos UI nuevas usan Room cifrado real para supervisor sin alta y actualización tras anulación/rango inválido; seis del selector y una de contexto+Room son regresiones. [Fuentes/APK/runners](evidencias/metrics-correction-2026-10-11.json).

El primer control cifrado rechazó un RUT ficticio con DV incorrecto en la preparación; se corrigió el fixture y el grupo final completo pasó. Lint detectó uso de ViewModelStore.put en código de producción; se sustituyó por ViewModelProvider/fábrica pública, conservando el control. Sin tests omitidos ni baseline nueva.


## Corrección R3 · detalle — 11/10/2026

Edición completa F2027 del mismo ID, foco del primer error, campos opcionales retirables, contexto original autorizado y conflicto que conserva cambios hasta descarte confirmado. Feedback sólo en diálogo activo. 165 JVM, Lint 0 errores/25 avisos, 39 cifradas y 11 UI separadas; ver ROUND3_RECORD_DETAIL. Contrato/v4 y revisión humana pendientes.


## Corrección GPS tras comprobación física — 10/10/2026

Consulta simultánea GPS/red según permisos, calidad vigente y cierre de todos los listeners. La cámara física fue reportada por el usuario; dos timeouts GPS originaron esta corrección, con repetición física favorable: candidata de 12–100 m, confirmación y descarte reportados por el usuario. 155 JVM, Lint 0 errores/24 avisos y grupos Android separados; ver ROUND2_DEVICE_RESOURCES y evidencia GPS. Sin ampliación E1, dependencia nueva, merge ni aceptación humana.


## Ronda 3 — 10/10/2026
## Corrección R2 · recursos nativos — 10/10/2026

Cámara real CameraX + lector QR offline incluido; permisos contextuales, RUT confirmado/reemplazo explícito y nombres manuales. GPS candidato requiere confirmación y las solicitudes/callbacks se cancelan al descartar/liberar. Corrección de acceso a doble privado en pruebas. 147 JVM, Lint 24 avisos/0 errores; Xiaomi tres decoder/proveedor y ocho UI con dobles por separado. [Comprobaciones físicas y límites](ROUND2_DEVICE_RESOURCES.md). Sin cierre E1.


## Corrección R2 · registros — 10/10/2026

Incremento original de dataset 400: añadió control host SQLite y fixture JVM. Su afirmación de certificación total de E1 y extrapolación del p95 host a Android se retiraron en la corrección del 11/10. El fixture no incluía los borradores ajenos esperados. Ver [método y evidencia corregidos](ROUND3_DATASET_400.md); el historial Git conserva el texto anterior.

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
