# Cambios del producto

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
