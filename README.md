# 📱 Captura de Prospectos — Grupo 6

Aplicación Android para registrar personas interesadas en estudiar en Duoc UC durante visitas a establecimientos, ferias y otras actividades de captación en terreno.

El proyecto nace del caso **Inscripción de potenciales clientes**, en la asignatura Desarrollo de Aplicaciones Móviles de Duoc UC Viña del Mar. Su objetivo es digitalizar la captura de datos e intereses académicos, reducir la transcripción manual y conservar registros con su consentimiento y contexto, incluso cuando no haya conexión.

## Descargar e instalar la demo E1

La [versión preliminar de evaluación](https://github.com/MartinCaviedes/Captura-Prospectos-G6/releases/tag/e1-demo-2026-10-19) se distribuye gratuitamente como APK para **Android 8.0 o superior**, sin Google Play. Puedes descargarla sin iniciar sesión en GitHub.

1. **[Descarga Captura-Prospectos-G6-E1-demo.apk](https://github.com/MartinCaviedes/Captura-Prospectos-G6/releases/download/e1-demo-2026-10-19/Captura-Prospectos-G6-E1-demo.apk)** desde tu teléfono Android y abre el archivo. Los ZIP de código fuente no son instaladores.
2. Si Android lo solicita, permite Instalar aplicaciones desconocidas para el navegador o gestor de archivos utilizado e instala la app. No necesitas Android Studio ni depuración USB.
3. Pulsa Comenzar y usa Cuentas de muestra → Captador A para rellenar los campos; después pulsa Ingresar. También puedes escribir `captador.a@g6.example` y la contraseña ficticia `DemoG6!2026`.
4. Elige un evento. Para ver registros y gráficos, abre Perfil → Preparar datos ficticios, revisa el aviso y confirma. La muestra completa por contexto incluye 18 listas, 24 sin contexto, 12 borradores y 6 anuladas; los totales pueden variar si ya existen datos o modificaciones.
5. Prueba el escáner con el [QR ficticio válido y el QR no compatible](https://github.com/MartinCaviedes/Captura-Prospectos-G6/tree/e1-demo-2026-10-19/docs/fixtures/qr), mostrados en otra pantalla o impresos. El válido aplica sólo el RUT tras confirmar; el no compatible debe permitir continuar manualmente sin borrar campos.

Consulta la **[guía completa de instalación y pruebas](https://github.com/MartinCaviedes/Captura-Prospectos-G6/blob/e1-demo-2026-10-19/docs/DISTRIBUCION_DEMO.md)** para el acceso como Captador B/Supervisor, los límites y los checksums.

Esta descarga es una demo/debug firmada para desarrollo, con datos ficticios y almacenamiento local por dispositivo, sin sincronización E1. La APK corresponde a la etiqueta enlazada; no se afirma que el código de `main` ya incluya los PR posteriores pendientes. El soporte de cédula real queda aplazado a E2. No desinstales una versión anterior sin considerar sus datos locales.

## 🎯 Caso de uso

El recorrido previsto comienza con el captador seleccionando su perfil y evento. Luego registra los datos del prospecto, sus carreras de interés y preferencias, confirma el consentimiento y guarda la inscripción. La aplicación permitirá recuperar esos registros y consultar un resumen del trabajo realizado.

| Participante | Función prevista |
|---|---|
| Captador | Registrar prospectos en sus eventos, revisar capturas propias y consultar resultados |
| Supervisor | Administrar eventos/asignaciones y revisar datos dentro de su ámbito de sede |
| Prospecto | Proporcionar sus datos, preferencias y consentimiento, sin crear una cuenta |

La sede donde se realiza el evento y las sedes preferidas por el prospecto se tratan como conceptos distintos. Los intereses académicos se registran con **hasta tres carreras ordenadas por prioridad**.

## ✨ Funcionalidades del producto

- Captura de datos de contacto y educación, con validación y errores por campo.
- Selección de carreras, prioridades y preferencias opcionales de sede, jornada e interés.
- Consentimiento explícito y versionado antes de persistir datos personales.
- Almacenamiento local cifrado, consulta y recuperación del trabajo guardado.
- GPS para el contexto de ubicación, cámara para asistencia de ingreso y detección de conectividad.
- Eventos, asignaciones y resúmenes para captadores y supervisores.
- Autenticación y sincronización con servicios remotos en la segunda etapa.

Estas funcionalidades describen el alcance del producto; el estado implementado se indica más abajo. El catálogo de demostración es ficticio y no acredita disponibilidad institucional de una carrera por sede o jornada. La asistencia por cámara utilizará inicialmente un código sintético documentado; el soporte de cédula real requiere verificar su formato.

## 🗓️ Desarrollo en dos entregas

| Etapa | Resultado previsto |
|---|---|
| **E1 · aplicación local** | Recorrido Android con perfiles demo, captura consentida, persistencia cifrada, consulta y recursos GPS/cámara/conectividad |
| **E2 · aplicación integrada** | Continuidad de E1 con identidad remota, servicios Spring Boot, PostgreSQL, sincronización, supervisión consolidada y API externa |

La división mantiene los requisitos finales y los controles de calidad. E1 corresponde a P2/P3 y E2 a P4/P5; el detalle de criterios y las aclaraciones académicas pendientes están en [Entregas](docs/ENTREGAS.md).

## 🚧 Estado actual

**Base local en desarrollo, actualizada al 10/10/2026.**

Están implementados el dominio de captura F2027, las altas y lectura de detalle en Room cifrado, las migraciones conservadoras y el contexto/catálogo sintético persistido. La app abre un selector de perfiles/eventos conectado a Room. También están preparados el observador real de conectividad, el formulario volátil (aún fuera del recorrido principal), el parser QR sintético y las interfaces de GPS/registros. Faltan conectar el alta al formulario, cámara/GPS reales, bandeja, edición/anulación, administración y recorridos completos. El backend se desarrollará en E2.

La revisión del 10/10 registra **97 pruebas JVM aprobadas**, build correcto y Lint sin errores. Dos pruebas de formulario pasaron en Android 14/API 34, en tema claro normal y oscuro con letra al 150 %, con apertura asistida en MIUI. La evidencia anterior conserva las pruebas separadas de persistencia, selector y conectividad. No acredita recorridos pendientes ni accesibilidad completa; ningún RF semestral está cerrado. Consulta [la evidencia técnica](docs/ANDROID_BASELINE.md) para resultados y límites.

El desarrollo y la demostración usan únicamente datos ficticios. Los perfiles demo permiten probar el alcance funcional local; la autenticación real pertenece a E2.

## 🚀 Ejecutar y explorar

Al ejecutar esta versión se verá el selector demo. «Preparar demo local» crea explícitamente perfiles, eventos y catálogo ficticios; después se puede seleccionar perfil/evento y salir de la selección. Abrir la app no selecciona respuestas ni crea prospectos. El formulario y el guardado de inscripciones todavía no están conectados a ese recorrido.

Abrir `android/` en Android Studio y seleccionar `demoDebug`. La [guía Android](android/README.md) describe el JDK/SDK, las versiones fijadas y los comandos para construir y probar esta base. No se necesita infraestructura de nube para el build local.

## 🛠️ Tecnologías y arquitectura

| Capa | Tecnología | Responsabilidad actual |
|---|---|---|
| Interfaz | Jetpack Compose · Material 3 | Pantallas declarativas, componentes y tema inspirado en el kit digital Duoc UC |
| Estado | ViewModel · StateFlow | Estado observable y acciones de pantalla |
| Dominio | Kotlin | Validación, consentimiento, prioridades y contratos independientes de Android |
| Persistencia | Room · SQLite · SQLCipher | Consultas, transacciones, migraciones y base local cifrada |
| Protección de claves | Android Keystore | Protección de la clave que permite abrir la base |
| Conectividad | ConnectivityManager | Estado real de la red; no confirma disponibilidad del futuro backend |
| Construcción y pruebas | Gradle · JUnit · AndroidX Test | Build, reglas de negocio, instrumentación y pruebas Compose |

La UI delega acciones al ViewModel y las operaciones de datos a repositorios. El dominio define reglas y contratos; las implementaciones Room resuelven la persistencia. [Arquitectura y convenciones](docs/ARCHITECTURE.md).

### 💾 ¿Dónde se guardan los datos?

**La base de datos local ya existe.** Al abrir la sesión, la app instancia Room y abre `captures-demo.db` mediante SQLCipher en su almacenamiento privado. La contraseña de cifrado se genera en el dispositivo y se guarda envuelta con una clave de Android Keystore. No requiere crear una cuenta de nube ni configurar credenciales de servidor.

Room utiliza el esquema v3 y migraciones 1→2→3. El selector conserva contexto y catálogo ficticios. El repositorio de admisiones ya permite guardar y recuperar inscripciones consentidas; conectar ese repositorio al formulario es el siguiente incremento. Un error de clave o migración se comunica sin borrar la base.

En E2 se incorporarán servicios remotos y sincronización. La base del teléfono seguirá permitiendo trabajar sin conexión; observar una red disponible no significa que un registro se haya sincronizado.

## 🗂️ Organización del repositorio

```text
android/                  Proyecto Android y Gradle wrapper
  app/src/main/           Dominio, persistencia y pantalla inicial
  app/src/test/           Pruebas de reglas en JVM
  app/src/androidTest/    Pruebas de almacenamiento, contexto, red y UI
  app/schemas/            Esquemas Room conservados para migraciones
docs/                     Requisitos, contratos y evidencia técnica
.github/                  Plantillas de contribución
```

## 📚 Documentación

Para comprender el proyecto, comienza por esta portada y Entregas. Para programar una tarea, consulta SRS y el contrato correspondiente. Para reproducir resultados, utiliza la guía Android y ANDROID_BASELINE. Los documentos de servicios remotos describen la segunda etapa; no son requisitos de instalación de esta versión.

- [Requisitos del producto](docs/SRS.md)
- [Contrato de datos local](docs/LOCAL_MODEL.md) y [contexto demo](docs/DEMO_CONTEXT.md)
- [Campos y preferencias F2027](docs/FORMULARIO_ADMISION_2027.md)
- [Índice de documentación](docs/README.md)
- [Guía de contribución y revisión](CONTRIBUTING.md)

## 👥 Equipo

Grupo 6: [Martín Caviedes](https://github.com/MartinCaviedes), [Jeremy Hanglin](https://github.com/JeremyDylan122) y [Sebastián Diaz](https://github.com/Sebastianidm). Proyecto académico para el caso Duoc UC, con supervisión externa reportada de THERIONLABS.
