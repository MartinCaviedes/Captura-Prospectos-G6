# Captura de Prospectos — Android

Base académica E1 del Grupo 6. Estado al 03/10/2026: base Compose compilada con primer dominio de captura y validadores Kotlin; pantalla todavía de plantilla, sin persistencia. Backend pendiente para E2.

## Abrir y compilar

Abrir esta carpeta android en Android Studio. Usar SDK Android plataforma 37 y JDK 25 para Gradle; el criterio está fijado en gradle/gradle-daemon-jvm.properties. Mantener wrapper Gradle 9.6.0 y versiones de gradle/libs.versions.toml. local.properties contiene la ubicación del SDK de cada equipo y no se comparte.

Seleccionar la variante demoDebug. Desde esta carpeta, con Java disponible en el entorno:

```powershell
.\gradlew.bat :app:assembleDemoDebug :app:testDemoDebugUnitTest :app:lintDemoDebug :app:assembleDemoDebugAndroidTest --console=plain
```

Comando ejecutado correctamente en Windows 11. Las 26 pruebas JVM pasan: 25 de dominio (consentimiento, contacto, intereses, estados y ubicación) y una de plantilla. No acreditan persistencia ni recursos nativos. Lint informa 15 avisos no bloqueantes. Las pruebas instrumentadas sólo se compilaron: requieren dispositivo/emulador para ejecutarse.

APK: app/build/outputs/apk/demo/debug/app-demo-debug.apk.
Reporte Lint: app/build/reports/lint-results-demoDebug.html.
Identidad de instalación demoDebug: io.github.grupo6.capturaprospectos.demo.debug.
Nombre visible: Captura de Prospectos; pantalla aún de ejemplo.

## Límites del incremento

La variante demo tiene identidad separada para evitar mezclar sus datos con integración futura. Backup y transferencia tienen exclusiones explícitas; su comportamiento en dispositivo sigue pendiente de prueba. Todavía no existen Room cifrado, formularios, GPS, cámara ni observador de red. Ningún servicio, secreto de nube o archivo privado del equipo es necesario para compilar esta base.

No versionar build, cachés, local.properties ni claves de firma. El APK debug no sustituye una entrega funcional ni un release de evaluación. Los siguientes cambios deben completar contexto/repositorios, persistencia cifrada y el primer recorrido recuperable con datos ficticios.

## Dominio implementado

Paquete domain.capture, Kotlin puro sin dependencias de Android. CaptureInput contiene entrada volátil; ContactValidator valida y normaliza contacto; LocationPolicy aplica los umbrales locales; PrepareCapture exige consentimiento y prepara DRAFT, NEEDS_CONTEXT o READY, siempre NOT_QUEUED.

Prepared no significa guardado. El futuro repositorio debe comprobar permisos/duplicados y confirmar la transacción; la UI no debe mostrar éxito de persistencia por recibir ese resultado. El preparador se usa para altas nuevas, no para revalidar ubicación histórica. Educación/canal se validan contra opciones suministradas, sin afirmar un catálogo institucional.

Última comprobación del incremento:

```powershell
.\gradlew.bat :app:testDemoDebugUnitTest :app:assembleDemoDebug :app:lintDemoDebug --console=plain
```

El comando de arriba se ejecutó con JDK 25 y terminó correctamente. Las pruebas instrumentadas se compilaron en el incremento anterior, pero no se han ejecutado en dispositivo.
