# Especificación de requisitos de software

Proyecto Captura de Prospectos · Grupo 6 · Duoc UC

Revisión 1.5 · 22 de noviembre de 2026

## Ficha del documento

| Dato | Descripción |
|---|---|
| Proyecto | Captura de Prospectos |
| Documento | SRS — Especificación de requisitos de software |
| Asignatura | Desarrollo de Aplicaciones Móviles, DSY1105 |
| Institución | Duoc UC, Escuela de Informática y Telecomunicaciones, sede Viña del Mar |
| Integrantes | Sebastián Diaz, Jeremy Hanglin y Martín Caviedes |
| Revisión | 1.5, 22/11/2026 |
| Entrega de referencia | E2 y evaluación final; aplicación integrada 1.1.0 |
| Alcance documental | Requisitos del producto, reglas de datos, criterios de aceptación y límites de entrega |

| Fecha | Versión | Responsables | Cambio |
|---|---|---|---|
| 06/10/2026 | 1.4 | Grupo 6 | Campos y preferencias del formulario de admisión 2027. |
| 22/11/2026 | 1.5 | Grupo 6 | Acceso, permisos, ubicación, integración remota y límites de E2; edición formal y Word. Se conservan RF01–RF13 y RNF01–RNF09. |

## 1 Introducción

Captura de Prospectos permite registrar personas interesadas en estudiar en Duoc UC durante actividades de admisión en terreno. Buscamos reducir la transcripción de formularios en papel, validar la información durante su ingreso y conservar cada inscripción junto con intereses académicos, consentimiento y contexto, incluso sin conexión.

### 1.1 Propósito

En esta especificación definimos comportamiento esperado, restricciones y criterios de aceptación. El documento sirve de referencia para desarrollo, revisión y presentación académica. Conservamos los identificadores de requisitos anteriores para mantener trazabilidad, y distinguimos alcance previsto de funciones incluidas en la entrega.

### 1.2 Ámbito del sistema

La solución comprende aplicación Android, almacenamiento local cifrado y servicios propios de usuarios y prospectos. E1 proporciona el incremento local. E2 incorpora registro y autenticación remota, permisos por rol y sede, sincronización persistente, consulta externa de ubicación, perfil local y despliegue académico AWS con IP elástica y HTTPS.

Los requisitos amplios de consolidación, catálogo y administración remota se conservan para una ampliación posterior. Las fichas indican qué parte corresponde al incremento actual; una función aplazada no se presenta como implementada.

No se incluyen en esta entrega SSO Microsoft/Duoc, recuperación de contraseña, CRM comercial completo, portal web separado, llamadas o SMS automáticos, mapas de calor ni verificación gubernamental de identidad. Las demostraciones utilizan datos ficticios. Un piloto con personas reales requiere validar consentimiento, menores, retención y responsables institucionales de los datos.

### 1.3 Definiciones y abreviaturas

| Término | Significado |
|---|---|
| SRS | Especificación de requisitos de software. |
| RF y RNF | Requisito funcional y no funcional. |
| Prospecto e inscripción | Persona interesada y registro de su interacción en un evento; no equivale a matrícula. |
| Evento | Actividad de captación por sede, con período, zona horaria y ubicación declarada. Puede durar varios días. |
| Contexto | Usuario, sede, evento, dispositivo, instante y procedencia de la ubicación. |
| PII | Información que identifica o puede relacionarse con una persona. |
| ACK | Confirmación válida del servidor después de persistir una operación. |
| Idempotencia | Repetir una operación con la misma identidad sin duplicar su efecto. |
| Outbox | Cola local durable de operaciones por enviar. |
| E1 y E2 | Incremento local e incremento integrado del proyecto. |

### 1.4 Referencias

Las fuentes de requisitos son el caso de inscripción de potenciales clientes, el levantamiento de septiembre de 2026, el formulario de admisión 2027 y los acuerdos de alcance del proyecto. Los documentos siguientes complementan esta especificación. Sus rutas se expresan desde la raíz del repositorio público.

