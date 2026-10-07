# Especificación de requisitos de software

Adaptación F2027 acordada: [FORMULARIO_ADMISION_2027](FORMULARIO_ADMISION_2027.md) registra fuente, respuestas y aclaraciones confirmadas por Martín el 06/10. F01–F03 definen reglas objetivo; dominio/persistencia F2027 implementados, con evidencia Android v2; contexto v3 probado en Android; UI pendiente de verificación funcional. Nacimiento desde QR se conserva condicionado a verificar formato, no como función comprobada.

La distribución vigente por hito está en [ENTREGAS.md](ENTREGAS.md): E1 (P2/P3) implementa el incremento local con GPS, cámara y conectividad; E2 (P4/P5) completa la integración. Los criterios finales de este SRS se conservan. No confundir una tarea E1 completada con un RF semestral terminado.

Versión 1.4, 06/10/2026: campos, obligatoriedad, sugerencias y preferencias F2027 acordados; conserva las demás reglas y distribución en dos entregas sin reducción del producto. Grupo 6: Sebastián Diaz, Jeremy Hanglin, Martin Caviedes. Cliente: Duoc UC; supervisión externa: THERIONLABS. Nombre de trabajo definido el 03/10: Captura de Prospectos; no implica marca institucional.

Estado: revisión de trabajo basada en el SRS 1.0 y las consultas de requisitos respondidas. No equivale a aprobación institucional. AC = acuerdo comunicado por Martín; PT = propuesta de esta revisión para prototipo, pendiente de revisión del grupo. Los originales se preservan. Véase `CHANGELOG.md`.

## 1. Propósito, alcance y fuentes

Digitalizar capturas de prospectos en terreno con Android y backend Spring Boot, reemplazando papel y transcripción. El producto incluye RF01–RF13 y los RNF siguientes, desarrollados en dos entregas según ENTREGAS. P2 objetivo: 19/10/2026, 19:00–21:20, con P3 el mismo día; fecha de P4/P5 pendiente. Se conservan GPS, cámara y detección de conectividad desde E1. AC14/AC15 sustituyen la asignación temporal de AC05, sin eliminar requisitos finales. Todo uso académico actual será con datos ficticios; un futuro piloto real requiere validación institucional adicional.

Fuentes: caso universitario, entrevista 24/09/2026, P2/P3, P4/P5 aportadas posteriormente, SRS 1.0 y respuestas D01–D36 de las consultas de requisitos de 29/09; distribución actualizada por instrucción de Martín del 02/10. El equipo aprobó internamente SRS 1.0. Las aclaraciones del docente reportadas por Martín sobre integrantes y entrega sustituyen las recomendaciones genéricas anteriores en esos puntos. P2/P4 gobiernan evidencia de cada entrega; P3/P5 complementan con dominio individual.

No se incluyen llamadas/SMS automáticos, CRM comercial completo, mapas de calor implementados, SSO Duoc, verificación de identidad gubernamental, portal web separado ni despliegue productivo. Guardar datos para una futura visualización geográfica no implica construir ese sistema ahora.

## 2. Usuarios y contexto

- Captador: cuenta individual, eventos asignados, capturas y estadísticas propias. Puede entregar temporalmente el formulario al prospecto bajo modo restringido (PT12).
- Supervisor de sede: eventos, asignaciones, estados y datos de captadores de las sedes autorizadas; correcciones y auditoría. Acceso verificado por servidor, no sólo escondiendo botones (PT08).
- Prospecto: aporta datos y aceptación; no se crea una cuenta para él.
- Evento: pertenece a una sede, tiene inicio/fin, zona horaria y ubicación declarada, y puede durar varios días. Una jornada de reporte se filtra por fecha local dentro del evento.

## 3. Datos y reglas transversales

