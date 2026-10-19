# 📱 Captura de Prospectos — Grupo 6

**R4 · candidato local de giro del acceso 19/10/2026.** Visual XXL inicial comprobado; el operador detecta pérdida de credenciales al girar. Borrador retenido sólo en DemoAccessViewModel, sin almacenamiento. Regresión falla antes/pasa después; 191 JVM, 14 Android dirigidas y Lint 27 avisos/0 errores. APK `8a02596d…` instalada, repetición física pendiente antes de publicar. [QA vigente](docs/ROUND4_QA.md). Sin merge ni cierre R4.

**R4 · visual XXL comprobado 19/10/2026.** El operador completa el recorrido guiado en Xiaomi, fuente XXL, sin botones/textos cortados, ilegibles ni inaccesibles (APK `733104d7…`). Escala numérica no medida; [alcance y pendientes](docs/ROUND4_QA.md). Seguimiento documental, sin nueva APK/suites, merge ni cierre R4.

**R4 · acceso a Ajustes comprobado 19/10/2026.** El operador confirma «Abrir ajustes» desde Cámara denegada, ficha correcta y regreso tras conceder permiso sin reinicio ni descarte en ese intento (APK `733104d7…`). M01 guiado comprobado; no garantiza recuperación tras muerte de proceso. [QA vigente](docs/ROUND4_QA.md). Seguimiento documental, sin nueva APK ni suites; R4 mantiene pendientes y PR #10 sin merge.

**R4 · corrección de red comprobada 19/10/2026.** [QA actual](docs/ROUND4_QA.md): M04 detectó aviso offline que exigía cambiar de pestaña. Se conserva la escucha entre recomposiciones; regresión falla antes/pasa después. 191 JVM, ocho Android dirigidas y Lint 27 avisos/0 errores aprobados. APK debug `733104d7…`: el operador confirma pérdida/retorno sin cambiar pestañas, estimando hasta 1 s; observación informal. Corrección en rama R4 para revisión, sin merge. M05 pendiente por falta de red controlada. M03 QR físico válido/desconocido reportado en la APK anterior. No cierre R4 ni cambio de Room/permisos/E2.

**R4 · avance 18/10/2026.** [QA actual](docs/ROUND4_QA.md): regresión nueva en Xiaomi 11T/API 34 sobre APK `b0294e11…`, 52 pruebas de datos/proveedores y 31 de UI aprobadas en dos ejecuciones, con bases de prueba y conservación de la demo. 400 capturas cifradas, alta p95 42,98 ms. Comprobación estática ELF de 64 bits aprobada; release sigue sin firma. Sin cambio funcional ni cierre E1: quedan permisos/cámara/GPS/redes reales, TalkBack, tablet, aceptación contractual y decisiones de entrega. Los cortes inferiores son históricos.

**R4 · corte 14/10/2026.** R0–R3/V3 integradas en develop por PR #9 (`0c64533`). [QA vigente](docs/ROUND4_QA.md): construcción desde clon remoto limpio, 191 JVM aprobadas, Lint 27 avisos/0 errores; debug/instrumental firmadas y release compilada sin firma. No se ejecutó Android en este corte; resultados del 11/10 conservan su APK. La revisión técnica externa fue reportada al integrar; aceptación de contratos, TalkBack, tablet, permisos adversos, portal cautivo, firma/entrega y defensa siguen pendientes. Los cortes inferiores son antecedentes, no un cierre de E1.

Aplicación Android para registrar personas interesadas en estudiar en Duoc UC durante visitas a establecimientos, ferias y otras actividades de captación en terreno.

El proyecto nace del caso **Inscripción de potenciales clientes**, en la asignatura Desarrollo de Aplicaciones Móviles de Duoc UC Viña del Mar. Su objetivo es digitalizar la captura de datos e intereses académicos, reducir la transcripción manual y conservar registros con su consentimiento y contexto, incluso cuando no haya conexión.

## 🎯 Caso de uso

El recorrido E1 comienza con bienvenida, credenciales demo ficticias y selección explícita de evento. Luego registra los datos del prospecto, sus carreras de interés y preferencias, confirma el consentimiento y guarda la inscripción. La aplicación permitirá recuperar esos registros y consultar un resumen del trabajo realizado.

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

**Candidato E1 · V3 Conexiones, 11/10/2026; aceptación humana pendiente.** [Implementación y diferencias frente al prototipo](docs/UI_CONEXIONES_E1.md). Las evidencias de los incrementos inferiores conservan sus versiones.

