# Captura de Prospectos — Android

API externa candidata: [localidad aproximada del GPS con BigDataCloud](../docs/EXTERNAL_GEOCODING_CONTRACT.md), sin cuenta/clave, consulta explícita y errores recuperables. Solo se consultan coordenadas actuales del propio dispositivo; las pruebas automáticas usan HTTP local y no envían posiciones ficticias al proveedor. Las ampliaciones del SRS acordadas con Martín se evaluarán después del alcance fundamental, sin declararlas descartadas ni cumplidas.

Para pruebas físicas mediante USB: compilar con `-Pg6ApiBaseUrl=http://127.0.0.1:8080/` y usar `adb -s <dispositivo> reverse tcp:8080 tcp:<puerto-local-gateway>`. La URL solo configura el backend propio; BigDataCloud conserva su HTTPS directo. Sin esa propiedad, se mantiene `http://10.0.2.2:8080/` para el emulador. No incluir credenciales en la URL; para un servidor remoto usar HTTPS.

Incrementos locales del 21–22/11: [cuentas y administración](../docs/ACCOUNT_ANDROID_VALIDATION.md), [sesión local offline hasta 12 horas](../docs/OFFLINE_SESSION_VALIDATION.md) y [cola persistente de sincronización](../docs/PROSPECT_SYNC_CONTRACT.md). ADMIN usa navegación independiente; se retiró el fallback de credenciales locales. Se requiere backend actualizado para el primer acceso. Los JWT nuevos duran cuatro horas; el permiso local no extiende ese plazo. Reiniciar el dispositivo exige conexión. La cola continúa al abrir la aplicación y autenticar; no se ejecuta con la app cerrada ni resuelve conflictos por sobrescritura. Las fichas anteriores requieren conciliación. La descripción del corte 10ba22d inferior es histórica y no acredita cierre de E2.

Develop `10ba22d` integra el flujo funcional completo sin dependencias de datos DEMO:
- **Bienvenida → Acceso real (inicio de sesión o registro de cuenta) → Selección o creación de evento → Menú principal (`Inicio`, `Registros`, `Resumen`, `Perfil`) → Alta F2027 consentida en Room cifrado y sincronización remota con el API Gateway mediante Retrofit.**
- El catálogo base (sedes, carreras y menciones de Admisión Duoc UC 2027, junto con el texto de consentimiento) se inicializa automáticamente al abrir la base de datos por primera vez (`ensureCatalogInitialized()` / `initializeCatalog()`), sin precargar perfiles ni eventos ficticios.
- Tanto los usuarios con rol `CAPTOR` como `SUPERVISOR` pueden registrar eventos en sus sedes autorizadas desde el selector de contexto y desde el panel.
- En `Perfil` se muestra el correo y rol reales del usuario autenticado, habiéndose eliminado el cargador de prospectos ficticios.

Consultar [R0](../docs/ROUND0_BASE.md), [R1](../docs/ROUND1_INTERFACES.md), [R4 QA](../docs/ROUND4_QA.md) y [evidencia de pruebas](../docs/ANDROID_BASELINE.md).

## Entorno fijado

Sesión online actual: hasta cuatro horas, con [restauración cifrada y foto en Inicio](../docs/REMEMBERED_SESSION.md). El registro offline de doce horas permanece separado. Las pruebas físicas anteriores de cámara, galería y apodo fueron confirmadas por Martín en Redmi 9A.

| Elemento | Valor en el proyecto |
|---|---|
| Gradle wrapper / AGP | 9.6.0 / 9.4.1 |
| JDK del daemon | 25; criterio en gradle/gradle-daemon-jvm.properties |
| Compatibilidad fuente/bytecode | Java 11; diferente del JDK que ejecuta Gradle |
| SDK compile / target / mínimo | 37 / 37 / 26 |
| Compose plugin / BOM | 2.2.10 / 2026.02.01 |
| Room / KSP | 2.8.5 / 2.3.10 |
| Retrofit / OkHttp / Gson | 2.11.0 / 4.12.0 / 2.11.0 |
| CameraX / ML Kit barcode incluido | 1.5.3 / 17.3.0 |
| SQLCipher / SQLite / coroutines | 4.19.1 / 2.7.0 / 1.10.2 |