| Dato | Regla de trabajo | Origen/estado |
|---|---|---|
| Nombres y apellidos | Dos campos obligatorios; trim 2–120 cada uno; permitir nombres compuestos y un apellido | Q01, acuerdo de equipo reportado; límites técnicos |
| RUT | Obligatorio; normalizar sin puntos y DV en mayúscula; validar dígito verificador; mostrar formato legible | D10/PT02; no acredita identidad |
| Correo | Obligatorio, máximo 254; validación de estructura, sin SMTP/SMS; trim; no afirmar titularidad | D10/PT |
| Teléfono | Obligatorio; +56 visible/preseleccionado; aceptar ingreso con espacios y normalizar; regla base móvil chileno +56 9 y ocho dígitos adicionales | Entrevista; propuesta internacional D11 aún no incorporada |
| Actividad actual | 4° Medio / Egresado Enseñanza Media / Otra, obligatoria sin preselección; Otra exige descripción | Q02, acuerdo de equipo reportado |
| Establecimiento | Obligatorio sólo en 4° Medio, opcional en egresado/otra actividad; 2–160 si se informa. Sugerencia de evento sólo confirmada y editable, nunca inferida del GPS | Q03 y aclaración 06/10 |
| Fecha de nacimiento | Obligatoria, fecha real no futura; manual. QR sólo si su contenido se verifica | Q04 + F04 |
| Comuna de residencia | Obligatoria, manual, 2–120 caracteres; no inferida | Q04/Q07 |
| Comuna del establecimiento | Opcional cuando se informa establecimiento, manual 2–120 | Q04/Q07 |
| Femenino / Masculino / No indica | Opcional, sin preselección; etiqueta institucional no informada; no respondido distinto de No indica | Q04 |
| Jornada y nivel de interés | Una opción opcional por campo, sin preselección | Aclaraciones 1–2 aprobadas 06/10 |
| Motivación | Una de las cuatro alternativas del cupón, opcional, sin texto libre adicional | Q06 + confirmación 06/10 |
| Carreras de interés | Entre 1 y 3 programas únicos al confirmar, ordenados por prioridad 1..n; borrador consentido admite cero | F2027 p.2 + F01 aprobado por Martín el 05/10 |
| Sede de interés | Cero o más sedes únicas, sin máximo adicional al catálogo; independientes del evento/carreras/jornada, sin promesa de disponibilidad | Q08/Q09 + aclaraciones 3–4 del 06/10 |
| Modalidad y canal | Opcionales; no inferir preferencia si no se selecciona | D10, propuesta |
| Observaciones | Opcional; máximo 500; no utilizar para registrar información sensible adicional | Caso/D10; límite PT |
| Número de documento, imagen de cédula, QR crudo | No almacenar en la base inicial | PT03; necesidad no confirmada |
| Contexto | Captador, sede, evento, deviceId, capturedAt, zona horaria, ubicación con fuente, estado y versión | RF01/RF04/RF08 |
| Consentimiento | Versión/texto disponible, aceptación y fecha, operador y modo de aceptación | RF05/PT |

Los límites técnicos se centralizan y se aplican también en backend. Modalidad opcional distinta de jornada; en E1 las preferencias son independientes y no se exige un cruce de ofertas programa/sede/jornada que aún no se ha verificado (F03). El cupón recibido aporta nombres de programas y sedes, pero no oferta por sede/jornada. Su transcripción documental no habilita automáticamente ofertas; ver FORMULARIO_ADMISION_2027.

## 4. Requisitos funcionales

### RF01. Autenticación y sesión contextualizada

Cuentas del prototipo con roles; primer login requiere servidor. Evento elegido entre asignaciones autorizadas; conservar su contexto al guardar. Sesión offline propuesta de hasta 12 h desde la última validación exitosa. Al vencer, conservar trabajo y requerir reautenticación; no extender vigencia modificando sólo el reloj. Cambio de usuario no reasigna ni expone registros anteriores. No almacenar contraseña del servidor en el teléfono.

Aceptación: login válido/inválido, cambio de rol, vencimiento offline y logout con pendientes no pierden datos ni permiten ver los de otro usuario. Un token rechazado por servidor pausa envíos; renovar o reautenticar antes de reintentar. D16/D17; PT01.

### RF02. Captura y validación

Formulario con campos de sección 3; validadores fuera de UI y mensajes por campo. No guardar una inscripción completa con campos inválidos. Ingreso manual de RUT/nombre siempre disponible. No persistir PII del formulario antes del consentimiento; durante edición sin consentimiento mantener sólo memoria volátil, sin SavedState persistente ni logs.

Aceptación: entradas válidas, vacías, mal formadas y longitudes límite; conservar formulario ante error recuperable mientras la pantalla sigue activa; no guardar ni sincronizar datos no consentidos. D09–D11.

### RF03. Preferencias académicas

Buscador y lista alfabética de opciones; entre 1 y 3 programas únicos al confirmar, con orden de prioridad 1..n, resumen visible, reordenación y eliminación individual. La lista seleccionada se presenta por prioridad, no se reordena alfabéticamente perdiendo el ranking. Sede de interés y sede del evento son conceptos distintos (F03); se permiten múltiples sedes opcionales sin máximo adicional al catálogo, como preferencias generales independientes de carreras/jornada. El mapa de oferta queda aplazado y no se presupone. No reutilizar el scope del captador como preferencia del prospecto. Cambiar contexto requiere revisar selección; conservar intereses históricos incluso si el catálogo los desactiva después. Se retira la métrica de dos toques.

