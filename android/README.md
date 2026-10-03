# Captura de Prospectos — Android

Base académica E1 del Grupo 6, al 03/10/2026. Dominio de captura y persistencia inicial compilados; pantalla Compose todavía de plantilla. Backend pendiente E2.

## Abrir y verificar

Abrir android en Android Studio. SDK plataforma 37, minSdk 26, JDK 25 para Gradle (criterio en gradle/gradle-daemon-jvm.properties). Usar wrapper Gradle 9.6.0 y versiones de gradle/libs.versions.toml. local.properties es propio de cada equipo y no se comparte.

Seleccionar demoDebug y ejecutar desde android:

```powershell
.\gradlew.bat :app:testDemoDebugUnitTest :app:assembleDemoDebug :app:lintDemoDebug :app:assembleDemoDebugAndroidTest --console=plain
```

Comando verificado en Windows: 30 JVM pasan (29 de dominio y una de plantilla), 17 avisos Lint y 0 errores. APK principal y de pruebas compilados. Ocho pruebas nuevas de persistencia y la instrumental de plantilla **no se han ejecutado en Android**. Para ejecutarlas, conectar un dispositivo de pruebas con depuración USB autorizada o un emulador compatible y usar:

```powershell
.\gradlew.bat :app:connectedDemoDebugAndroidTest --console=plain
```

Este comando instala app/pruebas. Las pruebas de almacenamiento crean bases test-* independientes y limpian sólo sus propios archivos/alias. No confundir compilación con cifrado/recuperación comprobados.

APK: app/build/outputs/apk/demo/debug/app-demo-debug.apk. Reporte Lint: app/build/reports/lint-results-demoDebug.html. Identidad: io.github.grupo6.capturaprospectos.demo.debug. Nombre visible: Captura de Prospectos.

## Incremento implementado

domain.capture es Kotlin independiente de Android: normalización/validación, consentimiento, estados, ubicación y reglas de alcance. PrepareCapture prepara altas, no confirma guardado. RoomCaptureRepository comprueba contexto/duplicados y devuelve Saved después del commit; recupera detalle autorizado.

Room 2.8.5, KSP 2.3.10, SQLCipher 4.19.1, SQLite 2.7.0 y coroutines 1.10.2. Esquema v1 en app/schemas: consentimiento, captura, intereses, clave activa RUT/evento y auditoría inicial. La contraseña se envuelve con AES-GCM/Android Keystore; ante pérdida de clave se conserva la base, sin reset. No añadir fallbackToDestructiveMigration.

## Límites y continuación

CaptureContextSource sólo tiene implementación en pruebas: faltan contexto persistido, seeds ficticios, ViewModel, formulario/listado/detalle, edición/anulación y los recursos GPS/cámara/conectividad. Los snapshots de intereses no acreditan oferta vigente para otra alta. No declarar RF completos ni seguridad comprobada hasta ejecutar las pruebas y recorridos.

Demo tiene identidad propia; no se importan sus datos automáticamente a E2. Backup/transferencia están excluidos por configuración, pendientes de comprobación efectiva. No se requieren servicios ni secretos de nube para compilar. Sólo datos ficticios.

No versionar build, cachés, local.properties ni claves. Git es local, sin remoto/publicación; revisión por otro integrante antes de integrar feature → develop → main. El APK debug no sustituye una entrega funcional ni una firma release. Próximo recorrido: contexto sintético → captura consentida → guardado cifrado → detalle recuperable.
