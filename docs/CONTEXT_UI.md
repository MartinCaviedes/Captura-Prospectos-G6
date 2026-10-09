# Componentes de entrada al contexto demo

Estado vigente 09/10: [ronda 0](ROUND0_BASE.md) conecta el selector desde MainActivity con apertura cifrada y dueño explícito. Recorrido Compose/Room probado. Los apartados A1/A2 siguientes conservan evidencia histórica; sus pendientes de implementación ya no describen el HEAD actual. Revisión humana y visual aún pendientes.

Antecedente A2 del 09/10/2026: A2 incorpora ViewModel y Route, aún sin apertura de Room ni conexión desde MainActivity. El apartado A1 siguiente conserva el alcance y la evidencia visual del 08/10; no describe un recorrido integrado actual.

## A2: coordinación de estado y acciones

`DemoContextViewModel` recibe DemoContextRepository por constructor. Expone StateFlow de sólo lectura, observa sin inicializar y delega las acciones explícitas. Después de una acción consulta el snapshot confirmado. Mientras está ocupado descarta clics adicionales, sin encolarlos contra un perfil antiguo; cancela/espera la observación anterior antes de mutar y la reinicia al terminar. No deduce permisos, selecciona preferencias ni altera transporte.

Los resultados inválidos, almacenamiento no disponible y contexto incompatible producen mensajes; el reintento sólo lee y vuelve a observar, sin resembrar. La cancelación se propaga; al liberar el ViewModel se cancelan sus trabajos. La pantalla ofrece reintento ante errores de acción. No se exponen excepciones ni datos personales en mensajes.

`DemoContextRoute` recibe el ViewModel de su futuro dueño y usa `collectAsStateWithLifecycle` para entregar el estado a la pantalla. Esta observación UI sigue el ciclo de vida; la observación del repositorio vive durante el ViewModel. La fábrica y apertura cifrada todavía no están implementadas. El patrón se apoya en la [guía oficial de corrutinas y ciclo de vida](https://developer.android.com/topic/libraries/architecture/coroutines).

Dependencias explícitas: lifecycle-viewmodel-ktx y lifecycle-runtime-compose 2.9.4, alineadas con la versión ya resuelta transitivamente; lifecycle-runtime-ktx usa esa misma declaración. kotlinx-coroutines-test 1.10.2 sólo en pruebas. No se actualizan Compose/AGP/Room ni se añade Navigation/Hilt.

Comprobaciones del 09/10: **71 JVM aprobadas** (58 anteriores + 13 de ViewModel), sin fallos/errores/omitidas; build demoDebug e instrumental correctos; **Lint 19 avisos/0 errores**. Dos avisos adicionales notifican versiones más nuevas de las dependencias Lifecycle ahora explícitas; se mantiene la versión comprobada, sin ocultar los avisos. [Resumen de resultados y hashes de fuentes](evidencias/context-viewmodel-2026-10-09.json).

Los casos nuevos cubren lectura inicial sin mutación, preparación explícita, cambio de perfil, selección confirmada, clics repetidos, rechazo, incompatibilidad, fallo de almacenamiento, recuperación de observación, salida, cancelación, cierre y reemplazo del observador. Son pruebas con doble de repositorio, no pruebas de Room. No se ejecutó Android en este incremento ni se extrapolan las seis UI previas a la Route nueva. Una primera prueba falló por comprobar el estado inicial después de que runTest hubiera avanzado el planificador; se hizo explícito el avance del reloj y se conservaron las aserciones antes/después.

## A1: componente visual del 08/10

Incremento E1-BASE-03-A1 del 08/10/2026, derivado de 978ca91. Aporta tema y pantalla presentacional para RF01 local/base RF13; no cierra estos RF. La actividad principal todavía muestra la plantilla: no se conecta la nueva pantalla al almacenamiento en este incremento.

## Qué está implementado

`ui/context/DemoContextScreen.kt` recibe DemoContextUiState y callbacks para preparar demo, reintentar lectura, seleccionar perfil/evento y salir. Representa carga, contexto sin inicializar, error de almacenamiento, listas vacías y selección recibida. Los callbacks transportan UUID; un clic no cambia por sí solo el estado ni afirma persistencia. Un supervisor no dispone de acciones de evento para capturar. La autorización real sigue perteneciendo al repositorio.

El tema adapta la paleta de [DESIGN](DESIGN.md) a claro/oscuro, sin color dinámico ni descarga de fuentes. Las previews usan estado ficticio y no abren Room. Los componentes muestran selección mediante texto y semántica, admiten desplazamiento y bloquean acciones durante una operación. No hay botón de formulario o envío ficticio.

## Comprobación

Build demoDebug y APK instrumental correctos; 58 pruebas JVM sin fallos; Lint 17 avisos/0 errores. Se añaden seis pruebas Compose sobre inicialización explícita, recuperación, callbacks, selección confirmada por estado, bloqueo de acciones y supervisor. Ejecución en Xiaomi 11T, Android 14/API 34: **OK (6 tests)**, 156,06 s, con seis resultados individuales correctos. MIUI bloqueó cada apertura en segundo plano: se inició explícitamente la actividad de prueba por ADB para permitir continuar al runner. No se modificaron permisos ni datos. Es ejecución asistida, no reproducción desatendida acreditada. [Salida del runner](evidencias/android-context-ui-2026-10-08.txt).

No se repitió la suite de persistencia: sus 28 pruebas aprobadas pertenecen al incremento A06. No sumar las seis nuevas a esas 28 como si se hubiera ejecutado una suite conjunta. Revisión visual de claro/oscuro, texto ampliado y TalkBack pendiente.

## Plan de integración original de A2 (ejecutado después en ronda 0)

1. Revisar y aceptar el contrato de contexto y A06 antes de conectar el consumidor; registrar revisor y base real.
2. ViewModel preparado y probado con dobles el 09/10. Revisar su coordinación y completar la fábrica/dueño explícitos al integrar; no volver a implementarlo ni sustituir contratos.
3. Route preparada con observación ligada al ciclo de vida. Falta abrir EncryptedDatabase fuera del hilo principal mediante un dueño con cierre definido; manejar fallo de apertura sin reset destructivo. No añadir inicialización automática.
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
