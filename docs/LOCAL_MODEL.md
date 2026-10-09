# Contrato local E1

Avance 09/10: contexto conectado y conectividad aislada en [ROUND0_BASE](ROUND0_BASE.md); formulario y contratos nativos en [ROUND1_INTERFACES](ROUND1_INTERFACES.md). [RECORD_CONTRACTS](RECORD_CONTRACTS.md) materializa firmas **propuestas**, sin DAO/implementación ni consumidores. No modifica el esquema v3 ni convierte ejemplos de mutaciones en APIs ya operativas.

Estado al 06/10: Room v3 y contexto/catálogo demo persistidos en 2298220; contrato consumidor en [DEMO_CONTEXT](DEMO_CONTEXT.md). Migración 2→3 aditiva y SQL del host comprobado; suite de 24 instrumentadas aprobada en Android 14/API 34 sobre v3. Las 16 aprobadas anteriormente corresponden al código 91b4983/Room v2. [ANDROID_BASELINE](ANDROID_BASELINE.md) separa evidencias. UI, administración, revisión y backup/transferencia efectiva pendientes.

## Contrato objetivo F2027 — 06/10/2026

F01–F03 están aclarados; campos y reglas canónicos en [FORMULARIO_ADMISION_2027](FORMULARIO_ADMISION_2027.md) y SRS 1.4. Dominio F2027 implementado parcialmente en `domain.admission`; persistencia F2027 probada en v2 y ampliada con contexto en v3; nueva suite Android v3 aprobada. AdmissionInput/AdmissionValidator/PrepareAdmission contemplan givenNames/familyNames, nacimiento/residencia obligatorios, actividad explícita y establecimiento condicional; campos opcionales, 1–3 intereses priorizados y lista opcional de sedes sin duplicados ni máximo adicional. Jornada/nivel/motivación opcionales; motivación sin texto libre. 26 pruebas nuevas de dominio pasan; los límites de preparación, autorización y guardado se describen aquí y la evidencia está en ANDROID_BASELINE.

Las preferencias son independientes: CaptureInterest referencia programa/versión y prioridad; no exige Offering ni cruce sede/jornada. CaptureSitePreference relaciona captura con sedes preferidas y snapshot; no modifica event.siteId ni scope del operador. La mención de turismo es opcional bajo su programa y no consume otro cupo. Programas/secciones homónimos no se fusionan por nombre sin una fuente que pruebe identidad. Catálogo demo sintético; transcripción nacional no equivale a oferta disponible.

PrepareCapture del código anterior aún filtra ofertas por sede del evento y no valida prioridad: no usarlo como contrato final para consumidores nuevos. PrepareAdmission no filtra preferencias por sede del evento; no sustituye autorización de contexto ni acredita guardado. AdmissionRepository/RoomAdmissionRepository ya implementan alta y lectura Found/Legacy; la ampliación v3 ya pasó la suite Android; revisión y UI pendientes. [Fixture objetivo](fixtures/f2027-contract.json) expresa casos documentales, no tests ejecutados automáticamente. El fixture v0.1 conserva evidencia del comportamiento anterior.

## Antecedente vigente: migración F2027 v1→v2, probada en Android

Implementado en 91b4983: esquema v2 exportado, migración y repositorio F2027 compilados. Verificación SQL del host pasada (preservación, esquema/FK/índices y rollback); no acredita migración cifrada Android. Siete pruebas F2027 instrumentadas aprobadas posteriormente sobre v2; ver evidencia del 06/10. No conectar dominio F2027 a RoomCaptureRepository antiguo; usar AdmissionRepository. La tabla siguiente describe el diseño aplicado.

