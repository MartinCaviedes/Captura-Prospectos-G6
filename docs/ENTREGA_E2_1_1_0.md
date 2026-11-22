# Entrega E2 · candidata 1.1.0

22/11/2026. Este documento reúne el estado del incremento acordado para E2/examen. Los documentos fechados de implementación conservan sus resultados históricos; este resumen y la revalidación final de cobertura identifican el candidato actual. No supone aceptación docente ni cierre de todos los requisitos del SRS.

## Ejecución y distribución

- [APK y archivos de la candidata](https://github.com/MartinCaviedes/Captura-Prospectos-G6/releases/tag/e2-1.1.0-rc1).
- Gateway: `https://13.58.183.187/`. [Swagger](https://13.58.183.187/docs/index.html) y [OpenAPI](https://13.58.183.187/docs/openapi/backend.openapi.json).
- Android: versión `1.1.0`, código `3`, applicationId `io.github.grupo6.capturaprospectos.demo`, minSdk 26. Es una APK release no depurable, firmada con la clave del grupo y probada en Redmi 9A. [Firma e instalación](RELEASE_DISTRIBUTION.md).
- AWS Ohio (`us-east-2`): una EC2 `m7i-flex.large`, tres microservicios en contenedores y PostgreSQL 17.12 con bases/roles separados. IP elástica, HTTPS, discos cifrados y datos en volumen persistente. Puertos públicos 80/443; los puertos internos no se publican. [Operación y respaldos](../backend/deploy/ec2/README.md).

El servicio depende de mantener la instancia encendida. Se comprobó un ciclo real de detener/encender: misma IP, datos y claves conservados, servicios y temporizadores iniciados automáticamente, login y Swagger recuperados. Hay respaldos lógicos diarios y una instantánea inicial completada; no se declara una política automática de instantáneas ni alta disponibilidad. Detener EC2 reduce cómputo, pero discos, instantáneas e IP elástica siguen generando cargos.

## Funciones incluidas

| Flujo | Comportamiento del candidato |
|---|---|
| Registro y permisos | Solicitud PENDING; ADMIN habilita y asigna rol/sedes. CAPTADOR, SUPERVISOR y ADMIN tienen permisos diferenciados. |
| Acceso | Login remoto con JWT RS256 de cuatro horas; recuperación cifrada de sesión y comprobación de identidad al reabrir. Reiniciar el teléfono o vencer la sesión exige nuevo login. |
| Captura | F2027, validación de campos, consentimiento, guardado local cifrado, bandeja, edición/anulación y resumen local. |
| Sin conexión | Autorización operacional local de hasta doce horas para captador/supervisor; guarda capturas y operaciones pendientes. ADMIN requiere conexión. |
| Sincronización | Outbox durable, idempotencia, ACK persistido y reintentos al recuperar conexión con la app abierta y sesión autorizada. |
| Recursos nativos | Cámara/QR sintético documentado, GPS real y observación de conectividad. No se declara lectura de cédula real. |
| API externa | Consulta opcional de localidad a BigDataCloud desde GPS confirmado; intento separado de dirección aproximada con Android Geocoder, con alternativa cuando no entrega calle. |
| Perfil | Apodo y foto desde cámara/galería, conservados localmente por cuenta; foto visible también en Inicio. |
| Documentación de API | OpenAPI unificado y Swagger UI servido por el gateway, con operaciones autenticadas. |

Existen tres captadores y dos supervisores habilitados para presentación, además del administrador y las cuentas de prueba creadas por el responsable. Las contraseñas se entregan por canal privado; no se incluyen en el repositorio, la APK ni los adjuntos públicos.

## Evidencia del candidato

| Verificación final | Resultado |
|---|---|
| Backend, Java 21 / Maven / JaCoCo | 60 tests, cero fallos/errores |
| Android JVM | 268 tests, cero fallos/errores |
| Android API 34 en emulador | 141 instrumentados autónomos + 2 con gateway aislado, todos aprobados |
| Líneas cubiertas de cada microservicio | Gateway 98,28 %; usuarios 84,35 %; prospectos 98,00 % |
| Lógica Android | 87,10 %; [métrica y alcance completo](COVERAGE_VALIDATION.md) |
| Release | Build/Lint aprobados; cero errores y 42 advertencias; firma v2/v3 y alineación verificadas |
| PostgreSQL | Exportación/restauración lógica en entorno temporal aislado con conteos coincidentes |
| Redmi 9A, reporte de Martín | Flujo debug completo de AWS aprobado; capturas online/offline, recuperación del envío y sesión/datos al reabrir. Smoke adicional de la release firmada aprobado. |

La instrumentación mide la variante debug, no la release. La confirmación del teléfono es evidencia manual reportada, no una suite automatizada ni una medición del tiempo de sincronización. La presentación Android y el código generado se informan por separado. Los informes públicos de cobertura no contienen credenciales de los entornos de prueba.

## Preparación de la demostración

1. Encender EC2 con anticipación y comprobar que Swagger responde por HTTPS.
2. Abrir la release instalada, entrar con una cuenta habilitada y seleccionar/crear un evento de su sede.
3. Crear una captura ficticia y mostrar «Sincronizado con el servidor».
4. Desconectar internet, guardar otra captura, recuperar conexión con la app abierta y mostrar su confirmación.
5. Mostrar edición/anulación, resumen local, perfil y administración de permisos según el rol.

Para demostrar el registro, utilizar un correo ficticio nuevo y aprobar la solicitud con ADMIN. Para un recorrido breve, usar directamente las cuentas de presentación. No mostrar archivos de contraseñas, claves o tokens en una grabación de pantalla.

## Alcance aplazado y revisión

Por decisión de Martín se prioriza el incremento fundamental y se aplazan las funciones amplias del SRS: administración remota completa de eventos/catálogo/asignaciones, consolidación/descarga entre dispositivos y conciliación completa de fichas históricas/conflictos. El resumen y la personalización continúan siendo locales. La cola no se ejecuta con la aplicación cerrada; no se incorporó WorkManager. No hay refresh ni revocación inmediata universal de JWT. Microsoft/Duoc institucional y recuperación de contraseña no están integrados. El QR de cédula real sigue siendo una mejora opcional.

Trello queda fuera de la entrega acordada. No se fuerza un cuarto rol sin función real. Estas decisiones de alcance del equipo no sustituyen las aclaraciones que correspondan al profesor sobre las pautas.

La candidata se publica para revisión. La integración a develop y la promoción a main requieren revisión del equipo; no se atribuye una aprobación de código a las pruebas manuales. La custodia de la firma y los accesos de AWS corresponde a Martín, sin copiar secretos al repositorio privado.
