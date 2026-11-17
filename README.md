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

- **Acceso y registro real de usuarios:** crear cuentas con rol (`Captador` o `Supervisor`) y sedes asignadas, e iniciar sesión autenticando contra el backend (`POST /api/v1/auth/login` vía API Gateway con JWT RS256) o contra el almacenamiento local cifrado en modo offline.
- **Gestión real de eventos y contextos:** crear, editar y seleccionar eventos de captación por sede (tanto para supervisores como para captadores en sus sedes autorizadas), sin depender de eventos ficticios precargados.
- **Formulario de admisión F2027 con validaciones por campo:** registrar datos personales, RUT con dígito verificador (módulo 11), correo, teléfono, actividad actual, establecimiento y hasta tres carreras priorizadas del catálogo Admisión Duoc UC 2027.
- **Consentimiento y persistencia local cifrada + sincronización remota:** almacenar cada inscripción o borrador en SQLite/Room cifrado con SQLCipher y Android Keystore, y sincronizar automáticamente las inscripciones confirmadas, ediciones y anulaciones con `prospects-service` (PostgreSQL) a través del `api-gateway` mediante Retrofit, sin perder datos locales ante fallos de red.
- **Bandeja, detalle, edición y anulación:** consultar registros reales filtrados por rol/sede, estado y búsqueda textual, editar con control optimista de versión y anular con motivo auditado.
- **Recursos nativos integrados:** escanear códigos QR con CameraX y ML Kit para proponer el RUT, adquirir ubicación candidata por GPS (`LocationManager`) y observar el estado de conectividad en tiempo real (`ConnectivityManager`).
- **Resumen y métricas reales:** consultar gráficos y estadísticas calculados directamente desde los registros almacenados por evento y rango de fechas, y alternar entre modo claro y oscuro desde Perfil.

El recorrido habitual es **bienvenida → inicio de sesión o registro de cuenta → selección o creación de evento → captura con consentimiento y guardado → consulta de bandeja y resumen**. La información guardada permanece en el dispositivo al cerrar la sesión o reiniciar la aplicación.

## Alcance de las entregas

| Entrega | Enfoque |
|---|---|
| **E1 · aplicación local** | Formulario F2027, Room cifrado, bandeja de registros, eventos, métricas, cámara QR, GPS y conectividad real en dispositivo. |
| **E2 / E3 · aplicación integrada** | Eliminación de dependencias DEMO, catálogo real Duoc UC 2027, registro/autenticación real de usuarios, microservicios Spring Boot (`api-gateway`, `users-service`, `prospects-service`), PostgreSQL e integración móvil con Retrofit. |

Consulta los criterios completos en [Entregas](docs/ENTREGAS.md) y [SRS](docs/SRS.md).

## Uso de la aplicación

1. Abre la aplicación y pulsa **Comenzar**.
2. En la pantalla de acceso:
   - Si aún no tienes cuenta en el dispositivo, pulsa **¿No tienes cuenta? Crear cuenta**, ingresa tu nombre completo, selecciona tu rol (**Captador** o **Supervisor**), marca tus sedes asignadas, ingresa tu correo y contraseña y pulsa **Registrar y entrar**.
   - Si ya dispones de una cuenta registrada localmente o en el backend (`users-service`), ingresa tu correo electrónico y contraseña y pulsa **Iniciar sesión**.
3. En **Tu punto de partida**, selecciona un evento existente o pulsa **Nuevo evento** para registrar una nueva actividad en tu sede (puedes usar **Autocompletar jornada de hoy** para rellenar el horario de la jornada actual).
4. Accede a **Inicio**, **Registros**, **Resumen** y **Perfil** para capturar prospectos, editar o anular fichas y consultar estadísticas reales.

Para probar la cámara, muestra un [QR compatible o no compatible](docs/fixtures/qr/README.md) frente al escáner. El primero propone el RUT para confirmación del usuario; el segundo verifica el rechazo seguro y permite continuar con ingreso manual.

## Tecnologías y arquitectura