| Área física | Cambio implementado y preservación |
|---|---|
| captures | Agregar contractVersion: filas antiguas LEGACY_1; altas nuevas F2027_1. Nuevos campos F2027 nullable para permitir borrador/histórico. Mantener fullName y educationLevel antiguos, nullable en esquema; fullName nuevo se compone como snapshot y educationLevel nuevo queda null; no dividir nombres ni derivar actividad de SECONDARY. Establecimiento pasa a nullable para nuevos casos opcionales; conservar cada valor antiguo |
| capture_interests | Mantener PK captureId/programId y snapshots antiguos; offeringId pasa a nullable. Agregar priority, section y mentionId/mentionName nullable. Filas antiguas conservan offeringId y quedan sin ranking/sección/mención inferidos; filas F2027 llevan programa/versión/prioridad y offeringId null |
| Índices de intereses | Único captureId/priority para prioridades no nulas; validación del repositorio impone 1..n, unicidad y máximo tres al confirmar F2027. No reducir ni eliminar intereses históricos que superen tres |
| capture_site_preferences | Nueva tabla con PK captureId/siteId, FK a captura, snapshot nombre/versión. Sin filas automáticas para capturas antiguas; event.siteId nunca se convierte en preferencia |
| Consentimiento/contexto/clave activa/auditoría | Conservar IDs, timestamps, actor/sede/evento, revisión, evidencia y estados exactamente. No reasignar propiedad ni liberar unicidad al migrar. Mantener FK sin borrado en cascada |

La modificación de nulabilidad exige reconstrucción controlada de tablas en SQLite; no basta ALTER de columnas. Preparar SQL con copia explícita de columnas y verificar todas las FK/índices en la prueba de actualización. No usar fallbackToDestructiveMigration, recrear el archivo cifrado ni regenerar claves. Una falla debe impedir apertura/escritura sin borrar los originales.

Lectura: resultado tipado con versión de contrato, distinguiendo Legacy de F2027. Mostrar datos históricos sin revalidarlos como altas actuales; no rebajar READY a DRAFT por campos nuevos desconocidos. No fabricar un AdmissionInput completo a partir de una fila antigua. Edición/conversión posterior deberá pedir datos faltantes y confirmar prioridades explícitamente; aún fuera de este incremento.

Alta F2027: contexto confiable y autorización del evento → PrepareAdmission → transacción de consentimiento inmutable, captura, intereses ordenados, sedes, reserva RUT/evento y auditoría. Mantener manejo de duplicados ajenos sin filtrar ID/PII y rollback completo. La UI confirma guardado sólo después de commit. Modalidad es preferencia opcional separada de jornada, sin cruce implícito con programa/sede.

Pruebas requeridas para cerrar esta adaptación: base cifrada v1 con DRAFT/READY/NEEDS_CONTEXT, consentimientos y más de tres intereses antiguos; migrar y comparar todos los valores originales; campos nuevos desconocidos; PK/FK/índices intactos; lectura autorizada de ambas versiones; alta F2027 con cuatro sedes y tres prioridades, cerrar/reabrir; conflicto de unicidad, rollback parcial y clave inaccesible sin reset. 1.json se conserva y 2.json ya está exportado; ejecutar las pruebas instrumentadas en Android. La suite v2 y la suite v3 ya pasaron en sus versiones registradas; repetir sólo ante cambios pertinentes o nueva aceptación de entorno.

## Implementación disponible y compatibilidad

La evidencia por versión está en [ANDROID_BASELINE](ANDROID_BASELINE.md). Domain/admission es el contrato para nuevas altas; domain/capture conserva tipos comunes y compatibilidad LEGACY_1. No eliminarlo hasta revisar sus consumidores y migraciones. Las pruebas históricas no son tareas nuevas pendientes por repetición de texto.

AdmissionRepository expone create/get; DemoContextRepository inicializa y selecciona contexto. Listado, actualización, anulación, corrección supervisada y métricas son objetivo pendiente. No suponer que la tabla semántica de abajo ya es una API Kotlin.

Normalización: trim, RUT sin puntos con guion/DV mayúscula, teléfono sin espacios, opcionales vacíos a null. El DV no verifica existencia/identidad y el correo no verifica titularidad. Borrador consentido permite campos incompletos o mal formados para corregirlos, conservando límites y reglas estructurales; confirmar aplica requisitos completos. La UI nunca informa guardado por obtener Prepared: espera commit del repositorio.

## Límites e identidad

Un módulo Android `app`, dominio Kotlin independiente de Android, interfaces de repositorio y adaptadores Room/plataforma. No añadir tablas de tokens, outbox, Retrofit ni backend en E1. El único estado de transporte E1 es `NOT_QUEUED`; red disponible nunca significa enviado.