| Documento | Finalidad |
|---|---|
| docs/FORMULARIO_ADMISION_2027.md | Campos, obligatoriedad, preferencias y fuentes. |
| docs/ENTREGAS.md | Distribución original por entregas. |
| docs/ENTREGA_E2_1_1_0.md | Estado y límites vigentes del incremento 1.1.0. |
| docs/ARCHITECTURE.md y docs/DESIGN.md | Capas, estado y presentación. |
| docs/LOCAL_MODEL.md y docs/RECORD_CONTRACTS.md | Datos, revisiones y mutaciones locales. |
| docs/SECURITY.md | Consentimiento y protección. |
| docs/ACCOUNT_ACCESS_CONTRACT.md | Registro y administración de permisos. |
| docs/REMEMBERED_SESSION.md | Recuperación de sesión al reabrir. |
| docs/OFFLINE_SESSION_VALIDATION.md | Autorización para trabajo sin red. |
| docs/PROSPECT_SYNC_CONTRACT.md | Cola durable, idempotencia y ACK. |
| docs/EXTERNAL_GEOCODING_CONTRACT.md | Ubicación externa y privacidad. |
| docs/PROFILE_PERSONALIZATION.md | Apodo y fotografía por cuenta. |
| docs/openapi/backend.openapi.json | Operaciones implementadas de API. |
| docs/COVERAGE_VALIDATION.md | Cobertura y alcance de pruebas. |
| docs/RELEASE_DISTRIBUTION.md | Firma y distribución. |
| backend/deploy/ec2/README.md | Despliegue y persistencia en AWS. |

### 1.5 Organización del documento

El capítulo 2 describe usuarios, datos y restricciones. El capítulo 3 presenta interfaces y requisitos funcionales y no funcionales. El capítulo 4 relaciona funciones de la entrega con los requisitos, delimita ampliaciones y establece el control de cambios.

## 2 Descripción general

### 2.1 Perspectiva del producto

La aplicación acompaña al equipo de admisión en ferias, visitas y actividades de captación. El dispositivo conserva la información consentida como fuente de trabajo local. Los servicios reciben operaciones autorizadas y centralizan usuarios y prospectos. La falta de red no debe eliminar capturas confirmadas ni convertir un intento de envío en una confirmación del servidor.

### 2.2 Funciones del producto

El recorrido incluye acceso, selección o creación de contexto autorizado, formulario, validación, consentimiento y guardado. Después se consultan, editan o anulan fichas y se revisan estados de envío y resúmenes locales. Cámara y GPS proponen datos que requieren confirmación; la consulta externa describe la localidad del dispositivo. Perfil conserva apodo, fotografía y apariencia en esta instalación.

### 2.3 Usuarios y permisos

| Participante | Alcance |
|---|---|
| Administrador | Habilita solicitudes y gestiona estado, rol y sedes. Requiere conexión. |
| Captador | Prepara contexto y gestiona capturas propias en sus sedes autorizadas. |
| Supervisor | Opera en sus sedes autorizadas conforme a los permisos de API y las vistas disponibles. La consolidación completa está prevista. |
| Prospecto | Aporta datos y consentimiento; no tiene cuenta ni rol autenticado. |

Implementamos tres roles: ADMIN, CAPTADOR y SUPERVISOR. Una solicitud de registro queda pendiente hasta su habilitación. Los controles del servidor deben impedir el acceso no autorizado aunque se invoque directamente la API. La entrega temporal del formulario al prospecto se realiza bajo supervisión; un modo independiente que bloquee la navegación se mantiene previsto.

### 2.4 Restricciones generales

Utilizamos Kotlin, Jetpack Compose, Material 3 y MVVM, con Room/SQLCipher y Android Keystore en Android, y Retrofit para REST. El backend usa Java 21, Spring Boot y PostgreSQL desplegado. Android requiere API 26 o superior. El entorno local de desarrollo puede utilizar H2 persistente, sin confundirlo con PostgreSQL del despliegue.

No guardamos la contraseña del servidor en el teléfono. Los secretos de infraestructura, firma y administración quedan fuera de repositorios y adjuntos públicos. AWS depende de mantener la instancia activa: detenerla conserva sus volúmenes y la IP elástica, pero interrumpe el acceso hasta recuperar los servicios. No se declara alta disponibilidad.

### 2.5 Datos y reglas de negocio

