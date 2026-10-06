# Captura de Prospectos — Android

Base académica E1 del Grupo 6, al 06/10/2026. Dominio, persistencia F2027 y contexto/catálogo sintético compilados; pantalla Compose todavía de plantilla. Backend pendiente E2.

## Abrir y verificar

Abrir android en Android Studio. SDK plataforma 37, minSdk 26, JDK 25 para Gradle (criterio en gradle/gradle-daemon-jvm.properties). Usar wrapper Gradle 9.6.0 y versiones de gradle/libs.versions.toml. local.properties es propio de cada equipo y no se comparte.

Seleccionar demoDebug y ejecutar desde android:

```powershell
.\gradlew.bat :app:testDemoDebugUnitTest :app:assembleDemoDebug :app:lintDemoDebug :app:assembleDemoDebugAndroidTest --console=plain
```

Comando verificado en Windows: 58 JVM pasan (55 de dominio, dos de coherencia del seed y una de plantilla), 17 avisos Lint y 0 errores. APK principal y de pruebas compilados. Quince pruebas de persistencia (ocho previas y siete F2027) y la instrumental de plantilla **pasaron en Android 14/API 34 el 06/10**, mediante ADB directo sin concesión automática de permisos. Para ejecutarlas, conectar un dispositivo de pruebas con depuración USB autorizada o un emulador compatible y usar:

```powershell
.\gradlew.bat :app:connectedDemoDebugAndroidTest --console=plain
```

Este comando instala app/pruebas. Las pruebas de almacenamiento crean bases test-* independientes y limpian sólo sus propios archivos/alias. No confundir compilación con cifrado/recuperación comprobados.

APK: app/build/outputs/apk/demo/debug/app-demo-debug.apk. Reporte Lint: app/build/reports/lint-results-demoDebug.html. Identidad: io.github.grupo6.capturaprospectos.demo.debug. Nombre visible: Captura de Prospectos.

## Incremento implementado

domain.capture es Kotlin independiente de Android: normalización/validación, consentimiento, estados, ubicación y reglas de alcance. PrepareCapture prepara altas, no confirma guardado. RoomCaptureRepository comprueba contexto/duplicados y devuelve Saved después del commit; recupera detalle autorizado.

Room 2.8.5, KSP 2.3.10, SQLCipher 4.19.1, SQLite 2.7.0 y coroutines 1.10.2. Esquema v1 en app/schemas: consentimiento, captura, intereses, clave activa RUT/evento y auditoría inicial. La contraseña se envuelve con AES-GCM/Android Keystore; ante pérdida de clave se conserva la base, sin reset. No añadir fallbackToDestructiveMigration.

## Límites y continuación

AdmissionContextSource tiene implementación RoomDemoContextRepository para contexto y catálogo persistidos; falta ejecución Android de este incremento, ViewModel, formulario/listado/detalle, edición/anulación y recursos GPS/cámara/conectividad. Los snapshots de intereses no acreditan oferta vigente para otra alta. No declarar RF completos ni seguridad comprobada hasta ejecutar las pruebas y recorridos.

Demo tiene identidad propia; no se importan sus datos automáticamente a E2. Backup/transferencia están excluidos por configuración, pendientes de comprobación efectiva. No se requieren servicios ni secretos de nube para compilar. Sólo datos ficticios.

No versionar build, cachés, local.properties ni claves. Git es local, sin remoto/publicación; revisión por otro integrante antes de integrar feature → develop → main. El APK debug no sustituye una entrega funcional ni una firma release. Próximo recorrido: contexto sintético → captura consentida → guardado cifrado → detalle recuperable.

## Adaptación F2027 de dominio — 06/10/2026

`domain.admission` prepara entradas nuevas con nombres separados, nacimiento/residencia,
actividad y establecimiento condicional, preferencias opcionales, uno a tres programas
priorizados y sedes independientes. `PrepareAdmission` recibe catálogos sintéticos,
consentimiento, reloj y zona del evento explícitos. Los canales y modalidades admitidos
se inyectan; no se inventan valores institucionales. Sin actividad ni opciones preseleccionadas.

Prepared no acredita autorización ni guardado. Room v2 y RoomAdmissionRepository implementan
alta transaccional F2027 y lectura versionada (Found/Legacy), con permisos del evento,
consentimiento inmutable, duplicados, ranking, sedes y auditoría. RoomCaptureRepository
se conserva para el contrato antiguo; no usarlo para altas F2027.

Migration 1→2 conserva columnas/filas antiguas, agrega campos desconocidos como null
y LEGACY_1 como versión. No divide nombres ni inventa preferencias. Las copias de
migración permanecen dentro de la misma base cifrada y la transacción; no se desactivan
FK ni se reinician claves. Esquemas 1.json conservado y 2.json exportado.