Están implementados el dominio de captura F2027, las altas y lectura de detalle en Room cifrado, las migraciones conservadoras y el contexto/catálogo sintético persistido. La app presenta bienvenida y acceso demo con credenciales ficticias, seguido de selección de evento conectada a Room y menú Inicio/Registros/Resumen/Perfil. También están preparados el observador real de conectividad, el formulario volátil conectado al alta desde el panel, el parser QR sintético y las interfaces de GPS/registros. En esta rama están corregidos alta/hub y bandeja, con edición/anulación transaccional y auditoría candidata v4. Esta rama conecta cámara QR real y permisos/confirmación de ubicación; el usuario comprobó lectura física del QR y adquisición/confirmación/descarte de ubicación; los restantes recorridos de permisos siguen pendientes. Esta rama corrige la edición completa y conflictos del detalle. Esta rama corrige métricas locales y el acceso de supervisión; administración de eventos se corrige y verifica en esta rama. Los recorridos completos y la aceptación humana siguen pendientes. El backend se desarrollará en E2.

La revisión histórica R0/R1 del 10/10 registra **97 pruebas JVM aprobadas**, build correcto y Lint sin errores. Dos pruebas de formulario pasaron en Android 14/API 34, en tema claro normal y oscuro con letra al 150 %, con apertura asistida en MIUI. La evidencia anterior conserva las pruebas separadas de persistencia, selector y conectividad. No acredita recorridos pendientes ni accesibilidad completa; ningún RF semestral está cerrado. Consulta [la evidencia técnica](docs/ANDROID_BASELINE.md) para resultados y límites.

La corrección R2 alta/hub registra **109 JVM**, Lint 0 errores/20 avisos, 27 pruebas cifradas y cuatro UI aprobadas en Xiaomi, ejecutadas por separado y con asistencia de apertura MIUI para UI. [Alcance y límites](docs/ROUND2_FORM_HUB.md).

La corrección R2 de registros registra **121 JVM**, Lint 0 errores/20 avisos, 37 pruebas cifradas y dos UI aprobadas en Xiaomi en grupos separados. [Alcance y límites](docs/ROUND2_RECORDS_TRAY.md).

La corrección nativa registra **147 JVM**, Lint 0 errores/24 avisos, tres pruebas de decoder/proveedor y ocho UI aprobadas en Xiaomi. El decoder usa bitmaps y la UI dobles. El usuario comprobó cámara física y permiso preciso de ubicación; GPS agotó el plazo. El incremento de adquisición suma 155 JVM y consulta GPS/red; el usuario comprobó candidata de 12–100 m, confirmación y descarte. [Alcance y límites](docs/ROUND2_DEVICE_RESOURCES.md).

La corrección de detalle registra 165 JVM, Lint 0 errores/25 avisos, 39 cifradas y 11 UI en Xiaomi en grupos separados. [Alcance y límites](docs/ROUND3_RECORD_DETAIL.md).

La corrección de métricas registra 172 JVM, Lint sin errores, 43 cifradas y 9 UI en Xiaomi en grupos separados. [Alcance y límites](docs/ROUND3_HUB_METRICS.md).

El desarrollo y la demostración usan únicamente datos ficticios. Los perfiles demo permiten probar el alcance funcional local; la autenticación real pertenece a E2.

## 🚀 Ejecutar y explorar

Pulsa «Comenzar». Si falta contexto, «Preparar demo local» crea explícitamente perfiles, eventos y catálogo ficticios. Entra con `captador.a@g6.example`, `captador.b@g6.example` o `supervisor@g6.example` y contraseña ficticia `DemoG6!2026`; «Cuentas de muestra» se despliega y rellena ambos campos sin iniciar sesión automáticamente. Google/Microsoft conservan su representación visual deshabilitada, sin autenticación externa ni registro remoto. Elige un evento y continúa al inicio. Abrir la app no selecciona respuestas ni crea prospectos.

El menú permite entrar a Registros, Resumen y Perfil. «Nueva captura» abre el formulario; el consentimiento precede al guardado local explícito y el resultado puede ser DRAFT, NEEDS_CONTEXT o READY según el contrato. La cámara confirma sólo el RUT del QR ficticio; nombres/apellidos son manuales. La ubicación exige permiso y confirmación; la red no implica envío. Desde Perfil del captador puedes confirmar una carga opcional de 24 prospectos ficticios para explorar registros y gráficos. Los gráficos usan Room. Salir conserva los registros; un proceso nuevo exige acceso demo. [Recorrido y pruebas manuales de V3](docs/UI_CONEXIONES_E1.md).

Los resultados identificados de V3 se registran en [ANDROID_BASELINE](docs/ANDROID_BASELINE.md). El acceso y el giro comprobados por el usuario en el refinamiento previo son antecedentes; repetir sobre esta APK. TalkBack, tablet real, permisos adversos, portal cautivo y revisión humana siguen pendientes.

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

Administración: 185 JVM, 47 cifradas y 11 UI separadas en Xiaomi. [Alcance y pendientes](docs/ROUND3_EVENT_ADMIN.md).

La comprobación acumulada registra 187 JVM, Lint 0 errores/25 avisos, 51 de datos/nativos y 22 UI separados en Xiaomi. El ensayo de 400 usa repositorios reales y Room cifrado. [Método, mediciones y pendientes](docs/ROUND3_DATASET_400.md).