IDs UUID generados una vez, fechas como Instant UTC y zona IANA del evento por separado. `deviceId` es un UUID aleatorio de instalación, no un identificador de hardware. `revision` comienza en 1 y aumenta en cada modificación confirmada. Valores de presentación se formatean en UI; no guardar fechas localizadas. Datos personales únicamente dentro de la base cifrada después del consentimiento.

No crear un registro maestro Person editable en E1: cada captura conserva su snapshot de contacto. Esto evita que una corrección de teléfono cambie otro evento. La entidad conceptual Person podrá materializarse en E2 bajo un contrato explícito.

## Esquema lógico para Room

Todas las FK se validan; sin cascadas que eliminen capturas. El primer subconjunto físico v1 ya está exportado; los cambios posteriores deberán añadir migraciones no destructivas y sus pruebas. La suite Android v3 de almacenamiento/contexto ya pasó; las operaciones objetivo aún no implementadas requieren sus propias pruebas. No se prescribe aquí un índice parcial que Room aún no haya comprobado.

| Modelo / tabla propuesta | Campos mínimos y restricciones |
|---|---|
| DemoProfile | id, displayName, role (`CAPTOR`/`SUPERVISOR`); rótulo de demo, sin contraseña ni token ficticio |
| ProfileSiteScope | profileId + siteId, clave compuesta única |
| Site | id, name; datos sintéticos identificados |
| Event | id, siteId, name, startsAt, endsAt, timeZone, declaredLatitude/Longitude opcionales; inicio anterior al fin, coordenadas completas y en rango |
| Assignment | eventId + captorId únicos; enabled; no borrar el contexto histórico al deshabilitar |
| CatalogVersion | id, sourceKind (`SYNTHETIC`/`OFFICIAL_REVIEWED`), sourceReference, consultedAt, academicYear; no rotular fixture como oferta oficial |
| Program / Offering | Objetivo: Program versionado, sección/origen e identidad estable. Offering no es requisito de preferencias generales E1; conservar referencias históricas existentes sin inventar relaciones programa/sede/jornada |
| ConsentText | version, text, isSynthetic; versión inmutable |
| Capture | id, revision, state, transportState, ownerId, siteId, eventId, deviceId, capturedAt, updatedAt, eventTimeZoneSnapshot, eventNameSnapshot; contacto, consentimiento y ubicación descritos abajo |
| CaptureInterest | captureId + programId únicos; catalogVersionId, programNameSnapshot, prioridad 1..n y mención opcional aplicable; máximo 3 programas confirmados. No exigir oferta o sede por carrera |
| CaptureSitePreference | captureId + siteId únicos, nombre/versionado de referencia; cero o más, independiente de intereses y event.siteId |
| ActiveRegistrationKey | eventId + normalizedRut únicos; captureId único; sólo para `READY`/`NEEDS_CONTEXT`; reserva transaccional contra duplicados |
| LocalAudit | id, captureId, actorId, occurredAt, action, previousRevision, resultingRevision, changedFields, reasonCode y reasonText breve opcional; no copiar valores personales anteriores/nuevos |
| CorrectionLink | originalCaptureId, replacementCaptureId, actorId, reason, occurredAt; original CANCELLED y reemplazo válido en la misma transacción |

Contacto objetivo Capture: campos y obligatoriedad F2027, incluidos nombres/apellidos, birthDate/residenceCommune/currentActivity, establecimiento condicional y opcionales definidos. fullName/educationLevel describen la base antigua; no duplicar dos reglas activas. En DRAFT se permiten campos pendientes/nulos y cero intereses; en READY/NEEDS_CONTEXT todos cumplen SRS §3. Normalizar RUT sin puntos y con guion/DV mayúscula (ejemplo ficticio `12345678-5`); no usar RUT como PK. Un DV correcto no verifica identidad. Los borradores no reservan unicidad y nunca se cuentan como inscripción. Al completarlos se comprueba duplicado dentro de la transacción.

Consentimiento embebido obligatorio en toda Capture persistida: consentVersion (FK a texto inmutable), acceptedAt, recordedBy, acceptanceMode (`DIRECT`/`VERBAL`). No existe una fila personal con consentimiento falso. La edición ordinaria preserva este comprobante; retirada formal de consentimiento sigue pendiente de política institucional, no se equipara silenciosamente a anulación académica.

