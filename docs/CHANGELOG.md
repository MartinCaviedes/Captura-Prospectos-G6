# Cambios del producto

## Ronda 3 â€” 10/10/2026
## Corrección R2 · recursos nativos — 10/10/2026

Cámara real CameraX + lector QR offline incluido; permisos contextuales, RUT confirmado/reemplazo explícito y nombres manuales. GPS candidato requiere confirmación y las solicitudes/callbacks se cancelan al descartar/liberar. Corrección de acceso a doble privado en pruebas. 147 JVM, Lint 24 avisos/0 errores; Xiaomi tres decoder/proveedor y ocho UI con dobles por separado. [Comprobaciones físicas y límites](ROUND2_DEVICE_RESOURCES.md). Sin cierre E1.


## Corrección R2 · registros — 10/10/2026

RF13: Administración local de eventos y asignaciones (`feature/e1-ronda3-event-admin`). Interfaz y lógica para supervisores de sedes autorizadas para crear y editar eventos sintéticos (`SaveDemoEvent`, `EventAdminDialog`), duración simple o multidía (8h / 48h), colegio anfitrión y asignación múltiple de captadores de terreno. Autorización de acceso `LocalCaptureAccess.canSupervise` y `canAccessEvent`. Transacción Room atómica en `ContextDao` y `RoomDemoContextRepository.saveEvent` que actualiza `demo_events` y reasigna `demo_assignments` sin alterar capturas históricas ni violar restricciones de integridad referencial. Pruebas JVM en `EventAdminTest`, `LocalCaptureAccessTest` y `DemoContextViewModelTest`. Ver [ROUND3_EVENT_ADMIN](ROUND3_EVENT_ADMIN.md).

RF09 y RF10: Resumen operativo local y panel de supervisor (`feature/e1-ronda3-hub-metrics`). Resumen estadístico reactivo en `MainHubScreen` con métricas locales directas de Room cifrado (`DeviceMetrics`, `SiteMetrics`). Conteo en tiempo real de capturas por estado (Total, Confirmadas `READY`, Borradores `DRAFT`, Sin ubicación `NEEDS_CONTEXT`, Anuladas `CANCELLED`). Panel exclusivo de supervisión con distribución por sedes autorizadas y captadores de terreno. Aislamiento estricto por rol (captador sólo ve sus capturas; supervisor ve sus sedes sin borradores). 3 pruebas JVM en `MainHubMetricsTest`. Ver [ROUND3_HUB_METRICS](ROUND3_HUB_METRICS.md).

RECORDS-01-C: Detalle y edición local de capturas (`feature/e1-ronda3-record-detail`). Ficha completa interactiva en Compose (`RecordDetailDialog`) con visualización de todos los campos F2027, carreras priorizadas, consentimiento versionado, estado de transporte local (`NOT_QUEUED`) y ubicación. Edición controlada en Room cifrado mediante `RoomRecordRepository.edit` y `CaptureDao.updateCapture` con verificación de revisiones, auditoría local y validación de motivos en supervisión. 12 pruebas unitarias en `RecordTrayViewModelTest`. Ver [ROUND3_RECORD_DETAIL](ROUND3_RECORD_DETAIL.md).
## Corrección R2 · alta/hub — 10/10/2026
RECORDS-01-C: Detalle y ediciÃ³n local de capturas (`feature/e1-ronda3-record-detail`). Ficha completa interactiva en Compose (`RecordDetailDialog`) con visualizaciÃ³n de todos los campos F2027, carreras priorizadas, consentimiento versionado, estado de transporte local (`NOT_QUEUED`) y ubicaciÃ³n. EdiciÃ³n controlada en Room cifrado mediante `RoomRecordRepository.edit` y `CaptureDao.updateCapture` con verificaciÃ³n de revisiones, auditorÃ­a local y validaciÃ³n de motivos en supervisiÃ³n. 12 pruebas unitarias en `RecordTrayViewModelTest`. Ver [ROUND3_RECORD_DETAIL](ROUND3_RECORD_DETAIL.md).
## CorrecciÃ³n R2 Â· registros â€” 10/10/2026