| Área | Tecnologías y responsabilidad |
|---|---|
| Interfaz | Kotlin, Jetpack Compose, Material 3 y animaciones de transición (`Crossfade`, `AnimatedVisibility`, `animateContentSize`). |
| Estado | ViewModel, StateFlow y Coroutines para estado inmutable y acciones unidireccionales (MVVM). |
| Dominio | Reglas de validación centralizadas, consentimiento, preferencias académicas y contratos en Kotlin. |
| Almacenamiento local | Room 2.8.5 sobre SQLite, con SQLCipher 4.19.1 para cifrado y Android Keystore para proteger la clave AES-256-GCM. |
| Comunicación remota | Retrofit 2.11.0 + OkHttp 4.12.0 + Gson hacia el `api-gateway` con autenticación Bearer JWT. |
| Backend y BD remota | Java 21, Spring Boot 3.5.16 (`api-gateway`, `users-service`, `prospects-service`), Spring Security OAuth2/JWT RS256 y PostgreSQL. |
| Cámara, GPS y red | CameraX + ML Kit Barcode Scanning (modelo incluido), `LocationManager` y `ConnectivityManager`. |
| Construcción y pruebas | Gradle Wrapper, Maven Wrapper, JUnit, AndroidX Test y pruebas de integración Spring Boot. |

El flujo principal en Android es **pantalla → ViewModel → repositorio → Room cifrado (+ sincronización Retrofit con API Gateway)**. La base de datos local SQLite/Room se conserva como fuente primaria offline-first para garantizar que ningún fallo de red provoque pérdida de capturas. [Más sobre la arquitectura](docs/ARCHITECTURE.md).

## Ejecutar desde el código

Para clonar el repositorio y ubicarse en la rama de trabajo `develop`:

```powershell
git clone https://github.com/MartinCaviedes/Captura-Prospectos-G6.git
cd Captura-Prospectos-G6
git checkout develop
```

### Android

Abre la carpeta **`android/`** en Android Studio, configura **SDK 37** y el **JDK 25** requerido por el daemon de Gradle, y selecciona la variante **`demoDebug`**.

Para compilar y ejecutar las pruebas unitarias desde la raíz del repositorio:

```powershell
cd android
.\gradlew.bat :app:testDemoDebugUnitTest :app:assembleDemoDebug --console=plain
```

La APK se genera en `android/app/build/outputs/apk/demo/debug/app-demo-debug.apk`. Más detalles en la [guía Android](android/README.md).

### Backend (`api-gateway`, `users-service`, `prospects-service`)

Los tres microservicios Spring Boot están en [`backend/`](backend/README.md):
- **`api-gateway` (`:8080`):** Punto de entrada único, validación JWT RS256, rate-limit de login y enrutamiento hacia usuarios y prospectos.
- **`users-service` (`:8081`):** Usuarios persistidos en JPA/PostgreSQL, contraseñas BCrypt y emisión de JWT RS256 (`/api/v1/auth/login`, `/api/v1/auth/logout`, `/api/v1/me`, `/api/v1/users`).
- **`prospects-service` (`:8082`):** Persistencia y CRUD de prospectos en PostgreSQL (`/api/v1/prospects`), control de acceso por rol/sede, unicidad activa `(rut, eventId)` y anulación auditada.

Para ejecutar las pruebas de los tres microservicios desde `backend/`:

```powershell
cd backend
.\mvnw.cmd test
```

Consulta la [guía del backend](backend/README.md) para levantar los servicios con PostgreSQL o H2 local y conectar el emulador Android (`http://10.0.2.2:8080/`).

## Organización del repositorio

```text
android/                  Aplicación Android (Kotlin, Jetpack Compose, Room/SQLCipher, Retrofit)
  app/src/main/           Dominio, datos locales, cliente remoto Retrofit, pantallas y recursos
  app/src/test/           Pruebas unitarias JVM
  app/src/androidTest/    Pruebas instrumentadas y de interfaz
  app/schemas/            Esquemas exportados de Room (v1 a v4)
backend/                  Microservicios Spring Boot (Java 21, Maven)
  api-gateway/            Gateway reactivo y validación JWT (:8080)
  users-service/          Servicio de usuarios, roles y autenticación JWT (:8081)
  prospects-service/      Servicio de prospectos y persistencia PostgreSQL (:8082)
docs/                     Requisitos, contratos, arquitectura, auditorías y evidencias
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
