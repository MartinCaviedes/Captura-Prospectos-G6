# Captura de Prospectos — Android

Base académica E1 del Grupo 6, al 06/10/2026. Dominio de captura y persistencia inicial compilados; pantalla Compose todavía de plantilla. Backend pendiente E2.

## Abrir y verificar

Abrir android en Android Studio. SDK plataforma 37, minSdk 26, JDK 25 para Gradle (criterio en gradle/gradle-daemon-jvm.properties). Usar wrapper Gradle 9.6.0 y versiones de gradle/libs.versions.toml. local.properties es propio de cada equipo y no se comparte.

Seleccionar demoDebug y ejecutar desde android:

```powershell
.\gradlew.bat :app:testDemoDebugUnitTest :app:assembleDemoDebug :app:lintDemoDebug :app:assembleDemoDebugAndroidTest --console=plain
```

Comando verificado en Windows: 56 JVM pasan (55 de dominio y una de plantilla), 17 avisos Lint y 0 errores. APK principal y de pruebas compilados. Ocho pruebas nuevas de persistencia y la instrumental de plantilla **no se han ejecutado en Android**. Para ejecutarlas, conectar un dispositivo de pruebas con depuración USB autorizada o un emulador compatible y usar:

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

## Adaptación F2027 de dominio — 06/10/2026

`domain.admission` prepara entradas nuevas con nombres separados, nacimiento/residencia,
actividad y establecimiento condicional, preferencias opcionales, uno a tres programas
priorizados y sedes independientes. `PrepareAdmission` recibe catálogos sintéticos,
consentimiento, reloj y zona del evento explícitos. Los canales y modalidades admitidos
se inyectan; no se inventan valores institucionales. Sin actividad ni opciones preseleccionadas.

Prepared no acredita autorización ni guardado. Room v1 y RoomCaptureRepository conservan
el contrato anterior `domain.capture`; **no conectar el formulario F2027 a ese repositorio**.
No convertir F2027 a CaptureInput descartando campos ni usar el validador antiguo para
altas F2027. La próxima adaptación requiere migración que preserve los registros antiguos
y un repositorio que guarde el contrato completo. La pantalla sigue siendo la plantilla.

26 pruebas JVM nuevas cubren F2027; las 30 anteriores siguen pasando. Sin nuevas
dependencias, migración ejecutada, pruebas instrumentadas ejecutadas ni RF cerrado.
La rama local depende del incremento previo de persistencia; revisión cruzada pendiente.
