# Ronda 2 · RECORDS-01-B y Bandeja de capturas locales

Incremento local sobre la base de ronda 2 (`feature/e1-ronda2-records-tray`). Implementa el repositorio de consulta y gestión de capturas en Room cifrado (`RoomRecordRepository`), así como la interfaz gráfica de bandeja de capturas (`RecordTrayScreen`, `RecordTrayViewModel`) accesible desde el Hub principal.

## Alcance técnico implementado

| Entregable | Responsabilidad e integración |
|---|---|
| **RECORDS-01-B (Repositorio)** | `RoomRecordRepository` cumpliendo `RecordContracts.kt`. Implementa `list(RecordQuery)` con aislamiento por rol (Captador ve sus propias capturas; Supervisor ve sedes autorizadas sin borradores ajenos), filtrado por estado, texto normalizado, fecha y paginación por cursor `(capturedAt DESC, id DESC)`. `cancel(CancelRecord)` con motivo validado (5–300 caracteres), transacción atómica de revocación de clave activa (`active_registration_keys`) y registro en `local_audit`. |
| **DAO & Sesión** | Métodos `@Query` en `CaptureDao` (`capturesByOwner`, `capturesBySitesExcludingDraft`, `updateState`, `deleteKey`) sin alteración de esquema Room v3. Exposición en `ContextSession`, `RoomContextSession` y `ContextSessionOwner`. |
| **UI Bandeja (RECORDS-01-B)** | `RecordTrayScreen`, `RecordTrayRoute` y `RecordTrayViewModel`. Barra de búsqueda, chips de filtro por estado (`Todos`, `Listos`, `Borradores`, `Cancelados`), tarjeta de captura con datos esenciales, estado de sincronización explícito (`Local - NOT_QUEUED`) y modal de cancelación accesible con validación. |
| **NAV-01-B** | Integración del flujo de navegación desde el Hub (`AppScreen.TRAY`) con retorno al panel principal y preservación del estado de sesión activo. |

## Reglas de negocio y privacidad preservadas

1. **Aislamiento de datos por rol:**
   - **Captador:** Solo puede listar, buscar o cancelar capturas asociadas a su propio `ownerId`.
   - **Supervisor:** Puede visualizar las capturas de las sedes autorizadas para supervisión, excluyendo estrictamente los borradores (`DRAFT`) de otros captadores para salvaguardar la privacidad de prospectos no finalizados.
2. **Liberación de claves activas en cancelación:** Al cancelar una captura, se elimina su registro en `active_registration_keys`, permitiendo una nueva captura legítima del mismo RUT en el evento sin colisiones espurias.
3. **Auditoría local inmutable:** Toda cancelación inserta un registro en la tabla `local_audit` con el actor responsable, marca de tiempo y motivo.
4. **Cero transmisiones remotas:** Las tarjetas y estados informan inequívocamente que los registros son locales (`NOT_QUEUED`). No se simulan envíos a la nube ni estados `SYNCED`.

## Pruebas agregadas

- `RecordTrayViewModelTest`: 8 pruebas unitarias en JVM que verifican:
  - Carga inicial y mapeo de capturas a la bandeja.
  - Filtrado reactivo por texto (nombre, apellido, RUT).
  - Filtrado por estado de captura (`READY`, `DRAFT`, `CANCELLED`).
  - Restricción de supervisor (exclusión de borradores).
  - Cancelación exitosa con motivo válido y actualización en vivo del estado.
  - Validación de motivo de cancelación (rechazo de motivos vacíos o menores a 5 caracteres).
  - Manejo de error de almacenamiento sin quiebre de la UI.
  - Paginación de registros mediante cursor.
- Verificación de esquema y migraciones: Aprobados scripts `verify_migration_sql.py` y `verify_context_migration_sql.py`.