| Dato | Regla |
|---|---|
| Nombres y apellidos | Dos campos obligatorios; trim 2–120 cada uno; permitir nombres compuestos y un apellido |
| RUT | Obligatorio; normalizar sin puntos y DV en mayúscula; validar dígito verificador; mostrar formato legible |
| Correo | Obligatorio, máximo 254; validación de estructura, sin SMTP/SMS; trim; no afirmar titularidad |
| Teléfono | Obligatorio; +56 visible/preseleccionado; aceptar ingreso con espacios y normalizar; regla base móvil chileno +56 9 y ocho dígitos adicionales |
| Actividad actual | 4° Medio / Egresado Enseñanza Media / Otra, obligatoria sin preselección; Otra exige descripción |
| Establecimiento | Obligatorio sólo en 4° Medio, opcional en egresado/otra actividad; 2–160 si se informa. Sugerencia de evento sólo confirmada y editable, nunca inferida del GPS |
| Fecha de nacimiento | Obligatoria, fecha real no futura; manual. QR sólo si su contenido se verifica |
| Comuna de residencia | Obligatoria, manual, 2–120 caracteres; no inferida |
| Comuna del establecimiento | Opcional cuando se informa establecimiento, manual 2–120 |
| Femenino / Masculino / No indica | Opcional, sin preselección; una respuesta ausente es distinta de seleccionar No indica |
| Jornada y nivel de interés | Una opción opcional por campo, sin preselección |
| Motivación | Una de las cuatro alternativas del cupón, opcional, sin texto libre adicional |
| Carreras de interés | Entre 1 y 3 programas únicos al confirmar, ordenados por prioridad 1..n; borrador consentido admite cero |
| Sede de interés | Cero o más sedes únicas, sin máximo adicional al catálogo; independientes del evento/carreras/jornada, sin promesa de disponibilidad |
| Modalidad y canal | Opcionales; no inferir preferencia si no se selecciona |
| Observaciones | Opcional; máximo 500; no utilizar para registrar información sensible adicional |
| Número de documento, imagen de cédula, QR crudo | No almacenar en la base inicial |
| Contexto | Captador, sede, evento, deviceId, capturedAt, zona horaria, ubicación con fuente, estado y versión |
| Consentimiento | Versión/texto disponible, aceptación y fecha, operador y modo de aceptación |

Las preferencias son independientes y no presuponen una oferta verificada por carrera, sede y jornada. La ubicación describe la captura, nunca el domicilio o establecimiento del prospecto. Los filtros diarios usan la fecha local del evento. Los intereses conservan referencias históricas aunque se retire una opción del catálogo.

El dominio Android centraliza los límites de la ficha. El servidor valida el DTO principal, RUT, propietario y sede del transporte implementado; la validación centralizada del catálogo F2027 y las asignaciones de eventos sigue prevista. Conservar el snapshot no equivale a validar remotamente todas sus reglas.

### 2.6 Suposiciones y dependencias

El primer acceso y la habilitación requieren servidor. La continuidad offline depende de una autorización local vigente del mismo usuario y arranque. Los recursos nativos dependen de permisos y disponibilidad. La localidad externa requiere internet y posición actual confirmada, pero una respuesta fallida no impide capturar. El prospecto no necesita una cuenta.

## 3 Requisitos específicos

### 3.1 Interfaces externas

#### 3.1.1 Interfaz de usuario

Bienvenida, acceso y registro, selección de contexto y secciones Inicio, Registros, Resumen y Perfil; gestión de cuentas para ADMIN. Los errores se muestran por campo o acción, sin depender solo del color. Los estados de carga y las animaciones deben permitir comprender el proceso sin bloquear innecesariamente la operación.

#### 3.1.2 Interfaces de hardware

Cámara para QR y fotografía de perfil, GPS para posición candidata y galería para imagen voluntaria. Conectividad informa la red y permite coordinar envíos. El rechazo de permisos ofrece continuidad manual o sin el recurso.

#### 3.1.3 Interfaces de software

Room/SQLCipher conserva la base local y Android Keystore protege claves. Retrofit consume api-gateway, users-service y prospects-service; OpenAPI y Swagger describen las operaciones implementadas. BigDataCloud utiliza cliente separado sin credenciales propias; Android Geocoder intenta una dirección aproximada de manera independiente.

#### 3.1.4 Interfaces de comunicación

