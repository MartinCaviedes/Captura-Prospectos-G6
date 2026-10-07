# Contexto y catálogo locales de demostración

Incremento E1 del 06/10/2026, código `2298220`, Room v3. Base de CONTEXT-01/CATALOG-01 y RF01/03/12/13; no cierra estos RF. Implementado y probado mediante suite completa de 24 instrumentadas en Android 14/API 34, 06/10; revisión humana pendiente. No necesita backend, Neon, cuentas ni tokens.

## Datos iniciales y procedencia

`DemoSeed` es la fuente ejecutable del fixture. `seedVersion=g6-synthetic-context-v1`, catálogo `g6-synthetic-catalog-v1`, procedencia `SYNTHETIC`, año académico 2027. No procede de una oferta institucional. El PDF y sus 84 entradas siguen siendo referencia documental y no alimentan este seed.

| Referencia del fixture | Contenido y uso |
|---|---|
| captor-a | Captador A — DEMO; ámbito site-1; eventos event-a y event-a2 |
| captor-b | Captador B — DEMO; ámbito site-2; evento event-b |
| supervisor | Supervisor — DEMO; ámbitos site-1 y site-2; puede consultar según permisos, no capturar en nombre de otro |
| site-1..site-4 | Cuatro sedes ficticias independientes de las preferencias de carrera |
| event-a | Visita ficticia A, site-1; establecimiento sugerible Colegio ficticio A / Comuna ficticia A |
| event-a2 | Feria ficticia A, site-1; sin establecimiento configurado |
| event-b | Visita ficticia B, site-2; sin establecimiento configurado |
| program-1..program-4 | Cuatro programas ficticios; program-2 sección DEMO_ONLINE, restantes DEMO_PHYSICAL |
| mention-a / mention-b | Dos menciones ficticias opcionales bajo program-3; no son carreras adicionales |

IDs: `UUID.nameUUIDFromBytes` sobre UTF-8 de `g6-synthetic-context-v1/<referencia>`. Son estables para esta versión; no cambiar claves/versiones ni renumerar capturas para repartir trabajo. deviceId es UUID aleatorio generado una vez al inicializar y conservado al cerrar selección/reabrir. No identifica hardware.

Eventos: inicio `2026-10-01T12:00:00Z`, fin `2026-11-01T23:00:00Z`, zona `America/Santiago`; sin coordenadas. Son fechas de fixture, no agenda institucional. No añadir cierre automático por fecha: el contrato actual valida el intervalo y asignación, no restringe altas por reloj. Sin ubicación válida, la captura confirmada queda NEEDS_CONTEXT.

Consentimiento `g6-synthetic-consent-v1`: texto explícitamente ficticio almacenado inmutablemente. Disponer del texto no es aceptación: cada alta/borrador sigue requiriendo ConsentEvidence. No se cargan prospectos, RUT, teléfonos ni capturas en el seed. No hay opciones nuevas de modalidad/canal: permanecen opcionales sin catálogo inventado.

## Inicialización y almacenamiento

Room v3 agrega nueve tablas demo: catálogo/versiones, sedes, perfiles, ámbitos, eventos, asignaciones, programas, menciones y contexto seleccionado. FK sin cascadas destructivas. `CaptureMigration2To3` sólo crea tablas/índices; no cambia filas v2 ni inicializa fixtures. Se conserva ruta 1→2→3 y esquemas 1/2.

`RoomDemoContextRepository.initialize()` es una acción explícita de bootstrap demo, transaccional e idempotente. No se llama aún desde la plantilla. Inserta el marcador de versión al final; ante fallo revierte también el consentimiento y todo el seed. Si ya está instalado devuelve ALREADY_INITIALIZED sin sobrescribir selección/deviceId; si hay versión, tablas parciales o consentimiento incompatibles, devuelve INCOMPATIBLE_SEED y conserva los datos. No borrar/resembrar automáticamente para reparar.

## Contrato para el siguiente consumidor

| Operación | Comportamiento |
|---|---|
| snapshot / observe | NotInitialized, Ready o StorageUnavailable; Ready contiene catálogo, perfiles, selección, eventos autorizados y texto de consentimiento |
| selectProfile(id) | Exige perfil existente, limpia evento; no modifica capturas ni selecciona preferencias del prospecto |
| selectEvent(id) | Exige captador seleccionado, asignación y ámbito; rechaza evento desconocido/ajeno conservando selección válida |
| clearSelection | Limpia perfil/evento únicamente; conserva deviceId, fixtures y capturas |
| currentActor | Actor derivado del perfil persistido, no de campos libres del formulario |
| environment(eventId) | Devuelve AdmissionEnvironment sólo para evento actualmente seleccionado y autorizado; incluye programas/sedes independientes y consentimiento |

La fuente se inyecta en RoomAdmissionRepository. El supervisor no tiene eventos seleccionables **para capturar** en este incremento; su futura administración/listado de eventos es otra operación, pendiente. Los perfiles son accesibles como demostración, no credenciales seguras.

Establecimiento sólo se expone como sugerencia con evento identificado. UI debe ofrecer confirmar/editar; nunca copiarlo automáticamente al formulario ni inferir residencia/GPS. Cambiar contexto durante un formulario exige revisión explícita; conservar los datos de trabajo sin reasignar autor de una captura ya guardada.

La observación reacciona a inicialización/selección. Catálogo/perfiles/eventos son inmutables en este incremento. Antes de agregar edición/asignaciones, ampliar invalidación del Flow, revisión esperada, auditoría y validación; no escribir tablas desde composables ni tomar esta base como CRUD terminado.

## Comprobaciones y límites

58 JVM pasan (56 anteriores y dos de coherencia de fixture); build demoDebug y APK instrumental compilados, Lint 17 avisos/0 errores. Ocho pruebas DemoContextTest ejecutadas con éxito: inicialización concurrente/idempotencia, reapertura, permisos/cambio de perfil, guardado con contexto persistido y cierre sin pérdida, consentimiento conflictivo, rollback/reintento, observación y migración cifrada v2→v3 conservando captura.

`python tools/verify_context_migration_sql.py` pasó en SQLite del host: cadena 1→2→3, preservación de filas v2, esquema/FK/índices, ausencia de seed automático y rollback. No sustituye Android. La suite instrumental actual de 24 pruebas pasó sobre 2298220/Room v3 en 23.178 s. Las 16 de 91b4983/Room v2 permanecen como antecedente.

Siguiente: revisar diff/evidencia y continuar BASE-03/FORM-01 con selección explícita → consentimiento → formulario → guardado → detalle. Sin cambios de negocio F2027, dependencias nuevas, UI conectada, administración de eventos ni RF completo. Evidencia en [ANDROID_BASELINE](ANDROID_BASELINE.md).
