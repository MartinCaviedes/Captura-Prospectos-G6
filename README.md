# 📱 Captura de Prospectos — Grupo 6

Aplicación Android para registrar personas interesadas en estudiar en Duoc UC durante visitas a establecimientos, ferias y otras actividades de captación en terreno.

El proyecto nace del caso **Inscripción de potenciales clientes**, en la asignatura Desarrollo de Aplicaciones Móviles de Duoc UC Viña del Mar. Su objetivo es digitalizar la captura de datos e intereses académicos, reducir la transcripción manual y conservar registros con su consentimiento y contexto, incluso cuando no haya conexión.

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

Están implementados el dominio de captura F2027, las altas y lectura de detalle en Room cifrado, las migraciones conservadoras y el contexto/catálogo sintético persistido. La app abre un selector de perfiles/eventos conectado a Room. También están preparados el observador real de conectividad, el formulario volátil conectado al alta desde el panel, el parser QR sintético y las interfaces de GPS/registros. En esta rama están corregidos alta/hub y bandeja, con edición/anulación transaccional y auditoría candidata v4. Esta rama conecta cámara QR real y permisos/confirmación de ubicación; el usuario comprobó lectura física del QR y adquisición/confirmación/descarte de ubicación; los restantes recorridos de permisos siguen pendientes. Esta rama corrige la edición completa y conflictos del detalle. Esta rama corrige métricas locales y el acceso de supervisión; administración se revisa en su rama dependiente. Los recorridos completos y la aceptación humana siguen pendientes. El backend se desarrollará en E2.

La revisión histórica R0/R1 del 10/10 registra **97 pruebas JVM aprobadas**, build correcto y Lint sin errores. Dos pruebas de formulario pasaron en Android 14/API 34, en tema claro normal y oscuro con letra al 150 %, con apertura asistida en MIUI. La evidencia anterior conserva las pruebas separadas de persistencia, selector y conectividad. No acredita recorridos pendientes ni accesibilidad completa; ningún RF semestral está cerrado. Consulta [la evidencia técnica](docs/ANDROID_BASELINE.md) para resultados y límites.

La corrección R2 alta/hub registra **109 JVM**, Lint 0 errores/20 avisos, 27 pruebas cifradas y cuatro UI aprobadas en Xiaomi, ejecutadas por separado y con asistencia de apertura MIUI para UI. [Alcance y límites](docs/ROUND2_FORM_HUB.md).

La corrección R2 de registros registra **121 JVM**, Lint 0 errores/20 avisos, 37 pruebas cifradas y dos UI aprobadas en Xiaomi en grupos separados. [Alcance y límites](docs/ROUND2_RECORDS_TRAY.md).

La corrección nativa registra **147 JVM**, Lint 0 errores/24 avisos, tres pruebas de decoder/proveedor y ocho UI aprobadas en Xiaomi. El decoder usa bitmaps y la UI dobles. El usuario comprobó cámara física y permiso preciso de ubicación; GPS agotó el plazo. El incremento de adquisición suma 155 JVM y consulta GPS/red; el usuario comprobó candidata de 12–100 m, confirmación y descarte. [Alcance y límites](docs/ROUND2_DEVICE_RESOURCES.md).

La corrección de detalle registra 165 JVM, Lint 0 errores/25 avisos, 39 cifradas y 11 UI en Xiaomi en grupos separados. [Alcance y límites](docs/ROUND3_RECORD_DETAIL.md).

La corrección de métricas registra 172 JVM, Lint sin errores, 43 cifradas y 9 UI en Xiaomi en grupos separados. [Alcance y límites](docs/ROUND3_HUB_METRICS.md).

El desarrollo y la demostración usan únicamente datos ficticios. Los perfiles demo permiten probar el alcance funcional local; la autenticación real pertenece a E2.

## 🚀 Ejecutar y explorar

Al ejecutar esta versión se verá el selector demo. «Preparar demo local» crea explícitamente perfiles, eventos y catálogo ficticios; después se puede seleccionar perfil/evento y salir de la selección. Abrir la app no selecciona respuestas ni crea prospectos. Tras seleccionar perfil captador y evento se abre el panel; «Nueva Captura de Prospecto» abre el formulario. Al volver al selector con contexto activo, «Ir al panel de captura» retorna al panel. El consentimiento precede al guardado local explícito; el resultado puede ser DRAFT, NEEDS_CONTEXT o READY según el contrato. La cámara solicita permiso para QR ficticio y confirma sólo RUT; nombres/apellidos se completan manualmente. La ubicación requiere permiso y confirmación del candidato o del evento. La red no implica envío remoto.

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

Esta rama utiliza Room v4 y migraciones 1→2→3→4; la última añade auditoría sin inventar atributos históricos. El selector conserva contexto y catálogo ficticios. El formulario está conectado al alta; la bandeja permite consulta paginada y anulación local. El repositorio implementa edición F2027; su UI pertenece al incremento de detalle. Un error de clave o migración se comunica sin borrar la base.

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