El gateway desplegado usa HTTPS. Los puertos internos y PostgreSQL no se exponen a internet. Las solicitudes propias usan Bearer JWT cuando corresponde. Solo el desarrollo local admite HTTP de loopback o emulador configurado. El cliente externo no envía tokens ni datos de la ficha.

### 3.2 Requisitos funcionales

#### RF01 Autenticación y sesión contextualizada

El sistema debe autenticar cuentas individuales y aplicar rol y sedes autorizadas. El registro crea una solicitud pendiente; el administrador la habilita y asigna permisos. El primer login requiere servidor. El JWT remoto tiene vigencia de cuatro horas. Al reabrir, la aplicación recupera la sesión cifrada y comprueba identidad y vigencia, sin guardar la contraseña. Captador y supervisor pueden continuar localmente hasta doce horas desde el último login verificado, dentro del mismo arranque del teléfono; reiniciarlo exige conexión. El administrador requiere conexión. Cambiar de cuenta no reasigna ni expone registros anteriores.

El usuario puede personalizar apodo y fotografía desde cámara o galería, por cuenta y dispositivo. Inicio debe mostrar la misma fotografía del perfil.

Aceptación: acceso válido e inválido, cuenta pendiente, cambio de permisos, vencimiento, cierre de sesión, reapertura y reinicio no pierden pendientes ni permiten ver los de otra cuenta. Un token rechazado pausa envíos y exige autenticación. No extender autorización modificando solo el reloj. La personalización persiste sin mezclarse entre usuarios.

**Alcance E2:** Registro, administración, tres roles, sesión recordada y autorización offline incluidos. El perfil es local; SSO institucional, recuperación de contraseña, refresh y revocación inmediata universal quedan fuera del incremento.

#### RF02 Captura y validación

Formulario con campos de la sección 2.5; validadores fuera de UI y mensajes por campo. No guardar una inscripción completa con campos inválidos. Ingreso manual de RUT/nombre siempre disponible. No persistir PII del formulario antes del consentimiento; durante edición sin consentimiento mantener sólo memoria volátil, sin SavedState persistente ni logs.

Aceptación: entradas válidas, vacías, mal formadas y longitudes límite; conservar formulario ante error recuperable mientras la pantalla sigue activa; no guardar ni sincronizar datos no consentidos.

**Alcance E2:** Formulario F2027, validación y guardado consentido incluidos. El estado previo al consentimiento es volátil y no garantiza recuperación tras cierre del proceso.

#### RF03 Preferencias académicas

Buscador y lista alfabética de opciones; entre 1 y 3 programas únicos al confirmar, con orden de prioridad 1..n, resumen visible, reordenación y eliminación individual. La lista seleccionada se presenta por prioridad, no se reordena alfabéticamente perdiendo el ranking. Sede de interés y sede del evento son conceptos distintos; se permiten múltiples sedes opcionales sin máximo adicional al catálogo, como preferencias generales independientes de carreras/jornada. El mapa de oferta queda aplazado y no se presupone. No reutilizar las sedes autorizadas del captador como preferencia del prospecto. Cambiar contexto requiere revisar selección; conservar intereses históricos incluso si el catálogo los desactiva después.

Aceptación: buscar con/sin acentos; seleccionar 1, 2 o 3, rechazar cuarta/duplicada, reordenar y recuperar prioridades; no inventar ranking histórico. Cada carrera cuenta una vez por inscripción en métricas.

**Alcance E2:** Preferencias incluidas con catálogo local versionado. La oferta por sede y jornada no está consolidada ni se presume disponible.

#### RF04 Metadatos y ubicación

Confirmar ubicación del evento al iniciar el trabajo e intentar ubicación del dispositivo por captura. Registrar latitud y longitud sólo si existen; fuente `DEVICE` o `EVENT_CONFIRMED`, momento de medición y precisión si procede. Parámetros técnicos: espera máxima 30 s, posición del dispositivo con edad <=5 min y precisión <=100 m. Estos límites son decisiones técnicas del prototipo y no garantizan precisión catastral.

Si GPS falla, permitir contexto del evento explícitamente confirmado por captador; coordenada manual sólo si se conoce, con rango validado. Nunca usar (0,0) como ausencia. Sin GPS ni contexto válido: guardar con consentimiento como `NEEDS_CONTEXT`, sin enviar. Resolver luego con ubicación del evento identificada como tal; no retrofechar una medición posterior. La alternativa identifica su procedencia y requiere validación institucional antes de una operación real.

