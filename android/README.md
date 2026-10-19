# Captura de Prospectos — Android

Develop `0c64533` integra el flujo E1: bienvenida → credenciales demo → evento → menú → alta F2027 consentida en Room cifrado; bandeja/detalle/edición/anulación, cámara QR sintética, GPS confirmado, métricas y eventos. [R4 QA](../docs/ROUND4_QA.md) registra construcción desde clon remoto limpio del 14/10: 191 JVM, Lint 27 avisos/0 errores y APK debug/instrumental/release unsigned. Evidencias físicas/Android anteriores conservan sus versiones; no se atribuyen a la nueva compilación. Revisión técnica externa reportada al integrar; aceptación contractual, accesibilidad completa, permisos/redes adversas, tablet y entrega pendientes. No acredita cierre E1.

Consultar [R0](../docs/ROUND0_BASE.md), [R1](../docs/ROUND1_INTERFACES.md) y [evidencia de pruebas](../docs/ANDROID_BASELINE.md).

## Entorno fijado

| Elemento | Valor en el proyecto |
|---|---|
| Gradle wrapper / AGP | 9.6.0 / 9.4.1 |
| JDK del daemon | 25; criterio en gradle/gradle-daemon-jvm.properties |
| Compatibilidad fuente/bytecode | Java 11; diferente del JDK que ejecuta Gradle |
| SDK compile / target / mínimo | 37 / 37 / 26 |
| Compose plugin / BOM | 2.2.10 / 2026.02.01 |
| Room / KSP | 2.8.5 / 2.3.10 |
| CameraX / ML Kit barcode incluido | 1.5.3 / 17.3.0 |
| SQLCipher / SQLite / coroutines | 4.19.1 / 2.7.0 / 1.10.2 |

Las versiones efectivas están en `gradle/libs.versions.toml`, wrapper y build del módulo; no regenerar el proyecto ni actualizar dependencias para reproducir esta base.

## Preparación

1. Obtener una copia identificada del código. Abrir esta carpeta `android/` en Android Studio.
2. Instalar plataforma SDK 37 y un JDK 25 compatible con el criterio del daemon. Configurar el SDK local mediante Android Studio/local.properties, excluido de Git.
3. Seleccionar variante `demoDebug`. El applicationId instalado será `io.github.grupo6.capturaprospectos.demo.debug`; namespace base `io.github.grupo6.capturaprospectos`.
4. Desde esta carpeta ejecutar en PowerShell:

```powershell
.\gradlew.bat :app:testDemoDebugUnitTest :app:assembleDemoDebug :app:lintDemoDebug :app:assembleDemoDebugAndroidTest --console=plain
```

En Linux/macOS usar `./gradlew` con el mismo JDK/SDK; esos entornos todavía no se han reproducido. La variante demo funciona sin backend. En el selector, «Preparar demo local» inicializa explícitamente perfiles, eventos y catálogo ficticios; no crea prospectos ni selecciona un contexto automáticamente.

Evidencia R0/R1 del 09/10: 95 JVM aprobadas y Lint sin errores. Los avisos y condiciones de cada ejecución están identificados en ANDROID_BASELINE. APK principal: `app/build/outputs/apk/demo/debug/app-demo-debug.apk`; APK de pruebas: `app/build/outputs/apk/androidTest/demo/debug/app-demo-debug-androidTest.apk`; Lint: `app/build/reports/lint-results-demoDebug.html`.

## Pruebas Android

Conectar dispositivo de pruebas API >=26 con depuración USB autorizada, o emulador compatible; confirmar que ADB lo muestra autorizado. Los tests usan bases/alias `test-*`, distintos de la base de la app. Revisar ese aislamiento antes de modificar tests destructivos.

```powershell
.\gradlew.bat :app:connectedDemoDebugAndroidTest --console=plain
```

Verificar que se ejecutaron los tests, no sólo el mensaje final del build. En el dispositivo probado, el instalador de Gradle encontró una restricción de concesión automática de permisos y no ejecutó la suite aunque imprimió BUILD SUCCESSFUL. Se resolvió con instalación sin `-g` y el runner directo:

```powershell
adb install -r -t app/build/outputs/apk/demo/debug/app-demo-debug.apk
adb install -r -t app/build/outputs/apk/androidTest/demo/debug/app-demo-debug-androidTest.apk
adb shell am instrument -w -r io.github.grupo6.capturaprospectos.demo.debug.test/androidx.test.runner.AndroidJUnitRunner
```

El antecedente R0/R1 se obtuvo en grupos separados: 28 pruebas de datos/red, una de selector con Room, una de transición Wi-Fi y una de formulario. Las evidencias posteriores están fechadas en ANDROID_BASELINE; este corte R4 no ejecutó pruebas Android. En MIUI las pruebas UI requirieron apertura asistida por ADB; el comando general no acredita ejecución desatendida de esos casos. La transición Wi-Fi requiere habilitación y preparación específicas; consultar ANDROID_BASELINE. Comprobar resultados individuales y omisiones. La alternativa no concede permisos peligrosos: futuros tests de cámara/GPS necesitarán su propio manejo de permisos.

## Implementación y continuidad

Nuevas altas: `domain.admission` → `AdmissionRepository` / `RoomAdmissionRepository`. `PrepareAdmission` prepara una entrada; el repositorio valida autorización/consentimiento y confirma guardado tras commit. El contrato antiguo `domain.capture` se conserva para compatibilidad; no convertir altas F2027 a `CaptureInput`.

Room v4 conserva esquemas 1/2/3/4 y migraciones 1→2→3→4. La migración aditiva de auditoría conserva todos los datos v3 y deja NULL los atributos históricos desconocidos. No inventa nombres separados, nacimiento, prioridades ni sedes para registros antiguos. La clave SQLCipher se protege con Keystore; fallo de clave no borra la base. No usar migración destructiva.

`RoomDemoContextRepository.initialize()` siembra perfiles/eventos/catálogo explícitamente, sin prospectos ni selección automática. El botón «Preparar demo local» del selector invoca esta inicialización. Ver [DEMO_CONTEXT](../docs/DEMO_CONTEXT.md) para el consumidor y sus estados.

Verificación SQL adicional desde esta carpeta, con Python 3 y sin librerías externas:

```text
python tools/verify_migration_sql.py
python tools/verify_context_migration_sql.py
```

Ambos controles pasaron en SQLite del host; no sustituyen Room/SQLCipher/Keystore en Android.

Alta, bandeja/detalle/edición, administración y métricas están integradas en develop, con evidencia por incremento. R4 continúa con la matriz de permisos, redes reales, adaptación/accesibilidad y revisión de aceptación. Backup/transferencia están excluidos por configuración, pendientes de prueba efectiva. Release unsigned compila, pero no acredita firma, instalación o RF completos. No guardar secretos, datos reales, build, cachés, local.properties o claves en Git.