56 JVM pasan; siete pruebas instrumentadas F2027 agregadas y compiladas, **ejecutadas con éxito en Android 14/API 34**.
La suite de migración/recuperación cifrada pasó en el dispositivo probado; backup/transferencia y otros dispositivos siguen pendientes. Complemento
reproducible con Python 3, sin dependencias, SQLite en memoria del host:

```text
python tools/verify_migration_sql.py
```

Este control pasó: conserva todas las columnas v1, comprueba esquema v2/FK/índices,
ausencia de datos inferidos y rollback de un fallo a mitad de migración. No sustituye
las pruebas Room/SQLCipher/Keystore en dispositivo.

La pantalla sigue siendo la plantilla. Contexto/catálogos persistidos implementados posteriormente; faltan su ejecución Android,
UI, edición/anulación y recursos nativos. Sin RF cerrado ni revisión cruzada completada.

## Ejecución instrumental verificada — 06/10/2026

Código probado: 91b4983. Dispositivo de pruebas Xiaomi 11T, Android 14/API 34.
Resultado AndroidJUnitRunner: OK (16 tests), 13.87 s; quince pruebas de almacenamiento
más una de contexto de aplicación. Sin fallos ni omisiones.

El ejecutor Gradle no logró instalar el APK de pruebas porque el dispositivo restringe
INSTALL_GRANT_ALL_REQUESTED_PERMISSIONS. Aunque imprimió BUILD SUCCESSFUL, ese intento
no ejecutó tests. Se instalaron ambos APK sin -g y se ejecutó el mismo runner directamente:

```powershell
adb install -r -t app/build/outputs/apk/demo/debug/app-demo-debug.apk
adb install -r -t app/build/outputs/apk/androidTest/demo/debug/app-demo-debug-androidTest.apk
adb shell am instrument -w -r io.github.grupo6.capturaprospectos.demo.debug.test/androidx.test.runner.AndroidJUnitRunner
```

Comprobar los resultados de cada test y OK (16 tests), no sólo el código de salida de ADB.
Esta alternativa es para la suite actual de almacenamiento, que no requiere permisos
peligrosos; futuras pruebas GPS/cámara requieren su propio manejo de permisos.
No acredita backup/transferencia efectiva, recursos nativos, UI ni cierre de RF.

## Contexto y catálogo persistidos — 06/10/2026

Room v3 añade nueve tablas de contexto/catálogo sin modificar filas ni esquemas v1/v2.
Migración 2→3 aditiva; cadena 1→2→3 disponible. Inicialización de fixtures explícita,
transaccional e idempotente mediante RoomDemoContextRepository.initialize(); no forma
parte de la migración ni se ejecuta todavía desde la pantalla de plantilla.

Tres perfiles DEMO, tres eventos, cuatro sedes, cuatro programas (incluye sección online)
y dos menciones subordinadas, todos ficticios. No importa las carreras del cupón como
oferta institucional. Consentimiento ficticio versionado; no crea capturas personales.
No selecciona perfil/evento ni respuestas del prospecto automáticamente.

API para UI: snapshot/observe, selectProfile, selectEvent y clearSelection. Cambiar perfil
limpia el evento; cerrar selección conserva deviceId y capturas. environment sólo entrega
el evento seleccionado y autorizado; usar esta fuente con RoomAdmissionRepository.
El supervisor conserva acceso de lectura por scope pero no selecciona evento para capturar.
No es autenticación real. La sugerencia de establecimiento nunca se aplica al formulario
sin confirmación. Coordenadas de fixtures ausentes; no fabricar GPS.

El catálogo es inmutable en este incremento. Futuras ediciones de eventos/asignaciones o
catálogo requieren revisión, auditoría/versionado y ampliar invalidación del Flow; no modificar
las tablas directamente desde UI. Modalidad/canal opcionales no reciben opciones inventadas.

58 JVM pasan; ocho pruebas DemoContextTest nuevas compiladas, **no ejecutadas**.
La suite actual tiene 24 pruebas instrumentadas. Las 16 aprobadas anteriormente corresponden
al código 91b4983/Room v2, no acreditan este incremento Room v3.

```text
python tools/verify_context_migration_sql.py
```

Control SQL del host ejecutado: cadena 1→2→3, preservación de filas, esquema/FK/índices,
sin seed automático y rollback. Pendiente ejecutar suite completa en Android y revisión.
Sin dependencias nuevas, UI conectada, administración de eventos ni RF cerrado.