Aceptación: permiso denegado, ubicación aproximada, timeout y GPS apagado no causan cierre inesperado; datos y fuente visibles.

La consulta opcional de localidad utiliza BigDataCloud mediante una acción explícita después de confirmar una adquisición GPS nativa de hasta sesenta segundos. Antes de consultar se informa del envío de coordenadas e IP al proveedor. No se envían RUT, nombre, correo, teléfono ni credenciales propias; tampoco se consultan posiciones históricas o ubicaciones de eventos en el endpoint gratuito. El resultado muestra localidad, región y país cuando estén disponibles y permanece en memoria. No completa domicilio, comuna del prospecto ni establecimiento. Android Geocoder permite un intento separado de calle y número aproximados; si no existe respuesta se puede continuar.

Aceptación adicional: rechazar resultados por IP o para otra coordenada; mostrar fallos recuperables sin cerrar la sesión propia ni impedir guardar. No hacer consultas automáticas ni duplicar pulsaciones. La falta de calle o localidad no invalida la captura.

**Alcance E2:** GPS, fuente de contexto, localidad externa e intento de dirección incluidos. La etiqueta externa no se persiste ni modifica el contrato de sincronización. La fuente del evento no equivale a una medición GPS. El transporte actual admite fichas identificadas como NEEDS_CONTEXT; el bloqueo de envío hasta resolver ubicación, previsto en el requisito objetivo, permanece pendiente y no se considera cumplido por recibir ACK.

#### RF05 Consentimiento

Casilla desmarcada, texto offline versionado. Prospecto acepta directamente o captador registra su aceptación verbal explícita, distinguiendo los modos. Rechazo cancela y limpia lo digitado. Para demostración, texto rotulado como ficticio; no atribuir aprobación a Duoc. Política para menores y retiro formal queda pendiente para operación real; la demo usa personas sintéticas.

Aceptación: no existen filas personales persistidas/enviadas sin aceptación; versión y fecha viajan junto al registro; cancelar borra memoria del formulario.

**Alcance E2:** Consentimiento académico incluido. Políticas institucionales de menores, retiro y tratamiento real requieren definición antes de un piloto.

#### RF06 Persistencia local

Room con cifrado definido en SECURITY. Guardar registro y trabajo de envío en una transacción; confirmar éxito después del commit. Pendientes sobreviven cierre, reinicio y más de 24 h offline. Almacenamiento lleno o clave inaccesible produce error explícito, sin afirmar guardado ni reinicializar la base automáticamente.

Borrador incompleto: sólo con consentimiento previo y acción explícita; separado de inscripción, cifrado y recuperable por su autor. Sin envío ni conteo como prospecto válido. El recordatorio a las 24 h se mantiene previsto para una ampliación; la eliminación es deliberada, no automática.

Aceptación: crear, reiniciar y recuperar; error de escritura y aislamiento por usuario; ensayar 400 registros sin convertirlo en límite de captura.

**Alcance E2:** Room cifrado, borradores consentidos y outbox atómico incluidos. El recordatorio de borradores permanece previsto.

#### RF07 Consulta, edición y anulación

Bandeja propia con filtros/búsqueda locales. Editar pendientes o errores corregibles; preservar instantáneas y orden de operaciones si coincide una edición con el envío, sin perder la revisión posterior. Anulación confirmada con motivo mínimo y rastro sin duplicar PII en logs. El captador puede editar o anular sus registros, incluidos los sincronizados, de acuerdo con su estado y revisión esperada. El servidor revalida permisos; los conflictos requieren revisión y no sobrescritura silenciosa.

Duplicado local por RUT/evento: advertir y abrir registro propio existente. No crear otra inscripción activa ni mostrar datos de otro captador. El backend resuelve coincidencias entre dispositivos con conflicto explícito; la aplicación no promete detección global offline.

Aceptación: carrera de edición/envío, anulación, duplicados, corrección de rechazo y permisos.

**Alcance E2:** Bandeja, edición/anulación y transporte remoto incluidos. Fichas históricas y conflictos se conservan para revisión, sin conciliación automática ni descarga completa entre dispositivos.

