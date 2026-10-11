# Evidencia de la base Android

## Corrección y comprobación de administración · 11/10/2026

185 JVM, ambas APK, Lint 0 errores/25 avisos. Xiaomi 11T Android 14/API 34: 47 cifradas (60.545 s) y 11 UI (76.858 s), en grupos separados y con apertura asistida MIUI en UI. Cuatro nuevas de almacenamiento verifican autorización de origen/destino/captadores, preservación histórica, rollback y revocación de asignación. Dos nuevas UI prueban edición desde el hub, fechas multidía explícitas, metadatos, validación y confirmación de descarte. Cuatro nuevas JVM prueban preparación y dos estado/bloqueo de guardado; se conservan las pruebas originales. [Evidencia y APK](evidencias/events-correction-2026-10-11.json).

Se corrigieron una expectativa histórica que omitía la selección tras guardar y un selector UI ambiguo entre la sede del hub y el diálogo. El grupo UI final completo pasó; sin pruebas omitidas ni reducción de controles.


## Corrección y verificación de métricas · 11/10/2026

**172 JVM**, ambas APK y Lint **0 errores/25 avisos**. Incluye cuatro regresiones nuevas de fechas/zona/DST, rango inválido que cancela una lectura, error con último cálculo y cancelación por dueño. Xiaomi 11T Android 14/API 34: **43 cifradas (55.885 s)** y **9 UI (54.032 s)** en grupos separados; asistencia MIUI en UI. Cuatro nuevas de Room verifican conteos/rango/eventos/intereses, alcance de captador/supervisor, almacenamiento corrupto y cancelación. Dos UI nuevas usan Room cifrado real para supervisor sin alta y actualización tras anulación/rango inválido; seis del selector y una de contexto+Room son regresiones. [Fuentes/APK/runners](evidencias/metrics-correction-2026-10-11.json).

El primer control cifrado rechazó un RUT ficticio con DV incorrecto en la preparación; se corrigió el fixture y el grupo final completo pasó. Lint detectó uso de ViewModelStore.put en código de producción; se sustituyó por ViewModelProvider/fábrica pública, conservando el control. Sin tests omitidos ni baseline nueva.


## Verificación nueva · 11/10/2026

**165 JVM**, ambas APK y Lint **0 errores/25 avisos**. Incluye las doce pruebas originales de bandeja, seis regresiones nuevas de edición y la corrección GPS propagada. Xiaomi 11T Android 14/API 34: **39 cifradas (49.303 s)** sobre ef940a4, concluidas durante el corte; **11 UI (107.106 s)** sobre el build final, en un grupo separado con asistencia MIUI. Tres UI nuevas usan Room cifrado real: completar borrador y confirmar el mismo registro, error/foco/reintento con retirada de notas opcionales, conflicto con descarte explícito. Ocho regresiones de formulario, recursos y bandeja también aprobadas. Fuentes/APK identificadas en [evidencia](evidencias/detail-correction-2026-10-11.json).

La primera ejecución después de la pausa encontró APK instaladas diferentes y clases ausentes; se reinstalaron los builds propios con -r -t y SHA256 verificado. No se toma aquel intento como fallo de producto. Los intentos válidos posteriores detectaron un selector incorrecto del RUT deshabilitado y feedback repetido: corregidos antes de la suite final. Se conservan las pruebas originales; no se omiten aserciones de campos, persistencia o revisión.

Giro real de MainActivity, TalkBack/visual completa, aceptación humana de contrato/v4 y ensayo P3 pendientes. La cámara/GPS físicos fueron comprobados por el usuario en el incremento nativo; no se extrapola ese resultado a todo el recorrido E1. Sin merge ni cierre de RF.


## Corrección de adquisición tras prueba física · 10/10/2026

El usuario comprobó cámara física con el QR ficticio: se detectan fullName/RUT y se aplica sólo RUT, sin dividir nombres. Dos intentos de GPS agotaron 10 s con permiso preciso y ubicación activada (uno cerca de ventana). Estos intentos anteriores no acreditaban adquisición ni confirmación.

Se corrige la selección exclusiva de GPS: con permiso preciso se suscriben GPS y red; con aproximado, sólo red. La primera muestra que cumple 100 m/5 min gana. Una muestra imprecisa/antigua no impide esperar al otro proveedor; al agotar plazo se informa calidad si hubo una muestra rechazada, o timeout si no hubo ninguna. No se usa última ubicación cacheada, no se cambian los umbrales ni se confirma automáticamente. Cada listener se retira al terminar, cancelar o revocarse permiso, incluso ante callback síncrono de registro. Sin dependencia nueva ni INTERNET.

