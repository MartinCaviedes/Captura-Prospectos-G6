# Captura de Prospectos — Grupo 6

Aplicación Android para registrar personas interesadas en estudiar en Duoc UC durante visitas a establecimientos, ferias y otras actividades de captación en terreno.

El proyecto nace del caso **Inscripción de potenciales clientes**, en la asignatura Desarrollo de Aplicaciones Móviles de Duoc UC Viña del Mar. Su objetivo es digitalizar la captura de datos e intereses académicos, reducir la transcripción manual y conservar registros con su consentimiento y contexto, incluso cuando no haya conexión.

## ¿Cómo se utilizará?

El recorrido previsto comienza con el captador seleccionando su perfil y evento. Luego registra los datos del prospecto, sus carreras de interés y preferencias, confirma el consentimiento y guarda la inscripción. La aplicación permitirá recuperar esos registros y consultar un resumen del trabajo realizado.

| Participante | Función prevista |
|---|---|
| Captador | Registrar prospectos en sus eventos, revisar capturas propias y consultar resultados |
| Supervisor | Administrar eventos/asignaciones y revisar datos dentro de su ámbito de sede |
| Prospecto | Proporcionar sus datos, preferencias y consentimiento, sin crear una cuenta |

La sede donde se realiza el evento y las sedes preferidas por el prospecto se tratan como conceptos distintos. Los intereses académicos se registran con **hasta tres carreras ordenadas por prioridad**.

## Funcionalidades del producto

- Captura de datos de contacto y educación, con validación y errores por campo.
- Selección de carreras, prioridades y preferencias opcionales de sede, jornada e interés.
- Consentimiento explícito y versionado antes de persistir datos personales.
- Almacenamiento local cifrado, consulta y recuperación del trabajo guardado.
- GPS para el contexto de ubicación, cámara para asistencia de ingreso y detección de conectividad.
- Eventos, asignaciones y resúmenes para captadores y supervisores.
- Autenticación y sincronización con servicios remotos en la segunda etapa.

Estas funcionalidades describen el alcance del producto; el estado implementado se indica más abajo. El catálogo de demostración es ficticio y no acredita disponibilidad institucional de una carrera por sede o jornada. La asistencia por cámara utilizará inicialmente un código sintético documentado; el soporte de cédula real requiere verificar su formato.

## Desarrollo en dos entregas

| Etapa | Resultado previsto |
|---|---|
| **E1 · aplicación local** | Recorrido Android con perfiles demo, captura consentida, persistencia cifrada, consulta y recursos GPS/cámara/conectividad |
| **E2 · aplicación integrada** | Continuidad de E1 con identidad remota, servicios Spring Boot, PostgreSQL, sincronización, supervisión consolidada y API externa |

La división mantiene los requisitos finales y los controles de calidad. E1 corresponde a P2/P3 y E2 a P4/P5; el detalle de criterios y las aclaraciones académicas pendientes están en [Entregas](docs/ENTREGAS.md).

## Estado actual

**Base técnica en desarrollo, documentación actualizada al 07/10/2026.**

Están implementados el dominio de captura F2027, las altas y lectura de detalle en Room cifrado, las migraciones conservadoras y el contexto/catálogo sintético persistido. La pantalla sigue siendo la plantilla Compose: todavía faltan navegación, formulario, recorridos completos, administración y recursos nativos. El backend se desarrollará en E2.

La evidencia del 06/10 sobre la implementación `2298220` registra **58 pruebas JVM y 24 pruebas instrumentadas aprobadas** en Android 14/API 34. Son pruebas de dominio, almacenamiento y contexto; no acreditan las funcionalidades visuales pendientes. Ningún RF semestral está cerrado. Consulta [la evidencia técnica](docs/ANDROID_BASELINE.md) para resultados y límites.

El desarrollo y la demostración usan únicamente datos ficticios. Los perfiles demo permiten probar el alcance funcional local; la autenticación real pertenece a E2.

## Ejecutar y explorar

Abrir `android/` en Android Studio y seleccionar `demoDebug`. La [guía Android](android/README.md) describe el JDK/SDK, las versiones fijadas y los comandos para construir y probar esta base. No se necesita infraestructura de nube para el build local.

Tecnologías actuales: **Kotlin, Jetpack Compose, Material 3, Room, SQLCipher y Android Keystore**, con negocio separado de la UI. La [arquitectura](docs/ARCHITECTURE.md) explica la evolución hacia MVVM y servicios remotos.

- [Requisitos del producto](docs/SRS.md)
- [Contrato de datos local](docs/LOCAL_MODEL.md) y [contexto demo](docs/DEMO_CONTEXT.md)
- [Campos y preferencias F2027](docs/FORMULARIO_ADMISION_2027.md)
- [Índice de documentación](docs/README.md)
- [Guía de contribución y revisión](CONTRIBUTING.md)

## Equipo

Grupo 6: [Martín Caviedes](https://github.com/MartinCaviedes), [Jeremy Hanglin](https://github.com/JeremyDylan122) y [Sebastián Diaz](https://github.com/Sebastianidm). Proyecto académico para el caso Duoc UC, con supervisión externa reportada de THERIONLABS.