EdiciÃ³n transaccional efectiva y validaciÃ³n F2027; alcance uniforme por sede/rol, identidad/consentimiento preservados, revisiÃ³n y duplicados. AuditorÃ­a candidata v4 con migraciÃ³n aditiva probada en SQLCipher. Bandeja con cursor consumido, lecturas obsoletas canceladas y anulaciÃ³n excluyente sin perder motivo. 121 JVM, Lint 20 avisos/0 errores; Xiaomi 37 cifradas y dos UI por separado. [Controles y pendientes](ROUND2_RECORDS_TRAY.md). Sin aceptaciÃ³n humana, merge ni cierre E1.


## CorrecciÃ³n R2 Â· alta/hub â€” 10/10/2026

Corrige compilaciÃ³n y el dueÃ±o del formulario: retenciÃ³n en memoria por contexto, liberaciÃ³n/cancelaciÃ³n antes de cerrar Room, AtrÃ¡s con confirmaciÃ³n y guardado excluyente. Sin segunda alta implÃ­cita despuÃ©s de Ã©xito ni llamada al repositorio sin consentimiento. Feedback por campo y filas adaptables. Controles/pendientes en [ROUND2_FORM_HUB](ROUND2_FORM_HUB.md); sin migraciÃ³n, backend, merge ni aceptaciÃ³n humana.


## Ronda 2 â€” 10/10/2026

DEVICE-01-B: Recursos nativos de GPS y CÃ¡mara (`feature/e1-ronda2-device-resources`). ImplementaciÃ³n de `AndroidLocationProvider` vÃ­a LocationManager con permisos de tiempo de ejecuciÃ³n, timeout de 10s y polÃ­tica de calidad de captura (100 m / 5 min). Asistente visual de lectura QR con `DemoQrParser` para fixtures sintÃ©ticos `G6DEMO:1`, retroalimentaciÃ³n accesible y confirmaciÃ³n manual sin almacenar datos crudos ni imÃ¡genes. ConexiÃ³n de ubicaciÃ³n en alta: capturas con ubicaciÃ³n quedan en `READY` y sin ubicaciÃ³n en `NEEDS_CONTEXT`. 21 pruebas JVM nuevas en `LocationPolicyTest`, `AndroidLocationProviderTest` y `AdmissionFormDeviceTest`, mÃ¡s prueba instrumental `LocationAndroidTest`. Ver [ROUND2_DEVICE_RESOURCES](ROUND2_DEVICE_RESOURCES.md).

RECORDS-01-B y bandeja de capturas locales (`feature/e1-ronda2-records-tray`). ImplementaciÃ³n de `RoomRecordRepository` con soporte completo de filtros por rol (Captador ve sus registros; Supervisor ve sedes asignadas excluyendo borradores), texto, estado y paginaciÃ³n por cursor. CancelaciÃ³n atÃ³mica con liberaciÃ³n de claves activas y auditorÃ­a local. Interfaz `RecordTrayScreen` y `RecordTrayViewModel` accesible desde el Hub principal con estado de sincronizaciÃ³n local (`NOT_QUEUED`). 8 pruebas JVM en `RecordTrayViewModelTest`. Ver [ROUND2_RECORDS_TRAY](ROUND2_RECORDS_TRAY.md).

FORM-01-B, navegaciÃ³n y Hub de conectividad (`feature/e1-ronda2-form-hub`). IntegraciÃ³n de flujo completo en MainActivity: selector de contexto â†’ Hub principal con estado de red nativo â†’ formulario de admisiÃ³n con guardado real en Room cifrado. Soporte de altas READY/DRAFT, detecciÃ³n de duplicados, retroalimentaciÃ³n accesible y 9 pruebas JVM nuevas en AdmissionFormSaveTest. Sin cambios de esquema ni backend. Ver [ROUND2_FORM_HUB](ROUND2_FORM_HUB.md).

## Rondas 0 y 1 â€” 09/10/2026

