# Captura de Prospectos — Android

Incremento 08/10: tema y componentes de contexto preparados, con seis pruebas UI aprobadas mediante apertura asistida por ADB en MIUI. MainActivity conserva la plantilla; conexión a Room y revisión humana pendientes. Ver [CONTEXT_UI](../docs/CONTEXT_UI.md).

Base E1 al 06/10/2026, implementación `2298220`, documentación previa `d658e76`. Dominio/persistencia F2027 y contexto/catálogo demo implementados; UI de plantilla. [Requisitos](../docs/SRS.md), [alcance E1/E2](../docs/ENTREGAS.md) y [evidencia](../docs/ANDROID_BASELINE.md).

## Entorno fijado

| Elemento | Valor en el proyecto |
|---|---|
| Gradle wrapper / AGP | 9.6.0 / 9.4.1 |
| JDK del daemon | 25; criterio en gradle/gradle-daemon-jvm.properties |
| Compatibilidad fuente/bytecode | Java 11; diferente del JDK que ejecuta Gradle |
| SDK compile / target / mínimo | 37 / 37 / 26 |
| Compose plugin / BOM | 2.2.10 / 2026.02.01 |
| Room / KSP | 2.8.5 / 2.3.10 |
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

En Linux/macOS usar `./gradlew` con el mismo JDK/SDK; esos entornos todavía no se han reproducido. Los cuatro pasos anteriores no activan un backend ni cargan el contexto desde la pantalla.

Resultados comprobados en Windows: 58 JVM sin fallos y 17 avisos Lint/0 errores. APK principal: `app/build/outputs/apk/demo/debug/app-demo-debug.apk`; APK de pruebas: `app/build/outputs/apk/androidTest/demo/debug/app-demo-debug-androidTest.apk`; Lint: `app/build/reports/lint-results-demoDebug.html`.

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

Esta suite pasó con `OK (24 tests)` sobre Room v3 en Android 14/API 34, 23.178 s. Comprobar resultados individuales y ausencia de fallos/omisiones. La alternativa no concede permisos peligrosos: futuros tests de cámara/GPS necesitarán su propio manejo de permisos.

## Implementación y continuidad

Nuevas altas: `domain.admission` → `AdmissionRepository` / `RoomAdmissionRepository`. `PrepareAdmission` prepara una entrada; el repositorio valida autorización/consentimiento y confirma guardado tras commit. El contrato antiguo `domain.capture` se conserva para compatibilidad; no convertir altas F2027 a `CaptureInput`.

Room v3 conserva esquemas 1/2/3 y migraciones 1→2→3. No inventa nombres separados, nacimiento, prioridades ni sedes para registros antiguos. La clave SQLCipher se protege con Keystore; fallo de clave no borra la base. No usar migración destructiva.

`RoomDemoContextRepository.initialize()` siembra perfiles/eventos/catálogo explícitamente, sin prospectos ni selección automática. La pantalla aún no lo llama. Ver [DEMO_CONTEXT](../docs/DEMO_CONTEXT.md) para el consumidor y sus estados.

Verificación SQL adicional desde esta carpeta, con Python 3 y sin librerías externas:

```text
python tools/verify_migration_sql.py
python tools/verify_context_migration_sql.py
```

Ambos controles pasaron en SQLite del host; no sustituyen Room/SQLCipher/Keystore en Android.

Faltan UI, edición/anulación, recursos nativos, administración, métricas y revisión cruzada. Backup/transferencia están excluidos por configuración, pendientes de prueba efectiva. El APK debug no acredita release firmado ni RF completos. No guardar secretos, datos reales, build, cachés, local.properties o claves en Git.
