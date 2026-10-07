# Evidencia de la base Android

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