Selector real, apertura cifrada y observador de red; formulario volÃ¡til, QR sintÃ©tico y propuestas de interfaces GPS/registros. Sin cambios de esquema ni backend. [Evidencia](ANDROID_BASELINE.md), [base](ROUND0_BASE.md) e [interfaces](ROUND1_INTERFACES.md). RevisiÃ³n humana pendiente.

## Estado y Route de contexto â€” 09/10/2026

ViewModel por constructor, StateFlow de sÃ³lo lectura, acciones exclusivas, reintento y cancelaciÃ³n; Route con observaciÃ³n ligada al ciclo de vida. 71 JVM aprobadas, build e instrumental correctos, Lint 19 avisos/0 errores. Sin conexiÃ³n MainActivity/Room, migraciÃ³n, backend ni ejecuciÃ³n Android nueva. Ver [CONTEXT_UI](CONTEXT_UI.md). Los paquetes anteriores siguen congelados.


## Componentes visuales de contexto â€” 08/10/2026

Tema claro/oscuro y pantalla presentacional de contexto, aÃºn sin conexiÃ³n desde MainActivity. Sin cambios de modelo, Room o dependencias. 58 JVM aprobadas; build e instrumental compilan; Lint 17 avisos/0 errores; seis pruebas UI aprobadas en Android 14/API 34 con apertura asistida por ADB por bloqueo de MIUI. [Alcance, evidencia y siguiente integraciÃ³n](CONTEXT_UI.md). RevisiÃ³n humana y pruebas visuales pendientes; no cierra RF01/13 ni sustituye los paquetes anteriores.


## Lectura robusta A06 â€” 08/10/2026

Errores de formato almacenado devuelven resultado tipado conservando datos; cancelaciÃ³n propagada. 58 JVM y 28 instrumentadas aprobadas en Android 14/API 34. Sin migraciÃ³n ni cambios de negocio. RevisiÃ³n humana pendiente.


## RevisiÃ³n documental â€” 08/10/2026

PreparaciÃ³n 0.1.13: entrada pÃºblica mÃ¡s clara, contrato local depurado, desarrollo paralelo por interfaces e integraciÃ³n y lÃ­mite de lectura de datos invÃ¡lidos registrado. Sin cambios funcionales Android ni pruebas de dispositivo nuevas. HistÃ³ricos y reglas de negocio conservados.


## Base preparada para revisiÃ³n â€” 07/10/2026

DocumentaciÃ³n pÃºblica autosuficiente sobre `d658e76`, preparaciÃ³n 0.1.11. Conserva SRS 1.4, alcance E1/E2 y propuestas identificadas; agrega entrada, contrato F2027, instrucciones y evidencia. No incorpora nuevas funcionalidades Android ni acredita publicaciÃ³n, revisiÃ³n humana o entrega.

## Contexto/catÃ¡logo y prueba Android â€” 06/10/2026

`2298220`: Room v3, migraciÃ³n aditiva 2â†’3, seed sintÃ©tico explÃ­cito/idempotente, perfiles/eventos/selecciÃ³n, catÃ¡logo independiente y fuente para AdmissionRepository. 58 JVM pasan y 24 instrumentadas aprobadas en Android 14/API 34. `d658e76` registra esa prueba; UI/administraciÃ³n pendientes.

## F2027 â€” 06/10/2026

`83bbed2`: dominio F2027, nombres separados, nacimiento/residencia, actividad, preferencias opcionales y 1â€“3 carreras priorizadas. `91b4983`: Room v2, alta/detalle F2027, lectura versionada y migraciÃ³n 1â†’2 conservadora. 56 JVM pasaron; 16 instrumentadas v2 aprobadas posteriormente. `731c8a6` documenta esa ejecuciÃ³n.

## Persistencia inicial y base â€” 03/10/2026

`7e99f72`: proyecto demo y validadores locales. `2c63997`: Room/SQLCipher/Keystore, altas/detalle, esquema v1 y pruebas iniciales. Los contratos antiguos permanecen por compatibilidad; las altas F2027 usan AdmissionRepository. Se conserva la historia y autorÃ­a reales, sin presentar commits como revisiÃ³n cruzada.