Aceptación: buscar con/sin acentos; seleccionar 1, 2 o 3, rechazar cuarta/duplicada, reordenar y recuperar prioridades; no inventar ranking histórico. Cada carrera cuenta una vez por inscripción en métricas. D10/D13/D20 y F01 aprobado; dominio/persistencia tienen pruebas, pero búsqueda/reordenación visual y recorrido completo siguen pendientes.

### RF04. Metadatos y ubicación

Confirmar ubicación del evento al iniciar el trabajo e intentar ubicación del dispositivo por captura. Registrar lat/lon sólo si existen; fuente `DEVICE` o `EVENT_CONFIRMED`, momento de medición y precisión si procede. Valores de prueba PT: espera máxima 10 s, posición del dispositivo con edad <=5 min y precisión <=100 m; medir y ajustar antes de cierre de RF. No son requisitos del cliente.

Si GPS falla, permitir contexto del evento explícitamente confirmado por captador; coordenada manual sólo si se conoce, con rango validado. Nunca usar (0,0) como ausencia. Sin GPS ni contexto válido: guardar con consentimiento como `NEEDS_CONTEXT`, sin enviar. Resolver luego con ubicación del evento identificada como tal; no retrofechar una medición posterior. La aceptación institucional de esta alternativa al GPS por captura sigue pendiente; para la demo se identifica su procedencia.

Aceptación: permiso denegado, ubicación aproximada, timeout y GPS apagado no causan crash; datos y fuente visibles. D14/PT04.

### RF05. Consentimiento

Casilla desmarcada, texto offline versionado. Prospecto acepta directamente o captador registra su aceptación verbal explícita, distinguiendo los modos. Rechazo cancela y limpia lo digitado. Para demostración, texto rotulado como ficticio; no atribuir aprobación a Duoc. Política para menores y retiro formal queda pendiente para operación real; la demo usa personas sintéticas.

Aceptación: no existen filas personales persistidas/enviadas sin aceptación; versión y fecha viajan junto al registro; cancelar borra memoria del formulario. D21/D22/PT05.

### RF06. Persistencia local

Room con cifrado definido en SECURITY. Guardar registro y trabajo de envío en una transacción; confirmar éxito después del commit. Pendientes sobreviven cierre, reinicio y más de 24 h offline. Almacenamiento lleno o clave inaccesible produce error explícito, sin afirmar guardado ni resetear la base automáticamente.

Borrador incompleto: sólo con consentimiento previo y acción explícita; separado de inscripción, cifrado y recuperable por su autor. Sin envío ni conteo como prospecto válido. Recordatorio a las 24 h; eliminación deliberada, no automática.

Aceptación: crear, reiniciar y recuperar; error de escritura y aislamiento por usuario; ensayar 400 registros sin convertirlo en límite de captura. D17/D23/PT05/PT06.

### RF07. Consulta, edición y anulación

Bandeja propia con filtros/búsqueda locales. Editar pendientes o errores corregibles; no editar en envío ni envío de resultado incierto hasta reconciliar. Anulación confirmada con motivo mínimo y rastro sin duplicar PII en logs. Captador no modifica directamente una inscripción sincronizada; puede solicitar corrección al supervisor.

Duplicado local por RUT/evento: advertir y abrir registro propio existente. No crear otra inscripción activa ni mostrar datos de otro captador. El backend resuelve coincidencias entre dispositivos con conflicto explícito; la aplicación no promete detección global offline.

Aceptación: carrera de edición/envío, anulación, duplicados, corrección de rechazo y permisos. D12/D15/PT02/PT07.

### RF08. Sincronización automática

Cola durable sobre Room, WorkManager con conectividad requerida y reintentos acotados por backoff; datos móviles/Wi-Fi permitidos. Botón Reintentar reutiliza la cola, no ejecuta un segundo envío simultáneo. Servidor confirma individualmente UUID/versión y persiste antes de responder.

Timeout/red/5xx: reintentar el mismo contenido y clave; no crear duplicado. 401: detener y pedir autenticación. 403: intervención por permisos. 400/422: error por campo corregible. 409: conflicto de negocio o versión a resolver, no éxito implícito. Registrar diagnóstico sin datos personales. Conexión no garantiza API alcanzable ni ejecución inmediata del sistema operativo.