#### RF08 Sincronización automática

Cola durable sobre Room, coordinador con conectividad y sesión autorizada, y reintentos con espera progresiva de cinco segundos a cinco minutos; datos móviles/Wi-Fi permitidos. Botón Reintentar reutiliza la cola, no ejecuta un segundo envío simultáneo. Servidor confirma individualmente UUID/versión y persiste antes de responder.

Timeout/red/5xx: reintentar el mismo contenido y clave; no crear duplicado. 401: detener y pedir autenticación. 403: intervención por permisos. 400/422: error por campo corregible. 409: conflicto de negocio o versión a resolver, no éxito implícito. Registrar diagnóstico sin datos personales. Conexión no garantiza API alcanzable ni ejecución inmediata del sistema operativo.

Aceptación: servidor acepta pero se pierde respuesta → un solo registro remoto después del reintento; restaurar cola tras reinicio; no marcar sincronizado antes de ACK.

La entrega ejecuta la coordinación con la aplicación abierta. El envío con la aplicación cerrada mediante WorkManager permanece previsto como ampliación, sin considerarse implementado.

**Alcance E2:** Outbox, idempotencia, ACK persistido y reintentos incluidos con la app abierta. WorkManager y conciliación completa de conflictos se mantienen aplazados.

#### RF09 Resumen del captador

Por evento y día/rango: inscripciones completas activas, enviadas, pendientes, errores, pendientes de contexto; anuladas e incompletas separadas. Personas distintas = RUT distintos dentro del filtro; inscripciones = registros persona/evento. Intereses = una selección por carrera/inscripción; sumatoria de intereses puede superar personas.

Preservar reporte tras limpieza de copias sincronizadas mediante resumen sin PII o consulta/caché agregada del servidor. No sumar agregados remotos a copias locales ya reconocidas como si fueran personas nuevas.

Aceptación: evento de varios días, persona en dos eventos, varias carreras, anulación y purga local no generan totales engañosos.

**Alcance E2:** Resumen calculado sobre datos locales de la instalación. Los agregados remotos y conservación de informes ante una futura purga quedan previstos.

#### RF10 Supervisión y correcciones

Supervisor consulta datos consolidados de sus sedes, último reporte por dispositivo y conflictos. Desconectado/sin reporte se muestra desconocido, no «0 pendientes». No puede forzar ejecución en otro teléfono desconectado. Puede solicitar revisión y verificar nuevos reportes; no vuelve a pedir datos al prospecto por una mera falla de red.

Correcciones remotas online con versión esperada y auditoría de actor, fecha, campos y motivo. Rechazar edición concurrente; no sobrescribir silenciosamente. RUT/evento erróneos requieren anulación/recreación supervisada y enlazada. Cierre con desconocidos es incompleto o excepcional con motivo; no borra pendientes.

Aceptación: dos captadores, un equipo offline, corrección concurrente y acceso a sede no autorizada.

**Alcance E2:** Rol y sedes aplicados en servidor, vistas locales y operaciones autorizadas de API. El tablero consolidado, reportes por dispositivo y cierre centralizado quedan previstos.

#### RF11 Asistencia por cámara

Leer códigos mediante cámara, mostrar campos reconocidos y requerir confirmación antes de rellenar; conservar entrada manual si se deniega permiso o formato no soportado. No abrir URLs del código ni enviar su contenido a terceros. Sin almacenamiento de imágenes o contenido crudo.

Aceptación académica: código sintético de formato documentado y pruebas de formato inválido, cancelación y cámara denegada. Objetivo de cédula real: requiere parser validado con especificación/muestra autorizada y campos realmente disponibles. Una demostración sintética no acredita ese objetivo. No inventar nombres ni afirmar validación de identidad. La extracción de nacimiento requiere formato y contenido verificados, candidato confirmado y alternativa manual; no se declara soporte real de cédula mediante el QR sintético.

**Alcance E2:** Cámara y QR sintético documentado incluidos. No se declara lectura de cédula real ni verificación de identidad.

#### RF12 Catálogo versionado

