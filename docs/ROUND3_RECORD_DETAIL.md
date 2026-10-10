# Ronda 3 · RECORDS-01-C: Detalle y edición local de capturas (RF07)

Incremento local sobre la base de ronda 2 (`feature/e1-ronda3-record-detail`). Completa el ciclo funcional de gestión local de capturas exigido para la entrega E1 (P2/P3), permitiendo la inspección detallada de las fichas de prospectos y la edición controlada en Room cifrado sin alterar identificadores ni violar reglas de privacidad.

## Alcance técnico implementado

| Entregable | Responsabilidad e integración |
|---|---|
| **RECORDS-01-C (Lectura de Ficha)** | `RecordRepository.get(id)` delega de forma tipada en la consulta versionada de admisiones, recuperando `StoredAdmission` con validación de acceso por rol (`LocalCaptureAccess.canRead`). |
| **RECORDS-01-C (Edición en Room)** | `RoomRecordRepository.edit(EditAdmission)` implementa la actualización transaccional de campos mutables (`email`, `phone`, `residenceCommune`, `establishment`, `notes`, etc.) mediante `CaptureDao.updateCapture`. Valida coincidencia de revisión (`expectedRevision`), prohíbe mutar el RUT en altas `READY`, exige motivo obligatorio (5–300 caracteres) a supervisores sobre registros no propios, e inserta auditoría local inmutable (`local_audit`). |
| **UI Detalle (RF07)** | `RecordDetailDialog` en Compose. Ficha completa con datos personales, académicos, preferencias, 1–3 carreras priorizadas con mención, sedes de interés, consentimiento versionado, coordenadas/fuente de ubicación y rótulo explícito de transporte local (`NOT_QUEUED`). |
| **UI Edición (RF07)** | Modo de edición integrado en `RecordDetailDialog` y orquestado por `RecordTrayViewModel`. Permite corregir datos de contacto o notas, y guarda los cambios reactivamente actualizando la bandeja y la revisión de la ficha. |

## Reglas de negocio y privacidad preservadas

1. **Inmutabilidad de la identidad y claves activas:**
   - En una captura confirmada (`READY`), el RUT no puede ser modificado mediante edición para evitar inconsistencias con la reserva activa (`active_registration_keys`).
   - Las capturas anuladas (`CANCELLED`) no admiten edición ni reactivación (`InvalidTransition`).
2. **Aislamiento por rol:**
   - El captador solo puede inspeccionar y editar sus propias capturas.
   - El supervisor puede inspeccionar y editar capturas en sus sedes asignadas, pero no puede editar borradores ajenos ni modificar registros sin proporcionar un motivo auditable.
3. **Auditoría local estricta:**
   - Toda edición incrementa la revisión en +1 y genera un registro en `local_audit` indicando el actor, instante y acción (`EDITED` o `SUPERVISOR_EDITED`).
4. **Transporte estrictamente local:**
   - La ficha exhibe con claridad que el registro permanece en almacenamiento seguro del dispositivo con transporte `NOT_QUEUED`, sin anticipar sincronizaciones de backend en E1.

## Pruebas agregadas y verificadas

- `RecordTrayViewModelTest`: 12 pruebas unitarias en JVM que verifican:
  - Carga y filtrado de la bandeja.
  - Apertura del diálogo de detalle cargando el `StoredAdmission` tipado.
  - Cierre y descarte seguro del detalle en memoria.
  - Activación y cancelación del modo de edición.
  - Guardado exitoso de edición con actualización de revisión y mensaje accesible.
  - Flujo de anulación con motivo y control de concurrencia/conflictos de revisión.
- Verificación de esquema SQLite: Aprobados scripts `verify_migration_sql.py` y `verify_context_migration_sql.py`.
