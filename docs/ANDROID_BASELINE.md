# Evidencia de la base Android

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

Pantalla de plantilla, revisión humana y reproducción por compañero; navegación/formulario, CRUD completo, recursos GPS/cámara/conectividad, métricas y administración. Backup/transferencia efectiva, rendimiento de 400 registros, accesibilidad, otros dispositivos/entornos y release firmado no comprobados. Ningún RF completo cerrado. No hay backend, ACK ni integración remota.
