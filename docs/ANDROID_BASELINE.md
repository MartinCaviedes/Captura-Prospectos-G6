# Evidencia de la base Android

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