Catálogo con fuente Duoc, fecha de revisión y sede/año cuando estén verificados. El cupón recibido aporta nombres y secciones, sin mapa programa/sede/jornada: transcripción documental no es oferta activa. La evolución prevista permite actualizar desde el backend del equipo al abrir una jornada online; esta descarga queda aplazada respecto de E2. Importación revisada, no scraping dentro del teléfono ni supuesto acceso a API institucional. Actualización transaccional y fallback al último catálogo válido.

Aceptación: sin red, descarga inválida, versión nueva y retiro de carrera; preservar referencias históricas.

**Alcance E2:** Catálogo local versionado con diez programas y ocho sedes. Descarga y administración remota aplazadas.

#### RF13 Administración de eventos y asignaciones

Captador y supervisor pueden crear, editar y seleccionar eventos locales dentro de sus sedes autorizadas, con fechas y ubicación. El captador puede preparar su propio contexto. Cambiar el evento activo no modifica retrospectivamente el contexto de capturas anteriores. La administración y distribución remota de asignaciones se mantiene prevista para una ampliación. Cambio de evento limpia formulario sólo tras confirmación de descarte o guardado consentido.

Aceptación: límites de sede, evento multidía y cambio de asignación. La futura revocación remota de asignaciones deberá conservar para revisión los envíos posteriores de un captador que trabajó offline; su validación requiere el servicio remoto de eventos aún aplazado.

**Alcance E2:** Gestión local de eventos y contexto autorizados. Administración remota y distribución de asignaciones aplazadas; el servidor actual valida usuario y sede, sin un servicio remoto de eventos.

### 3.3 Requisitos no funcionales

#### RNF01 Material 3, legibilidad, animaciones funcionales

**Descripción:** La aplicación debe utilizar Material 3, texto legible y animaciones que comuniquen carga, guardado y transición sin obstaculizar el flujo.

Flujo completo objetivo <5 min; meta interna media <=3,5 min en 10 tareas ficticias documentadas; errores con texto/icono, no sólo color

**Alcance E2:** Compose y estados implementados. Las metas de tiempo y la validación exhaustiva de accesibilidad requieren medición específica; no se declaran acreditadas por pruebas de base de datos.

#### RNF02 Capacidad y rendimiento

**Descripción:** El almacenamiento debe admitir el conjunto de prueba de capacidad y completar transacciones locales sin bloquear la interfaz.

Conjunto de datos mínimo 400 capturas/dispositivo; meta técnica p95 <150 ms para transacción local, excluyendo GPS/red, con dispositivo y versión documentados; no bloquear UI

**Alcance E2:** Evidencia histórica en Xiaomi 11T/API 34: 400 registros cifrados y alta p95 42,98 ms. No mide el recorrido completo ni garantiza el mismo resultado en otros equipos.

#### RNF03 MVVM/UDF, negocio desacoplado

**Descripción:** La solución debe separar presentación, dominio y datos mediante MVVM y flujo unidireccional de estado; las reglas deben poder comprobarse aisladamente.

Composables sin acceso directo a DAO o REST ni validación de negocio; validadores comprobables y estado inmutable. La lógica evaluada debe alcanzar al menos 80 % de cobertura, con alcance y exclusiones explícitos.

**Alcance E2:** Arquitectura y suites incorporadas. Los informes separan lógica Android, presentación, código generado y cada servicio.

#### RNF04 REST Spring Boot/Retrofit, contratos versionados

**Descripción:** La comunicación debe utilizar contratos REST versionados con Spring Boot y Retrofit, documentando únicamente operaciones disponibles.

OpenAPI y tests de contrato antes de integración; HTTPS fuera de entorno local debug explícito

**Alcance E2:** Gateway, usuarios, prospectos, OpenAPI y Swagger por HTTPS incluidos. Las rutas futuras no se presentan como implementadas.

#### RNF05 Tolerancia a fallos

**Descripción:** Las capturas confirmadas y la cola pendiente deben sobrevivir fallas recuperables de red, cierres y reinicios sin reinicialización destructiva.

Preservar transacciones confirmadas en ensayos de reinicio; no prometer recuperación ante pérdida física o daño del almacenamiento

**Alcance E2:** Base local cifrada, cola persistente y volúmenes PostgreSQL; respaldo lógico diario y restauración aislada comprobada. No se declara alta disponibilidad ni recuperación sin respaldo ante pérdida física.

#### RNF06 Seguridad