Controles nuevos: **155 JVM**, Lint **0 errores/24 avisos**, ambas APK compiladas. Ocho casos JVM específicos de adquisición, cierre y permiso revocado. Xiaomi: **3 decoder/proveedor (1.214 s)** y **8 UI (68.764 s)**, separados con asistencia de apertura MIUI en UI; dobles/bitmaps, sin simular hardware aprobado. Fuentes y APK instaladas verificadas por SHA256 en [evidencia GPS](evidencias/gps-correction-2026-10-10.json). La caché de proyecto fue aislada por worktree y se deshabilitó configuration cache para evitar reutilizar rutas del worktree de detalle. Se corrigió el hallazgo MissingPermission de Lint manejando SecurityException explícitamente; no se añadió baseline ni se deshabilitó el control.

En la APK corregida, el usuario reportó una ubicación candidata con precisión de 12–100 m y comprobó las acciones de confirmar y descartar. No se registraron coordenadas reales ni se atribuye la muestra a un proveedor específico. [Proveedores Android](https://developer.android.com/reference/android/location/LocationManager), [permisos](https://developer.android.com/develop/sensors-and-location/location/permissions). No cierra RF ni revisión humana.


## Corrección R2 · recursos nativos — 10/10/2026

Rama `feature/e1-ronda2-device-resources`, sobre 925810f con las correcciones previas de alta/hub y registros. **147 JVM aprobadas**, cero fallos/errores/omitidas; ambas APK compiladas; **Lint 24 avisos/0 errores**. Los cuatro avisos adicionales corresponden a dependencias/UseKtx; no se modifican las versiones previas por estilo.

Nuevas ejecuciones en Xiaomi 11T Android 14/API 34: **tres nativas/decoder** (0.227 s) y **ocho UI** (68.595 s), en grupos separados. El lector ML Kit incluido decodifica bitmaps QR ficticios y el parser rechaza una URL reconocida, sin permiso INTERNET. El proveedor Android devuelve PermissionDenied con permisos de ubicación denegados. Eso no acredita adquisición GPS ni lectura física con cámara. La UI prueba confirmación de candidato de ubicación y reemplazo explícito de RUT, más seis regresiones de formulario/Atrás/bandeja, con dobles y apertura asistida ADB en MIUI. No se conceden/revocan permisos ni se borra la base del usuario.

Las APK instaladas coinciden con el build por SHA256. [Alcance y comprobación física pendiente](ROUND2_DEVICE_RESOURCES.md), [fuentes/APK/suites](evidencias/device-correction-2026-10-10.json), [runner nativo](evidencias/android-device-data-2026-10-10.txt), [runner UI](evidencias/android-device-ui-2026-10-10.txt). Se corrigió además una referencia original a un doble privado de otro archivo que impedía compilar las pruebas; se conservan los nueve casos originales y se ajustan las expectativas incompatibles con el contrato (confirmación GPS y nombres manuales), agregando cinco regresiones de cancelación/estado.

Quedan lectura física de QR ficticio/enfoque/giro/cierre de cámara, permiso aproximado/preciso, denegación permanente y ajustes, servicios GPS apagados, timeout/precisión y confirmación sobre ubicación real. TalkBack/visual completa, aceptación del contrato de registros y cierre E1 siguen pendientes. La evidencia de Room v4 pertenece al bloque previo y no se presenta como una nueva prueba nativa conjunta.


## Corrección R2 · registros — 10/10/2026

Rama `feature/e1-ronda2-records-tray`, sobre 08df94f e incorporando la corrección de alta/hub 0fc8b41. **121 JVM sin fallos/errores/omitidas**, APK principal/instrumental correctos y **Lint 20 avisos/0 errores**. Nuevas ejecuciones en Xiaomi 11T Android 14/API 34: **37 pruebas cifradas** (48.262 s), incluidas diez nuevas de registros/migración y 27 de regresión; **dos UI** (16.334 s), por separado y con apertura asistida ADB en MIUI. Se usan bases test-* y datos ficticios, sin borrar la base del usuario ni conceder permisos peligrosos. Ambas APK instaladas coinciden por SHA256 con el build.

La suite de datos verifica permisos por sede/rol, paginación real de 120 registros con timestamps empatados, normalización de búsqueda, edición válida/inválida, borrador→NEEDS_CONTEXT→READY, identidad y consentimiento inmutables, duplicados, revisión concurrente, motivo, rollback y migración cifrada v3→v4. La UI verifica carga de siguiente página y bloqueo de anulación/Atrás con conservación del motivo al fallar. Usa un doble: no acredita un recorrido UI+Room conjunto. Las ocho pruebas JVM originales de bandeja se conservan y cuatro nuevas cubren cursor/lecturas obsoletas/mutación excluyente.

[Alcance](ROUND2_RECORDS_TRAY.md), [fuentes/APK/suites](evidencias/records-correction-2026-10-10.json), [runner cifrado](evidencias/android-records-data-2026-10-10.txt), [runner UI](evidencias/android-records-ui-2026-10-10.txt). El primer intento UI falló porque la aserción inspeccionaba el texto hijo sin la semántica del botón; corregido el selector, los dos casos pasaron manteniendo la comprobación. Los scripts SQLite del host de migraciones 1→2→3 también pasaron; no sustituyen la nueva prueba cifrada 3→4.

Room v4 es el candidato de auditoría de esta rama. La aceptación humana de RECORD_CONTRACTS, giro real, TalkBack/visual completa, detalle editable en su rama dependiente y recursos cámara/GPS siguen pendientes. Sin backend, merge ni cierre de E1.


## Corrección R2 · alta/hub — 10/10/2026

Rama `feature/e1-ronda2-form-hub`, corrección sobre ef5a0f4. 109 JVM sin fallos/errores/omitidas; APK principal/instrumental y Lint correctos (20 avisos/0 errores). Ejecuciones nuevas en Xiaomi 11T Android 14/API 34: 27 de persistencia cifrada (29.645 s) y cuatro UI de formulario/Atrás (40.641 s), en grupos separados. UI asistida por ADB en MIUI; ninguna concesión de permisos ni limpieza de la base del usuario. Los cuatro casos UI usan estado/doble; no sustituyen un recorrido nuevo conjunto de alta con Room ni el giro real de MainActivity. Retención/cancelación/cierre están cubiertos con ViewModelStore en JVM.

[Alcance y pendientes](ROUND2_FORM_HUB.md), [fuentes/APK/suites](evidencias/form-correction-2026-10-10.json), [runner UI](evidencias/android-form-correction-2026-10-10.txt). Sin aceptación humana ni cierre global E1; las secciones siguientes siguen siendo evidencia histórica por incremento.


## Revisión R0/R1 — 10/10/2026

Sobre la base publicada 0f43a21, rama fix/e1-revision-rondas-0-1. Correcciones: escapes QR limitados a hexadecimal ASCII y prefijo +56 visible en formulario, con pegado de número completo sin duplicación y borrado a vacío. Sin cambios de Room v3, dependencias ni reglas de negocio.

**97 pruebas JVM aprobadas**, cero fallos/errores/omitidas. APK principal/instrumental correctos. **Lint 17 avisos, cero errores.** La nueva regresión QR falló antes de corregir el parser (aceptaba un Candidate para JSON no válido); pasó después. Las ocho pruebas de parser incluyen Unicode válido y pares sustitutos.

Xiaomi 11T, Android 14/API 34: **2 pruebas de formulario aprobadas en 19,578 s**, claro con fuente 1.0 y oscuro con fuente 1.5, escaladas sólo en la composición de prueba. Comprueban ingreso/pegado/borrado de teléfono, actividad con confirmación, consentimiento y descarte. [Salida instrumental](evidencias/android-form-review-2026-10-10.txt) · [conteos y hashes](evidencias/review-r01-2026-10-10.json).

Los primeros intentos quedaron esperando apertura de ActivityScenario; uno se interrumpió, luego la prueba individual pasó. El log identificó que el intent asistido sin MAIN/LAUNCHER no coincidía. En la ampliación de texto falló la búsqueda de un elemento aún no compuesto de LazyColumn; se corrigió el test para desplazarse primero con performScrollToNode, sin retirar aserciones. La ejecución final de los dos casos pasó.

Reproducción del caso específico tras instalar las APK, con pantalla desbloqueada:

```text
adb shell am instrument -w -r -e class io.github.grupo6.capturaprospectos.ui.admission.AdmissionFormUiTest io.github.grupo6.capturaprospectos.demo.debug.test/androidx.test.runner.AndroidJUnitRunner
```

En MIUI, desde otra terminal al comenzar cada caso y sólo si el inicio queda bloqueado:

```text
adb shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -f 0x10008000 -n io.github.grupo6.capturaprospectos.demo.debug/androidx.activity.ComponentActivity
```

Es una ejecución asistida, no una suite desatendida. No se cambiaron ajustes de fuente/tema/red del teléfono ni se borraron datos. No se repitieron persistencia, selector ni transición Wi-Fi: sus evidencias del 09/10 siguen siendo históricas. Pendientes: evaluación TalkBack/visual completa, portal cautivo en entorno controlado y revisión cruzada. La comprobación de acciones con letra ampliada no acredita por sí sola accesibilidad completa.


## Rondas 0 y 1 — 09/10/2026

R0 en ec9b93b y R1 preparada sobre esa base: **95 pruebas JVM aprobadas**, 0 fallos/errores/omitidas; assembleDemoDebug y assembleDemoDebugAndroidTest correctos; **Lint 19 avisos/0 errores**. R0 agregó ocho pruebas (79); R1 agregó dieciséis (95). [Resumen por suite y hashes de fuentes](evidencias/rounds-0-1-2026-10-09.json).

Ejecuciones separadas en Xiaomi 11T Android 14/API 34, no una suite conjunta:

| Código / alcance | Resultado |
|---|---|
| R0: paquete data (27 persistencia + 1 conectividad) | 28 aprobadas, 30,139 s; incluye A06 y 110 colecciones de red sin agotar callbacks |
| R0: ContextRoomUiTest | 1 aprobada, 16,302 s; selector Compose con Room cifrado y apertura asistida ADB |
| R1: ConnectivityTransitionTest, adaptador R0 sin cambios | 1 aprobada, 2,676 s; VALIDATED → OFFLINE → VALIDATED; Wi-Fi restaurado y estado 1 comprobado |
| R1: AdmissionFormUiTest | 1 aprobada, 20,499 s; ingreso volátil, confirmación de actividad, consentimiento y descarte; apertura asistida ADB |

[Salida de red](evidencias/android-wifi-2026-10-09.txt) y [salida de formulario](evidencias/android-form-2026-10-09.txt). Una primera prueba de formulario falló al buscar un nodo aún no compuesto en LazyColumn: se corrigió el test con performScrollToNode. El intento siguiente terminó con Process crashed tras interrupción/bloqueo del dispositivo; no se atribuye una causa técnica concluyente ni se cuenta como aprobado. El reintento completo con teléfono desbloqueado pasó. No se ocultaron fallos ni se redujeron aserciones.

Alcance en [ROUND0_BASE](ROUND0_BASE.md) y [ROUND1_INTERFACES](ROUND1_INTERFACES.md). Sin migración Room, nuevas dependencias, backend, publicación ni RF completo cerrado. Revisión visual/accesibilidad y aceptación humana pendientes. Portal cautivo no probado. La firma de registros es propuesta: no acredita DAO de edición/anulación. El formulario todavía no está conectado a MainActivity ni guarda.

## ViewModel y Route de contexto — 09/10/2026

Incremento A2 sobre 1a8f8e9: 71 JVM aprobadas (13 nuevas), sin fallos/errores/omitidas; build demoDebug y APK instrumental correctos. Lint 19 avisos/0 errores, incluidos dos avisos adicionales de versiones disponibles para Lifecycle declarado explícitamente. Sin ejecución Android nueva, apertura Room ni conexión de MainActivity. [Alcance y comprobaciones](CONTEXT_UI.md); [evidencia por suite y hashes](evidencias/context-viewmodel-2026-10-09.json).


## Componentes de contexto — 08/10/2026

Incremento E1-BASE-03-A1 sobre 978ca91: 58 JVM, build demoDebug e instrumental correctos; Lint 17 avisos/0 errores. Seis pruebas Compose aprobadas en Xiaomi 11T Android 14/API 34 mediante apertura asistida de la actividad de prueba por ADB, debido al bloqueo de segundo plano de MIUI. No se repitió la suite A06 ni se declara ejecución conjunta de 34 tests. [Alcance y reproducción](CONTEXT_UI.md). UI presentacional; conexión a Room y revisión visual/humana pendientes.


## Corrección A06 verificada — 08/10/2026

Rama fix/e1-a06-lectura-registros, basada en 3870166. Room v3 sin cambios de esquema/migración. Lectores F2027 y Legacy convierten errores de fecha/zona, UUID, enum y ubicación incompleta/desconocida en StorageUnavailable. Los valores nulos requeridos producen error controlado; no se captura CancellationException ni se restablece la base.

58 JVM pasadas sin fallos/errores/omitidas; assembleDemoDebug y assembleDemoDebugAndroidTest correctos; Lint 17 avisos/0 errores. Suite completa ejecutada por ADB directo en Xiaomi 11T (21081111RG), Android 14/API 34: **OK (28 tests)**, 29.492 s, 28 resultados individuales correctos. Incluye 24 anteriores y cuatro nuevas: matrices de 11 valores inválidos en F2027 y Legacy (ambos lectores), conservación de filas/lectura de registros sanos/reapertura y propagación de cancelación en ambos repositorios. Sólo bases test-* y datos ficticios.

[Salida del runner](evidencias/android-a06-instrumentation-2026-10-08.txt). A06 queda corregido y probado en este alcance; revisión humana e integración pendientes. No acredita toda corrupción posible, otros dispositivos ni RF completos. Los resultados del 06/10 siguientes son históricos. Las distribuciones congeladas del producto 0.1.13 y marco interno 0.1.15 no contienen este incremento; no se regeneran ni publican automáticamente.


## Revisión de estructura y límite conocido — 08/10/2026

Revisión estática del código de dominio, almacenamiento/contexto y configuración, sin nuevas pruebas Android. Continúa la evidencia del 06/10 sobre 2298220. Antes de conectar detalle/recuperación, reforzar la decodificación de filas inválidas: RoomAdmissionRepository.get no captura DateTimeException de Instant.parse/ZoneId.of; el lector Legacy tampoco convierte todos los errores de formato en StorageUnavailable. Añadir casos de fecha/zona/enum/ubicación corruptas y comprobar preservación de las filas. No se observó pérdida de datos en esta revisión ni se probó este caso sobre Android. La UI y los RF completos continúan pendientes.


Verificación 06/10/2026. Implementación `2298220`; README histórico `d658e76`. Entorno de build Windows/JDK 25; instrumento Xiaomi 11T, Android 14/API 34. No se conserva su identificador ADB. [Comandos reproducibles](../android/README.md).

| Comprobación | Resultado observado | Alcance |
|---|---|---|
| testDemoDebugUnitTest | 58 tests, sin fallos/errores/omisiones | 55 de dominio, dos de seed y uno de plantilla |
| assembleDemoDebug | Correcto | APK de desarrollo, no firma release |
| lintDemoDebug | 17 avisos, 0 errores | No acredita ausencia de todo problema |
| assembleDemoDebugAndroidTest | Correcto | Compilación del APK instrumental |
| Runner Android directo sobre v3 | OK (24 tests), 23.178 s | 8 contexto, 7 F2027, 8 almacenamiento cifrado, 1 plantilla |
| Verificadores SQL 1→2 y 1→2→3 | Pasaron en host | SQLite en memoria, sin simular Keystore/SQLCipher Android |

Suite actual: [salida del runner v3](evidencias/android-context-instrumentation-2026-10-06.txt). La ejecución anterior sobre `91b4983`/Room v2 dio 16 pruebas aprobadas en 13.87 s: [evidencia histórica](evidencias/android-instrumentation-2026-10-06.txt).

El instalador Gradle falló por restricción de permisos del dispositivo. Su BUILD SUCCESSFUL no acreditó instrumentación; se instalaron ambos APK con ADB sin `-g` y se ejecutó AndroidJUnitRunner. Las pruebas indicadas corresponden a ese runner, no al intento fallido de instalación.

SHA-256 del APK probado `2298220`: `5707c6cd9dc2703e63f43d15dbb2b02ef96b789f4166fd79685b661d31807c34`. Un build posterior puede generar otro hash; registrar el suyo y su commit, sin atribuirle automáticamente esta ejecución.

## Pendientes reales

Revisión humana y reproducción por compañero; conexión del formulario al alta, bandeja/edición/anulación, cámara/GPS y presentación de red, métricas y administración. Selector/Room y observador de red ya implementados. Backup/transferencia efectiva, rendimiento de 400 registros, accesibilidad, otros dispositivos/entornos y release firmado no comprobados. Ningún RF completo cerrado. No hay backend, ACK ni integración remota.