Ubicación: source (`DEVICE`/`EVENT_CONFIRMED`), latitude/longitude, measuredAt opcional, accuracyMeters opcional, confirmedAt opcional y confirmedBy opcional. Sin ubicación, todos nulos. DEVICE exige coordenadas, measuredAt y precisión válida conforme a PT04. EVENT_CONFIRMED exige coordenadas conocidas del evento y confirmación explícita del captador; no inventar precisión GPS ni retrofechar medición. Registrar la causa de fallback por enum sin PII. El timestamp original de captura no se sustituye al completar contexto posteriormente.

## Estados y operaciones atómicas

| Operación | Resultado |
|---|---|
| Escribir formulario nuevo, sin aceptar consentimiento | Sólo memoria volátil; nunca Room, logs ni SavedState persistente |
| Guardar borrador explícitamente, con consentimiento | DRAFT, incluso si el usuario dejó datos completos; fuera de métricas y unicidad hasta confirmar inscripción |
| Confirmar inscripción consentida con todos los campos/intereses válidos | READY si hay contexto válido; NEEDS_CONTEXT si falta ubicación válida; en ambos reservar RUT/evento |
| Completar contexto de NEEDS_CONTEXT | READY; mantener capturedAt e identificar la procedencia posterior |
| Editar inscripción | No degradar campos obligatorios a DRAFT; error por campo si quedan inválidos. Preservar identidad, consentimiento y contexto original |
| Cancelar edición | Descartar copia de trabajo; conservar íntegra la revisión guardada |
| Cancelar formulario nuevo | Limpiar memoria; no crear anulación ni auditoría personal |
| Anular captura persistida con motivo | CANCELLED + auditoría + liberar clave activa; conservar datos cifrados, sin borrado físico automático |
| Corregir RUT/evento de inscripción | Supervisor: anular/recrear enlazado en transacción, verificar permisos en ambos eventos; nunca editar esas claves directamente |

Cada escritura comprueba actor/alcance, consentimiento, expectedRevision y validaciones en la misma operación. Guardar Capture + intereses + clave activa + auditoría en una transacción. Si falla cualquier parte, rollback total; UI confirma sólo tras commit. Colisión de versión devuelve conflicto y preserva el formulario sin sobrescribir. CANCELLED es terminal; recreación genera otro UUID. Corregir un borrador propio puede cambiar su RUT; su contexto de creación se conserva.

La clave activa impide dos capturas completas del mismo RUT/evento aun con dos operaciones concurrentes. No usar sólo una consulta previa en UI. Anular y recrear debe restaurar el original si la creación falla. Si el duplicado pertenece a otro captador, devolver error genérico sin ID ni datos; el supervisor lo revisa desde su bandeja autorizada.

## Matriz local de acciones

Perfiles demo, sin afirmar autenticación segura ni autorización remota. Aplicar restricciones en repositorio además de UI; los listados, detalles y métricas usan el mismo alcance.

| Acción | Captador | Supervisor |
|---|---|---|
| Crear inscripción/borrador | Evento asignado de su sede | No atribuir capturas a terceros; usar perfil captador para capturar |
| Consultar/editar borrador | Sólo propio | Sin acceso a borradores ajenos en E1; criterio conservador por SRS RF06 |
| Consultar inscripción | Sólo propia | Capturas locales de sus sedes autorizadas |
| Editar contacto/intereses de inscripción activa | Sólo propia, NOT_QUEUED | En sus sedes, con motivo y auditoría |
| Corregir RUT/evento de inscripción | Solicitar revisión | Anular/recrear enlazado en sus sedes; conservar autor original y actor de corrección |
| Anular inscripción | Propia con motivo | En sus sedes con motivo |
| Crear/editar eventos y asignaciones | No | En sus sedes; no alterar snapshots ya capturados |
| Ver métricas | Propias | Locales de sus sedes; no son cifras consolidadas de otros equipos |

Los perfiles demo no son credenciales y cualquiera que maneje el dispositivo de demo puede seleccionarlos. No introducir bloqueo ficticio. PT12 continúa pendiente: la captura asistida puede desarrollarse; entregar el teléfono al prospecto exige primero resolver el modo restringido. La exclusión de borradores ajenos y el formulario exclusivo de captador son criterios conservadores de este contrato para revisión humana, no nuevos derechos aprobados por cliente.

