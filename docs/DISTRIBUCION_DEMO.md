# Instalar y probar la demo E1

Versión preliminar de evaluación del 19/10/2026. APK Android demo/debug; no es una versión de producción ni acredita cierre de E1/R4. Se distribuye el mismo archivo instalado en el Xiaomi, sin modificar código, dependencias, permisos o datos.

- [Descargar APK](https://github.com/MartinCaviedes/Captura-Prospectos-G6/releases/download/e1-demo-2026-10-19/Captura-Prospectos-G6-E1-demo.apk)
- [Versión y archivos de prueba en GitHub](https://github.com/MartinCaviedes/Captura-Prospectos-G6/releases/tag/e1-demo-2026-10-19)
- [QR ficticio válido y QR no compatible](fixtures/qr/README.md)

## Instalación

Necesitas un teléfono o tablet con Android 8.0/API 26 o superior. La ejecución física comprobada corresponde al Xiaomi 11T con Android 14/API 34; no se afirma compatibilidad física ya probada con todas las tablets o dispositivos.

1. Descarga `Captura-Prospectos-G6-E1-demo.apk` desde los archivos de la versión; los ZIP de código fuente no son instaladores.
2. Abre el APK en Android. Si el sistema lo solicita, autoriza temporalmente Instalar aplicaciones desconocidas para el navegador o gestor de archivos utilizado.
3. Instala y abre Captura de Prospectos. No requiere Android Studio, depuración USB o conexión con un computador.
4. Puedes retirar después el permiso de instalación del navegador/gestor. Cámara y ubicación se solicitan sólo al usar sus funciones.

No se requiere desactivar Play Protect. Si Android bloquea la instalación por una política del dispositivo, anota el mensaje y revisa la causa. No desinstales una versión anterior para resolver incompatibilidad de firma sin considerar sus datos locales: la desinstalación los elimina. Esta demo usa el identificador `io.github.grupo6.capturaprospectos.demo.debug`; una futura release puede usar otro identificador o firma y no implica transferencia automática de la base.

## Acceso y registros de muestra

En Cuentas de muestra puedes rellenar los campos; después pulsa Ingresar. También puedes escribir estas credenciales ficticias públicas:

| Perfil | Correo | Contraseña de muestra |
|---|---|---|
| Captador A | captador.a@g6.example | DemoG6!2026 |
| Captador B | captador.b@g6.example | DemoG6!2026 |
| Supervisor | supervisor@g6.example | DemoG6!2026 |

Son identidades locales de demostración, no cuentas institucionales ni autenticación remota. Google/Microsoft no tienen autenticación real integrada en E1.

Para ver los gráficos: entra como Captador A, selecciona un evento habilitado, abre Perfil → Preparar datos ficticios y confirma. La muestra completa del contexto contiene 18 listas, 24 sin contexto, 12 borradores y 6 anuladas. El supervisor consulta los registros del mismo dispositivo según su alcance y no ve borradores de otros perfiles. Los totales pueden variar por contexto, registros anteriores o modificaciones manuales; la carga no reemplaza el trabajo existente ni duplica las mismas fixtures.

Las ubicaciones de las muestras son simuladas y se declaran en la confirmación; no prueban adquisición GPS ni presencia en un evento. Cada instalación mantiene su propia base Room cifrada. No recibe los prospectos del teléfono del equipo y E1 no sincroniza entre dispositivos. Usa sólo datos ficticios para la evaluación.

## Recorrido breve

1. Recorre bienvenida, acceso y selección de evento.
2. Revisa Inicio, Registros, Resumen y Perfil; cambia el tema desde Perfil.
3. Prepara las muestras voluntarias y revisa los estados del resumen.
4. Abre una captura, escribe datos ficticios y prueba los [dos QR](fixtures/qr/README.md) mostrándolos en otro equipo.
5. Si pruebas GPS, revisa la candidata y decide confirmar o descartar; no se aplica automáticamente. Un timeout no implica una ubicación confirmada.
6. Gira el teléfono y comprueba los campos; al terminar, descarta la captura sin guardar.

El soporte de QR de cédula real se aplaza a E2 por decisión de Martín del 19/10/2026; no forma parte de este instalador. Se conserva el QR sintético, confirmación y entrada manual.

## Identificación y evidencia

- APK SHA-256: `22d3e9876628f4eb076bee009845737b32f808fdbad670353219c98bb368ae49`.
- Código de la app: `ae68318e99f5ff342f96667544a174200c4ac1da`; el commit de la etiqueta sólo añade esta documentación, los PNG y el registro de distribución, sin cambios bajo `android/`.
- Compilación y Lint de esta APK: aprobados, 30 avisos/0 errores; firma y alineación ZIP verificadas. Instalación y bienvenida oscuro/vertical inspeccionadas en Xiaomi.
- 191 JVM/21 Android pertenecen al incremento anterior `1f5c8b2b`; no se presentan como suites nuevas de esta APK. [Evidencia específica](evidencias/bienvenida-captura-2026-10-19.json).
- Los PR R4 #10 e identidad #11 conservan su revisión/integración pendiente. Publicar esta preliminar no fusiona ramas ni sustituye aceptación contractual.
- TalkBack sigue aplazado; tablet físico y red sin internet/portal controlado continúan pendientes. No se afirma cierre general por distribuir la demo.

La APK se adjunta como archivo de GitHub Releases; no se incorpora el binario al historial del repositorio. Para informar un problema, indica versión, modelo/Android, pasos, resultado y captura sin datos personales. No adjuntes fotos de cédulas ni QR personales.
