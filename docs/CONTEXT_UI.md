# Componentes de entrada al contexto demo

Incremento E1-BASE-03-A1 del 08/10/2026, derivado de 978ca91. Aporta tema y pantalla presentacional para RF01 local/base RF13; no cierra estos RF. La actividad principal todavía muestra la plantilla: no se conecta la nueva pantalla al almacenamiento en este incremento.

## Qué está implementado

`ui/context/DemoContextScreen.kt` recibe DemoContextUiState y callbacks para preparar demo, reintentar lectura, seleccionar perfil/evento y salir. Representa carga, contexto sin inicializar, error de almacenamiento, listas vacías y selección recibida. Los callbacks transportan UUID; un clic no cambia por sí solo el estado ni afirma persistencia. Un supervisor no dispone de acciones de evento para capturar. La autorización real sigue perteneciendo al repositorio.

El tema adapta la paleta de [DESIGN](DESIGN.md) a claro/oscuro, sin color dinámico ni descarga de fuentes. Las previews usan estado ficticio y no abren Room. Los componentes muestran selección mediante texto y semántica, admiten desplazamiento y bloquean acciones durante una operación. No hay botón de formulario o envío ficticio.

## Comprobación

Build demoDebug y APK instrumental correctos; 58 pruebas JVM sin fallos; Lint 17 avisos/0 errores. Se añaden seis pruebas Compose sobre inicialización explícita, recuperación, callbacks, selección confirmada por estado, bloqueo de acciones y supervisor. Ejecución en Xiaomi 11T, Android 14/API 34: **OK (6 tests)**, 156,06 s, con seis resultados individuales correctos. MIUI bloqueó cada apertura en segundo plano: se inició explícitamente la actividad de prueba por ADB para permitir continuar al runner. No se modificaron permisos ni datos. Es ejecución asistida, no reproducción desatendida acreditada. [Salida del runner](evidencias/android-context-ui-2026-10-08.txt).

No se repitió la suite de persistencia: sus 28 pruebas aprobadas pertenecen al incremento A06. No sumar las seis nuevas a esas 28 como si se hubiera ejecutado una suite conjunta. Revisión visual de claro/oscuro, texto ampliado y TalkBack pendiente.

## Integración siguiente: E1-BASE-03-A2

1. Revisar y aceptar el contrato de contexto y A06 antes de conectar el consumidor; registrar revisor y base real.
2. Crear ViewModel con DemoContextRepository por constructor, StateFlow de sólo lectura y observación del contexto. Serializar acciones para evitar selecciones simultáneas; traducir ContextChange a mensajes, conservando los datos ante error.
3. Añadir Route con observación ligada al ciclo de vida. Abrir EncryptedDatabase fuera del hilo principal mediante un dueño con cierre definido; manejar fallo de apertura sin reset destructivo. No añadir inicialización automática.
4. Conectar MainActivity a esa Route. Mantener un único contenedor explícito y las dependencias mínimas compatibles, sin Hilt/Navigation por costumbre para una sola pantalla.
5. Probar inicialización explícita → perfil → evento → reapertura → cambio de perfil → salida; comprobar estado persistido, eventos autorizados y conservación de capturas. Probar fallo de apertura, cancelación y rechazo de selección. Un test con callbacks no sustituye este recorrido.

La capa visual no introduce datos personales, migraciones, backend ni cambios del contrato F2027. El formulario completo sigue siendo otro encargo.

### Reproducción de la comprobación asistida

Ejecutar sólo esta clase con el teléfono desbloqueado:

```text
adb shell am instrument -w -r -e class io.github.grupo6.capturaprospectos.ui.context.DemoContextScreenTest io.github.grupo6.capturaprospectos.demo.debug.test/androidx.test.runner.AndroidJUnitRunner
```

Si el registro de MIUI confirma bloqueo de lanzamiento y el runner espera el inicio de un caso, abrir en otra terminal:

```text
adb shell am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n io.github.grupo6.capturaprospectos.demo.debug/androidx.activity.ComponentActivity
```

Se usó esa apertura en los seis casos, sin saltar aserciones ni modificar tests. No repetir el comando durante interacciones activas. Los intentos anteriores detenidos por bloqueo no cuentan como pruebas aprobadas. La duración incluye esperas por apertura y no es una métrica de rendimiento de la app.