## Interfaces y errores compartidos

Contrato semántico objetivo, no inventario de APIs Kotlin implementadas. AdmissionRepository y DemoContextRepository son los consumidores actuales de altas F2027/contexto; sus tipos reales prevalecen sobre los nombres conceptuales siguientes. Listados/edición/anulación/métricas y servicios nativos siguen pendientes. Materializar y revisar cada interfaz antes de sus ramas consumidoras, sin generar repositorios vacíos.

| Interfaz | Operaciones y resultado |
|---|---|
| API objetivo de registros (ampliación de AdmissionRepository, nombre por revisar) | observeList(actor, filter): Flow de resúmenes autorizados; get(actor,id); saveDraft(actor,input,expectedRevision?); confirm(actor,input,expectedRevision?); update(actor,id,input,expectedRevision); completeContext(actor,id,location,expectedRevision); cancel(actor,id,reason,expectedRevision); correctIdentity(actor,id,replacement,reason,expectedRevision) |
| ContextRepository | observeProfiles; observeAssignedEvents(actor); selectContext(actor,eventId); saveEvent/assignCaptor sólo con permiso supervisor y revisión esperada |
| CatalogRepository | observePrograms y observeSites; validar IDs/versiones sin descargar ni exigir cruce programa/sede/jornada en E1; adaptar la interfaz anterior de ofertas |
| SummaryRepository | observeSummary(actor,eventId,dateRange); rangos por fecha local del evento, extremos inclusivos para el usuario |
| LocationProvider | requestLocation(): resultado DEVICE o causa tipada (denegado, apagado, timeout, no disponible); confirmación de evento es otra acción explícita |
| ScanParser / lector de cámara | Cámara entrega texto transitorio; parser puro devuelve candidato sintético o formato no soportado; UI confirma antes de aplicar; sin persistir texto crudo |
| ConnectivityObserver | Flow de UNKNOWN/OFFLINE/CONNECTED_UNVALIDATED/VALIDATED; ninguna variante promete salud del backend |

Mutaciones devuelven Success(id,revision,state) o Validation(fieldErrors), ConsentRequired, AccessDenied, DuplicateOwn(id), DuplicateRestricted, RevisionConflict, NotFound, StorageUnavailable, KeyUnavailable. Errores no devuelven PII/logs SQL. El actor viene del contexto seleccionado, no de un campo editable del formulario. Clave inaccesible o almacenamiento lleno no crean base nueva ni borran trabajo. E2 necesitará adaptar estas interfaces: no se declara listo el contrato REST.

## Métricas, fixtures y verificaciones

Métrica principal: READY activas; NEEDS_CONTEXT se cuenta por separado como captura completa pendiente de contexto. DRAFT y CANCELLED separados. Personas distintas e intereses usan READY dentro del filtro; mostrar este denominador en UI. Un mismo RUT en dos eventos suma dos inscripciones y una persona al consultar ambos. No mostrar NOT_QUEUED como «pendiente de envío» en E1: usar «guardado local». E2 revisará categorías remotas sin cambiar retroactivamente qué contó E1.

[Fixtures E1](fixtures/e1-contract.json) define datos sintéticos de entrada y resultados esperados; no es un seed Room ejecutable. Incluye formato QR exclusivo de demostración `G6DEMO:1:<JSON>` con exactamente fullName y rut; máximo 512 caracteres; tipos string, límites SRS y DV válido. Rechazar versiones/campos desconocidos, URLs y JSON malformado. No interpretar este formato como cédula chilena ni ejecutar contenido del QR. Aplicar candidato sólo tras confirmación; validar otra vez el formulario antes de guardar.

Antes de cerrar BASE-02/DATA-01 comprobar: borrador consentido incompleto; rechazo sin escritura; cancelación de edición intacta; doble guardado concurrente; duplicado ajeno sin filtración; rollback anular/recrear; revisión obsoleta; aislamiento de perfil/sede; clave inaccesible sin reset; permiso GPS denegado y fallback confirmado; QR inválido/manual; red sin ACK; reinicio con registros conservados. Esta lista mezcla invariantes ya cubiertas con operaciones aún pendientes. Consultar ANDROID_BASELINE para evidencia realizada; edición/anulación, nativos y recorridos UI necesitan pruebas nuevas.