Aceptación: servidor acepta pero se pierde respuesta → un solo registro remoto después del reintento; restaurar cola tras reinicio; no marcar sincronizado antes de ACK. D19/PT.

### RF09. Resumen del captador

Por evento y día/rango: inscripciones completas activas, enviadas, pendientes, errores, pendientes de contexto; anuladas e incompletas separadas. Personas distintas = RUT distintos dentro del filtro; inscripciones = registros persona/evento. Intereses = una selección por carrera/inscripción; sumatoria de intereses puede superar personas.

Preservar reporte tras limpieza de copias sincronizadas mediante resumen sin PII o consulta/caché agregada del servidor. No sumar agregados remotos a copias locales ya reconocidas como si fueran personas nuevas.

Aceptación: evento de varios días, persona en dos eventos, varias carreras, anulación y purga local no generan totales engañosos. D20/PT06.

### RF10. Supervisión y correcciones

Supervisor consulta datos consolidados de sus sedes, último reporte por dispositivo y conflictos. Desconectado/sin reporte se muestra desconocido, no «0 pendientes». No puede forzar ejecución en otro teléfono desconectado. Puede solicitar revisión y verificar nuevos reportes; no vuelve a pedir datos al prospecto por una mera falla de red.

Correcciones remotas online con versión esperada y auditoría de actor, fecha, campos y motivo. Rechazar edición concurrente; no sobrescribir silenciosamente. RUT/evento erróneos requieren anulación/recreación supervisada y enlazada. Cierre con desconocidos es incompleto o excepcional con motivo; no borra pendientes.

Aceptación: dos captadores, un equipo offline, corrección concurrente y acceso a sede no autorizada. D18/D20/PT07/PT08.

### RF11. Asistencia por cámara

Nuevo ID, derivado de D05. Leer códigos mediante cámara, mostrar campos reconocidos y requerir confirmación antes de rellenar; conservar entrada manual si se deniega permiso o formato no soportado. No abrir URLs del código ni enviar su contenido a terceros. Sin almacenamiento de imágenes o contenido crudo.

Aceptación académica: código sintético de formato documentado y pruebas de formato inválido, cancelación y cámara denegada. Objetivo de cédula real: requiere parser validado con especificación/muestra autorizada y campos realmente disponibles. Una demo sintética NO acredita ese objetivo. No inventar nombres ni afirmar validación de identidad. Mantener posibilidad F04 de extraer nacimiento sólo con formato/contenido verificado, candidato confirmado y alternativa manual; no es soporte acreditado ni cambio implícito del QR sintético.

### RF12. Catálogo versionado

Nuevo ID para explicitar actualización ya prevista. Catálogo con fuente Duoc, fecha de revisión y sede/año cuando estén verificados. El cupón recibido aporta nombres y secciones, sin mapa programa/sede/jornada: transcripción documental no es oferta activa. Update desde backend del equipo al abrir jornada online en E2. Importación revisada, no scraping dentro del teléfono ni supuesto acceso a API institucional. Actualización transaccional y fallback al último catálogo válido.

Aceptación: sin red, descarga inválida, versión nueva y retiro de carrera; preservar referencias históricas. D08/D13/PT09.

### RF13. Administración de eventos y asignaciones

Nuevo ID que hace explícito el contexto necesario para RF01/RF10. Supervisor de sede crea/edita evento, fechas, ubicación y asigna captadores. Captador selecciona sólo eventos asignados; no se autoasigna ni cambia contexto de capturas anteriores. Cambio de evento limpia formulario sólo tras confirmación de descarte o guardado consentido.

Aceptación: límites de sede, evento multidía y cambio de asignación. Si una asignación se revoca mientras el captador está offline, servidor pone sus envíos posteriores en revisión; no los elimina. D18/PT08.

## 5. RNF y medidas