**Descripción:** El sistema debe proteger datos personales y credenciales, restringir accesos y mantener los secretos fuera de los entregables públicos.

Cifrado local, roles en servidor, cero secretos de infraestructura en repositorios/APK, sólo datos de prueba ficticios; sin declaración de auditoría legal

**Alcance E2:** Controles del prototipo incluidos, sin atribuir auditoría legal, aprobación institucional ni seguridad absoluta.

#### RNF07 Cámara/GPS/conectividad

**Descripción:** Los recursos nativos deben ofrecer estados visibles y alternativas cuando un permiso o recurso no esté disponible.

Pruebas de permiso, indisponibilidad y respuesta visible en UI; no suponer que todos requieren diálogo de permiso

**Alcance E2:** Cámara, GPS y conectividad integrados. La prueba real de localidad se diferencia del uso de dobles HTTP.

#### RNF08 Android teléfono/tablet

**Descripción:** La aplicación debe ser utilizable en los dispositivos Android previstos, con orientación y tamaño de texto adaptables.

minSdk 26; orientación vertical/horizontal, texto aumentado y flujo sin recortes; documentar dispositivo y API de cada verificación.

**Alcance E2:** Pruebas en emulador API 34 y recorrido físico en Redmi 9A/API 29. La cobertura exhaustiva de tablet, accesibilidad y ejecución con páginas de 16 KB no se considera acreditada.

#### RNF09 Entregables y colaboración

**Descripción:** La entrega debe permitir localizar código, requisitos, ejecución, firma y evidencia de colaboración mediante artefactos versionados.

Historial real de los tres integrantes, ramas y PR, README, SRS versionado y APK release firmada. Identificar custodia privada de la clave y entrega académica. Trello queda fuera del alcance acordado.

**Alcance E2:** Código, documentación y candidata firmada 1.1.0 disponibles. El equipo realiza la entrega y activación individual en la plataforma según las instrucciones del docente.

## 4 Alcance de entrega y control de cambios

### 4.1 Trazabilidad del incremento integrado

| Función | Requisitos | Evidencia de referencia |
|---|---|---|
| Registro, permisos y sesión | RF01, RNF06 | Contratos de cuenta, sesión recordada y autorización offline, citados en 1.4. |
| Formulario y consentimiento | RF02, RF03, RF05 | Formulario de admisión y pruebas de dominio. |
| Ubicación, localidad y cámara | RF04, RF11, RNF07 | Contrato de geocodificación y evidencia de cámara/GPS. |
| Persistencia, edición y envío | RF06, RF07, RF08, RNF05 | Contrato de sincronización y OpenAPI. |
| Eventos y resumen locales | RF09, RF10, RF13 | Contratos de resumen y eventos; límites de la entrega 1.1.0. |
| Catálogo local | RF03, RF12 | Formulario de admisión 2027. |
| Arquitectura y cobertura | RNF03, RNF04 | Arquitectura e informes de cobertura. |
| Infraestructura y distribución | RNF05, RNF06, RNF09 | Guías de operación EC2 y distribución firmada. |

### 4.2 Límites y ampliaciones previstas

Priorizamos el recorrido completo de cuenta habilitada, captura consentida, guardado local, envío idempotente y persistencia remota, junto con consulta externa y APK firmada. La entrega no implica que todos los criterios del producto completo estén terminados.

Mantenemos para una ampliación posterior consolidación y descarga entre dispositivos, catálogo y asignaciones remotas, supervisión integral y cierre centralizado, conciliación de históricos/conflictos, sincronización con la app cerrada y recordatorios de borradores. La cédula real y el acceso institucional requieren fuentes y condiciones verificadas. El resumen y la personalización permanecen locales.

### 4.3 Verificación y control de cambios

Los resultados de construcción, pruebas, cobertura y recorrido manual se identifican en ENTREGA_E2_1_1_0.md y sus evidencias. Cada resultado se limita a la versión y entorno documentados; no reemplaza aceptación institucional ni acredita funciones aplazadas.

Las modificaciones posteriores conservan identificadores y registran motivo, impacto en datos, contratos y pruebas e historial de revisión. Markdown es la fuente editable del SRS y el documento Word corresponde a la misma revisión de entrega. La revisión 1.5 actualiza los acuerdos de E2 sin descartar requisitos amplios explícitamente aplazados.