Las versiones efectivas están en `gradle/libs.versions.toml`, wrapper y build del módulo.

## Preparación

1. Obtener una copia identificada del código. Abrir esta carpeta `android/` en Android Studio.
2. Instalar plataforma SDK 37 y un JDK 25 compatible con el criterio del daemon. Configurar el SDK local mediante Android Studio/local.properties, excluido de Git.
3. Seleccionar variante `demoDebug`. El applicationId instalado será `io.github.grupo6.capturaprospectos.demo.debug`; namespace base `io.github.grupo6.capturaprospectos`.
4. Desde esta carpeta ejecutar en PowerShell:

```powershell
.\gradlew.bat :app:testDemoDebugUnitTest :app:assembleDemoDebug --console=plain
```

La aplicación conserva capturas y outbox en Room/SQLCipher. Guardado y pendiente son atómicos; transmite con JWT vigente y `/me` verificado contra el `api-gateway` (`http://10.0.2.2:8080/` en emulador). Reintenta fallos transitorios con espera persistida. Conflictos y fichas anteriores se conservan para revisión, sin reenviarlas a ciegas. El detalle distingue guardado local, pendiente, confirmado y revisión. Room migra de 4 a 5 sin borrar datos.

APK principal: `app/build/outputs/apk/demo/debug/app-demo-debug.apk`; APK de pruebas: `app/build/outputs/apk/androidTest/demo/debug/app-demo-debug-androidTest.apk`.

## Pruebas Android

Cobertura JaCoCo, alcance de lógica, unión JVM/emulador y comprobación del 80 %: [guía reproducible](../docs/COVERAGE_VALIDATION.md). La instrumentación de cobertura se activa con `-Pg6AndroidCoverage=true`; no es necesaria para instalar la APK normal en el teléfono.

Conectar dispositivo de pruebas API >=26 con depuración USB autorizada, o emulador compatible; confirmar que ADB lo muestra autorizado. Los tests usan bases/alias `test-*`, distintos de la base de la app.

```powershell
.\gradlew.bat :app:connectedDemoDebugAndroidTest --console=plain
```

Si el dispositivo restringe la concesión automática de permisos durante la instalación por Gradle, instalar sin `-g` y ejecutar el runner directo:

```powershell
adb install -r -t app/build/outputs/apk/demo/debug/app-demo-debug.apk
adb install -r -t app/build/outputs/apk/androidTest/demo/debug/app-demo-debug-androidTest.apk
adb shell am instrument -w -r io.github.grupo6.capturaprospectos.demo.debug.test/androidx.test.runner.AndroidJUnitRunner
```

## Implementación y continuidad

- **Altas y persistencia local (`domain.admission` → `RoomAdmissionRepository` / `RoomRecordRepository`):** `PrepareAdmission` valida todos los campos F2027; el repositorio verifica autorización y consentimiento en una transacción Room cifrada y confirma el guardado tras el commit.
- **Sincronización con Backend (`data.remote.GatewayApiClient`):** Si existe una sesión JWT activa en `BackendSession`, las altas confirmadas (`POST /api/v1/prospects`), ediciones (`PUT /api/v1/prospects/{id}`) y anulaciones (`POST /api/v1/prospects/{id}/cancel`) se envían a través del `api-gateway`. Ante cualquier error de red, el registro local se conserva íntegro en Room sin pérdida de información.
- **Cuentas y eventos (`RoomDemoContextRepository`):**
  - `initializeCatalog()` instala únicamente el catálogo base de sedes, carreras, menciones y versión de consentimiento sin perfiles ni eventos ficticios.
  - `registerAccount(...)` y `authenticate(...)` gestionan las cuentas de usuario en la tabla cifrada `local_user_accounts` (hash SHA-256 con salt aleatorio) vinculada a `demo_profiles` y `demo_profile_sites`.
  - `saveEvent(...)` permite crear y editar eventos tanto a `SUPERVISOR` como a `CAPTOR` dentro de sus sedes autorizadas.
- **Esquema Room v4:** Conserva esquemas 1/2/3/4 y migraciones no destructivas `1→2→3→4`, con clave SQLCipher protegida en `AndroidKeyStore`.

Verificación SQL adicional desde esta carpeta, con Python 3:

```text
python tools/verify_migration_sql.py
python tools/verify_context_migration_sql.py
```
