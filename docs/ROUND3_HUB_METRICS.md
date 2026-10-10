# Ronda 3 · RF09 y RF10: Resumen Operativo Local y Panel de Supervisión en Hub

Incremento local sobre la base de ronda 3 (`feature/e1-ronda3-hub-metrics`). Implementa las métricas operativas y el panel de supervisión local exigidos para la entrega E1 (P2/P3), calculando resúmenes estadísticos directamente desde la base de datos Room cifrada del dispositivo sin requerir servicios de red ni sincronización remota.

## Alcance técnico implementado

| Entregable | Responsabilidad e integración |
|---|---|
| **Contratos de Dominio (RF09 / RF10)** | `DeviceMetrics` y `SiteMetrics` en `RecordContracts.kt`. Modelan agregaciones tipadas de capturas (`totalCaptures`, `readyCount`, `draftCount`, `needsContextCount`, `cancelledCount`) desglosadas por evento y por sede. |
| **Repositorio Room (Persistencia)** | `RoomRecordRepository.getMetrics(eventId)` ejecuta consultas agregadas tipadas en `CaptureDao`: `getMetricsByOwner` para captadores (aislamiento estricto de registros propios) y `getMetricsBySites` para supervisores (agrupación por sede dentro de las sedes autorizadas del evento, excluyendo borradores ajenos). |
| **UI Resumen Operativo (RF09)** | Tarjeta visual en `MainHubScreen` ("Resumen operativo (este dispositivo)"). Muestra tarjetas métricas con conteos en tiempo real: Total, Confirmadas (`READY`), Borradores (`DRAFT`), Sin ubicación (`NEEDS_CONTEXT`) y Anuladas (`CANCELLED`). Incluye aviso explícito de alcance local: *"Alcance: este dispositivo. Datos calculados directamente desde Room cifrado en este equipo. En E1 no se consolidan registros de otros teléfonos ni servidores."* |
| **UI Panel de Supervisión (RF10)** | Sección condicional visible exclusivamente para el rol `SUPERVISOR` en `MainHubScreen`. Presenta el desglose de capturas por cada sede asignada y el número de captadores de terreno asignados al evento activo. |
| **Rutas y Orquestación** | `MainHubRoute` y `MainActivity` conectan `RecordRepository` al Hub principal, actualizando reactivamente las métricas operativas al ingresar a la pantalla o tras guardar/anular/editar registros. |

## Reglas de negocio y privacidad preservadas

1. **Aislamiento por rol:**
   - **Captador:** Solo contabiliza capturas creadas por su propio identificador (`ownerId == actor.id`). No tiene visibilidad sobre capturas realizadas por otros captadores en el mismo evento.
   - **Supervisor:** Contabiliza capturas de todas las sedes asignadas a su perfil dentro del evento, excluyendo estrictamente borradores (`state != 'DRAFT'`) para salvaguardar la privacidad de prospectos no confirmados.
2. **Esquema de base de datos inmutable:**
   - La implementación de las métricas no altera las entidades Room ni genera migraciones de esquema (permanece en versión 3 intacta con hash validado).
3. **Transporte estrictamente local:**
   - Los datos se computan en memoria y SQLite local. No se generan peticiones HTTP ni colas de red.

## Pruebas agregadas y verificadas

- `MainHubMetricsTest`: 3 pruebas unitarias en JVM que validan:
  - Cómputo correcto de totales operativos (`totalCaptures`, confirmadas, borradores, pendientes, anuladas).
  - Desglose por sedes y recuentos para supervisores.
  - Inicialización consistente en cero ante eventos sin registros.
- `RecordTrayViewModelTest`: Actualización de mocks y contratos auxiliares (`FakeRecordRepository`).
- Verificación de esquema SQLite: Scripts `verify_migration_sql.py` y `verify_context_migration_sql.py` ejecutados con 100% éxito.