| ID | Requisito | Evidencia/criterio |
|---|---|---|
| RNF01 | Material 3, legibilidad, animaciones funcionales | Flujo completo objetivo <5 min; meta interna media <=3,5 min en 10 tareas ficticias documentadas; errores con texto/icono, no sólo color |
| RNF02 | Capacidad y rendimiento | Dataset mínimo 400 capturas/dispositivo; meta PT p95 <150 ms para transacción local, excluyendo GPS/red, con dispositivo y versión documentados; no bloquear UI |
| RNF03 | MVVM/UDF, negocio desacoplado | Composables sin DAOs/REST/validación de negocio; validadores testeables; estado inmutable |
| RNF04 | REST Spring Boot/Retrofit, contratos versionados | OpenAPI y tests de contrato antes de integración; HTTPS fuera de entorno local debug explícito |
| RNF05 | Tolerancia a fallos | Preservar transacciones confirmadas en ensayos de reinicio; no prometer recuperación ante pérdida física o daño del almacenamiento |
| RNF06 | Seguridad | Cifrado local, roles en servidor, cero secretos de infraestructura en repositorios/APK, sólo fixtures ficticias; sin declaración de auditoría legal |
| RNF07 | Cámara/GPS/conectividad | Pruebas de permiso, indisponibilidad y feedback en UI; no suponer que todos requieren diálogo de permiso |
| RNF08 | Android teléfono/tablet | minSdk 26 como meta del equipo sujeta a matriz de dependencias; portrait/landscape, tamaño de texto aumentado y flujo sin recortes |
| RNF09 | Entregables y colaboración | Git feature/develop/main, revisión cruzada, actividad real de tres integrantes, tablero, README, APK release firmado y código AVA identificados |

No hay rendimiento medido aún. Las métricas son objetivos de verificación, no resultados. P2 sigue siendo la prioridad; ENTREGAS distribuye los criterios sin eliminarlos. RNF04 y controles remotos de RNF05/06 se completan en E2. RNF09 se acredita por hito; firma en E1 sujeta a aclaración docente y obligatoria en E2. Cualquier reducción real de alcance requiere registrar el cambio, no esconder una omisión como cumplimiento.

## 6. Trazabilidad de evaluación

| P2 | Peso | RF/RNF y evidencia |
|---|---:|---|
| IE 2.1.1 | 15% | RF01/RF07/RF09/RF10/RF13; navegación y contexto |
| IE 2.1.2 | 15% | RF02/RF03/RF05; formularios completos y errores |
| IE 2.2.1 | 10% | RF02/RNF03; validación lógica y UiState |
| IE 2.2.2 | 10% | RNF01; animaciones útiles de carga/guardado/transición |
| IE 2.3.1 | 15% | RF06/RF08/RNF03; persistencia y modularidad |
| IE 2.3.2 | 20% | RNF09; commits, PR, tareas y participación |
| IE 2.4.1 | 15% | RF04/RF11/RNF07; GPS/cámara y conectividad mantenida por acuerdo E1 |

La tabla relaciona requisitos del producto con competencias P2; no exige que la parte remota de cada RF esté completa en E1. Persistencia y métricas se prueban localmente, no con una simulación de ACK. Inventario de formularios, criterios por hito y continuidad E2 se consultan en ENTREGAS. P4 agrega integración Spring/DB/CRUD, API externa, cobertura de lógica >=80 % y APK firmado. La API externa es una obligación de evaluación adicional, pendiente de selección, no se considera cubierta por la API propia del catálogo.

P3 complementaria, sin reutilizar su numeración como si fuera P2: validación/modificación 20%; diseño 7%; incorporación de componente 22%; estado/control 7%; persistencia en vivo 18%; arquitectura 7%; explicación nativa 7%; demostración nativa 12%. Cada integrante debe explicar y modificar el conjunto.

## 7. Pendientes y cambio de línea base

Formulario recibido y analizado: cupón de admisión 2027. F01–F03 aclarados y aprobados para el proyecto; dominio/persistencia implementados y suite Android v3 aprobada, con UI y revisión técnica humana pendientes. Oferta por sede/jornada aplazada, etiqueta institucional del campo personal no informada. Pendientes externos restantes: formato real de cédula; política institucional y menores; ampliación telefónica. Pendientes del equipo: permisos de colaboración, firma/AVA, revisión de PT y validación del entorno. Propietario/URLs y nombre técnico definidos el 03/10. Los pendientes no bloquean documentación, validadores base o infraestructura ficticia; sí impiden afirmar finalizadas las funcionalidades o autorizaciones que dependen de ellos.

Toda modificación futura registra ID, motivo, fuente, antes/después, impacto en pruebas/contrato y revisor. Mantener IDs RF01–RF10. RF11–RF13 explicitan alcance adicional, sin renumerar los originales.

## Referencias de decisiones

D01–D36 identifican respuestas de levantamiento del 29/09; Q identifica aclaraciones F2027; F01–F04 y sus reglas están explicadas en FORMULARIO_ADMISION_2027. AC identifica acuerdos comunicados por el equipo; PT identifica propuestas pendientes de revisión. Estos códigos aportan trazabilidad y no prueban aprobación institucional. Para implementar, las reglas completas son las descritas en este SRS, ENTREGAS y el contrato F2027.
