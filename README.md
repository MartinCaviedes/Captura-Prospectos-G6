# Captura de Prospectos — Grupo 6

Aplicación Android para registrar personas interesadas en estudiar en Duoc UC durante ferias, visitas a establecimientos y otras actividades de captación en terreno.

El proyecto aborda el caso **Inscripción de potenciales clientes**, de la asignatura Desarrollo de Aplicaciones Móviles. Reúne los datos de contacto, los intereses académicos y el contexto de cada inscripción para facilitar el trabajo del equipo de captación, incluso sin conexión a internet.

**[Descargar APK](https://github.com/MartinCaviedes/Captura-Prospectos-G6/releases/download/e1-demo-1.0.1/Captura-Prospectos-G6-E1-demo.apk)** · [Guía de instalación](docs/DISTRIBUCION_DEMO.md) · [QR para pruebas](docs/fixtures/qr/README.md) · [Documentación](docs/README.md)

## Caso de negocio

En una actividad de admisión se necesita vincular a cada persona interesada con sus preferencias de estudio y con el evento en que fue atendida. La aplicación busca reducir la transcripción manual, validar la información durante su ingreso y conservar registros que luego puedan revisarse y resumirse.

| Participante | Función en el proceso |
|---|---|
| Captador | Seleccionar un evento, registrar prospectos y revisar sus capturas y resultados. |
| Supervisor | Administrar eventos y asignaciones, consultar registros y revisar resultados dentro de su ámbito. |
| Prospecto | Proporcionar sus datos, intereses y consentimiento; no necesita una cuenta en la app. |

La sede del evento y las sedes preferidas para estudiar son datos distintos. Cada prospecto puede expresar **hasta tres carreras ordenadas por prioridad**, junto con las preferencias previstas en el formulario de admisión.

## Qué permite hacer

- Registrar datos personales, de contacto y educación con validaciones por campo.
- Seleccionar carreras y preferencias académicas, y revisar el resumen antes de guardar.
- Confirmar el consentimiento y almacenar la inscripción en una base local cifrada.
- Consultar, editar y anular registros, conservando el contexto y la auditoría de las operaciones.
- Leer un QR ficticio mediante la cámara y confirmar el RUT propuesto; nombres y apellidos se completan manualmente.
- Adquirir una ubicación candidata por GPS y decidir si se aplica; informar el estado de conectividad.
- Consultar gráficos y métricas locales por evento y período, y administrar eventos según el perfil.
- Cambiar entre modo claro y oscuro desde Perfil.

El recorrido habitual es **acceso → selección de evento → captura → consentimiento y guardado → consulta de registros y resultados**. La información guardada permanece en el dispositivo al cerrar la sesión.

## Alcance de las entregas

| Entrega | Enfoque |
|---|---|
| **E1 · aplicación local** | Perfiles de muestra, formulario, Room cifrado, registros, eventos, métricas, cámara, GPS y conectividad real. Funciona sin backend. |
| **E2 · aplicación integrada** | Autenticación remota, servicios Spring Boot, PostgreSQL, sincronización y supervisión consolidada. El soporte de cédula real requiere validar su formato. |

La versión disponible corresponde a **E1**. El acceso es una demostración local; Google y Microsoft no tienen autenticación integrada. La red disponible no implica que los registros se hayan enviado: E1 no sincroniza entre teléfonos. El catálogo y los datos de evaluación son ficticios. Consulta los criterios completos en [Entregas](docs/ENTREGAS.md) y [SRS](docs/SRS.md).

## Descargar e instalar la demo E1

Necesitas **Android 8.0 o superior**. La APK de evaluación se descarga gratuitamente desde [GitHub Releases](https://github.com/MartinCaviedes/Captura-Prospectos-G6/releases/tag/e1-demo-1.0.1), sin iniciar sesión en GitHub ni usar Google Play.

1. [Descarga Captura-Prospectos-G6-E1-demo.apk](https://github.com/MartinCaviedes/Captura-Prospectos-G6/releases/download/e1-demo-1.0.1/Captura-Prospectos-G6-E1-demo.apk) en tu teléfono y abre el archivo. Los ZIP de código fuente no son instaladores.
2. Si Android lo solicita, permite instalar aplicaciones desde el navegador o gestor de archivos utilizado y completa la instalación.
3. Abre la app y pulsa **Comenzar**. En **Cuentas de muestra**, elige un perfil para rellenar las credenciales y después pulsa **Ingresar**.
4. Selecciona un evento para acceder a Inicio, Registros, Resumen y Perfil.

También puedes ingresar manualmente con estas cuentas. Todas usan la contraseña ficticia **`DemoG6!2026`**:

| Perfil | Correo de muestra |
|---|---|
| Captador A | `captador.a@g6.example` |
| Captador B | `captador.b@g6.example` |
| Supervisor | `supervisor@g6.example` |

### Explorar con datos y QR de prueba

Como captador, abre **Perfil → Preparar datos ficticios** y confirma la carga para explorar registros y gráficos. La muestra completa por contexto incluye **18 listas, 24 sin contexto, 12 borradores y 6 anuladas**. Los totales pueden variar si ya existen registros o se han modificado las muestras; cada teléfono conserva su propia base de datos.

Para probar la cámara, muestra el [QR ficticio válido o el QR no compatible](docs/fixtures/qr/README.md) en otra pantalla, o imprímelos. El primero propone un RUT que debes confirmar; el segundo comprueba el rechazo de un formato no admitido y permite continuar manualmente. Descarta el formulario al terminar la prueba.

La [guía de distribución](docs/DISTRIBUCION_DEMO.md) contiene el recorrido detallado, las características del instalador y sus verificaciones. Utiliza únicamente datos ficticios para la evaluación.

## Tecnologías y arquitectura

| Área | Tecnologías y responsabilidad |
|---|---|
| Interfaz | Kotlin, Jetpack Compose y Material 3 para pantallas, componentes y temas. |
| Estado | ViewModel, StateFlow y coroutines para acciones y estado observable. |
| Dominio | Reglas de validación, consentimiento, preferencias y contratos en Kotlin. |
| Almacenamiento | Room sobre SQLite, con SQLCipher para cifrado y Android Keystore para proteger la clave. |
| Cámara y QR | CameraX y ML Kit, con el modelo de lectura incluido en la APK. |
| Ubicación y red | LocationManager y ConnectivityManager para recursos reales de Android. |
| Construcción y pruebas | Gradle Wrapper, JUnit, AndroidX Test y pruebas de Compose. |

El flujo principal es **pantalla → ViewModel → repositorio → Room**. Las pantallas presentan el estado y envían acciones; el dominio define las reglas y los repositorios coordinan la persistencia. Los prospectos se guardan en una base del almacenamiento privado de la app; la preferencia de apariencia se conserva por separado. [Más sobre la arquitectura](docs/ARCHITECTURE.md).

## Ejecutar desde el código

Para una copia nueva del proyecto:

```powershell
git clone https://github.com/MartinCaviedes/Captura-Prospectos-G6.git
cd Captura-Prospectos-G6
```

Abre la carpeta **`android/`** en Android Studio, configura **SDK 37** y el **JDK 25** requerido por el daemon de Gradle, y selecciona la variante **`demoDebug`**. Ejecuta la app con Run sobre un teléfono o emulador compatible. El proyecto incluye su Gradle Wrapper y no requiere servicios de nube para funcionar localmente.

Para compilar y ejecutar los controles JVM y Lint, desde la raíz del repositorio:

```powershell
cd android
.\gradlew.bat :app:testDemoDebugUnitTest :app:assembleDemoDebug :app:lintDemoDebug --console=plain
```

La APK se genera en `android/app/build/outputs/apk/demo/debug/app-demo-debug.apk`, tomando como referencia la raíz del repositorio. Las pruebas instrumentadas necesitan un dispositivo o emulador; su preparación, los comandos y las versiones fijadas están en la [guía Android](android/README.md).

## Cómo continuar el desarrollo

1. Define una tarea acotada, su requisito, la entrega a la que pertenece y los criterios que debe cumplir. Consulta el [SRS](docs/SRS.md), [Entregas](docs/ENTREGAS.md) y los contratos del área.
2. Comprueba el estado de tu clon y conserva cualquier trabajo local. Actualiza `develop` mediante avance rápido y crea una rama `feature/`, `fix/`, `docs/` o `chore/` según la tarea.
3. Implementa el cambio respetando las capas existentes. Coordina antes de modificar navegación, dependencias, permisos, esquema Room o contratos compartidos.
4. Ejecuta controles proporcionales al cambio y actualiza la documentación que corresponda.
5. Publica la rama y abre un PR hacia `develop` para revisión de otro integrante. Los incrementos comprobados se promueven de `develop` a `main` mediante PR.

La [guía de contribución](CONTRIBUTING.md) explica los comandos y convenciones. Conserva autores e historial y utiliza datos ficticios; los secretos y la documentación operativa privada quedan fuera de este repositorio.

## Organización del repositorio

```text
android/                  Proyecto Android y Gradle Wrapper
  app/src/main/           Dominio, datos, pantallas y recursos
  app/src/test/           Pruebas JVM
  app/src/androidTest/    Pruebas instrumentadas y de interfaz
  app/schemas/            Esquemas Room para migraciones
docs/                     Requisitos, contratos, guías y evidencias
  fixtures/qr/            Imágenes QR para probar el escáner
.github/                  Plantillas de contribución
```

## Documentación del proyecto

| Para consultar | Documento |
|---|---|
| Requisitos y alcance por entrega | [SRS](docs/SRS.md) · [Entregas](docs/ENTREGAS.md) |
| Campos, carreras y preferencias | [Formulario de admisión](docs/FORMULARIO_ADMISION_2027.md) |
| Capas y decisiones técnicas | [Arquitectura](docs/ARCHITECTURE.md) · [Diseño](docs/DESIGN.md) |
| Datos, contexto y protección | [Modelo local](docs/LOCAL_MODEL.md) · [Contexto demo](docs/DEMO_CONTEXT.md) · [Seguridad](docs/SECURITY.md) |
| Construcción y pruebas | [Guía Android](android/README.md) · [Evidencia técnica](docs/ANDROID_BASELINE.md) · [QA y pendientes](docs/ROUND4_QA.md) |
| Historial y documentación completa | [Changelog](docs/CHANGELOG.md) · [Índice](docs/README.md) |

## Equipo

Grupo 6: [Martín Caviedes](https://github.com/MartinCaviedes), [Jeremy Hanglin](https://github.com/JeremyDylan122) y [Sebastián Diaz](https://github.com/Sebastianidm).

Proyecto académico de Desarrollo de Aplicaciones Móviles, Duoc UC Viña del Mar.
