# Propuesta de consultas y mutaciones locales · RECORDS-01-A

09/10/2026. Contrato Kotlin compilable en `domain/records/RecordContracts.kt`, **propuesto para revisión, sin implementación Room ni consumidores**. Extiende el alcance semántico de [LOCAL_MODEL](LOCAL_MODEL.md); no cambia AdmissionRepository.create/get ni el esquema v3. Antes de RECORDS-01-B/C, registrar revisión y fijar esta propuesta como contrato compartido o corregirla una sola vez.

## Consulta

`RecordRepository.list(RecordQuery)` devuelve Page, InvalidQuery, AccessDenied o StorageUnavailable. Page contiene resúmenes autorizados y cursor opcional; lista vacía significa consulta exitosa sin coincidencias, nunca error. El actor se obtiene del contexto de aplicación, no del comando ni de los filtros.

- Captador: sólo propios de sus sedes. Supervisor: inscripciones de sus sedes, excluyendo borradores ajenos. Aplicar exactamente el mismo alcance que `get`; filtrar antes de paginar. No filtrar datos ajenos después de devolverlos a UI.
- Query vacío incluye todo lo autorizado. Búsqueda propuesta: subcadena de nombre sin distinción de mayúsculas/acentos o de RUT normalizado; nunca interpolar SQL. Máximo 120 caracteres; los signos SQL son literales. Normalización idéntica en consulta y datos indexados.
- Evento opcional, estados vacíos = todos los autorizados, fechas UTC `[fromInclusive, untilExclusive)`. UI convierte los días en la zona del evento. Intervalo invertido o igual: InvalidQuery.
- Propuesta de paginación: límite 1–100, valor inicial 30; orden `capturedAt DESC, id UUID canónico DESC` con comparación binaria. Cursor exclusivo de la última fila. No garantiza snapshot entre páginas: refrescar desde inicio tras mutación o cambio de actor/filtro, sin repetir registros por un offset desplazado.
- Resumen: ID, revisión, instante, evento, nombre, RUT, estado y LEGACY/F2027. toString oculta datos personales. No devolver coordenadas ni consentimiento en bandeja.

## Edición y anulación

`edit(EditAdmission)` lleva ID, expectedRevision, AdmissionInput, intención, ubicación y motivo opcional. `cancel(CancelRecord)` lleva ID, expectedRevision y motivo. El ID del actor, la propiedad, sede/evento, dispositivo, instante original y consentimiento persistido nunca vienen como campos editables. Valores de ubicación nuevos se validan contra LocationPolicy; no retrofechar GPS ni sobreescribir con una ubicación ausente accidental.

Orden transaccional: resolver actor/autorización y registro → comparar revisión positiva → validar transición/contrato/campos → comprobar unicidad → actualizar fila, selecciones, clave activa y auditoría conjuntamente. Un fallo revierte todo. No confirmar éxito antes de commit. Aumentar revisión una vez por mutación efectiva; los reintentos con revisión vieja producen conflicto, no otra modificación.

| Situación | Resultado contractual |
|---|---|
| Sin acceso o ID inexistente | Unavailable, sin revelar existencia ni revisión |
| expectedRevision <= 0 / revisión autorizada obsoleta | InvalidRevision / RevisionConflict |
| Borrador propio F2027 | Editar entrada y validar con PrepareAdmission; confirmar sólo si cumple requisitos |
| Inscripción activa propia NOT_QUEUED | Editar contacto/intereses; no cambiar identidad RUT/evento ni degradar a DRAFT |
| Supervisor en su sede | Editar inscripción no borrador, con motivo y auditoría; no reasignar autor |
| CANCELLED | Sin edición/reactivación; InvalidTransition |
| LEGACY | Lectura/listado y anulación conservadora; edición F2027 devuelve UnsupportedContract, sin inventar nombres separados/prioridades |
| Duplicado propio / ajeno | DuplicateOwn(id) / DuplicateRestricted sin identificador ajeno |
| Anulación autorizada | Motivo explícito, estado CANCELLED y liberación transaccional de clave activa; conservar datos y rastro |
| Fallo de almacenamiento o clave | StorageUnavailable, sin reset ni registro de SQL/PII |

Propuestas a revisar: motivo no vacío de 5–300 caracteres tras trim para anulación y edición supervisora; ReasonRequired si vacío, InvalidReason si fuera de rango. En borrador propio se permite corregir RUT antes de confirmar; una inscripción activa conserva RUT. Corrección supervisada anular/recrear enlazada necesita contrato/operación específica posterior, no dos llamadas independientes con riesgo de pérdida. Retirada institucional del consentimiento no se equipara a este procedimiento académico.

La auditoría actual v3 no almacena todos los atributos necesarios para edición/anulación. RECORDS-01-B deberá coordinar migración aditiva para motivo, revisión anterior y campos modificados (nombres de campo, sin duplicar sus valores personales), y enlaces de corrección cuando se implemente esa operación. Este documento no reserva una versión Room ni declara la migración hecha.

## Casos de revisión y trabajo independiente

[Fixtures de contrato](fixtures/records-contract.json) describen permisos, conflicto, transición, unicidad y rollback. Son ejemplos esperados; las dos pruebas JVM actuales sólo verifican límites de consulta y diagnóstico sin PII. **No acreditan mutaciones ni autorización ejecutada**, que necesitan tests Room de ronda 2.

Después de revisar las firmas: persistencia implementa este contrato; UI de bandeja puede usar un doble identificado con los mismos resultados. Ningún consumidor debe inventar una API alternativa ni presentar éxito simulado como guardado. Formulario de alta y dispositivos no dependen de completar estas mutaciones.
