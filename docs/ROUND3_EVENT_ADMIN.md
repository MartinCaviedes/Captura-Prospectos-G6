# Ronda 3 · RF13: Administración Local de Eventos y Asignaciones

Incremento local sobre la base de ronda 3 (`feature/e1-ronda3-event-admin`). Cierra el ciclo de administración de contexto y gobernanza de eventos demo exigido para la entrega E1 (P2/P3), permitiendo a perfiles de supervisión crear y editar eventos, programar fechas y asignar captadores de terreno de forma puramente local en Room cifrado.

## Alcance técnico implementado

| Entregable | Responsabilidad e integración |
|---|---|
| **Contratos de Dominio (RF13)** | `SaveDemoEvent` y `SaveEventResult` en `DemoContext.kt`. Soporte de identificador de evento (creación/edición), sede autorizada, nombre descriptivo, instante inicio y término, zona horaria, establecimiento anfitrión y conjunto de captadores asignados. |
| **Acceso y Autorización** | `LocalCaptureAccess.canSupervise` y `LocalCaptureAccess.canAccessEvent` en `CaptureRepository.kt`. Permite a supervisores seleccionar y supervisar eventos de sus sedes asignadas, manteniendo la restricción de que solo captadores asignados pueden crear prospectos de admisión (`canCreate`). |
| **Persistencia Room v3** | `ContextDao` y `RoomDemoContextRepository.saveEvent`: Transacción atómica que actualiza `demo_events` y sincroniza `demo_assignments` (reemplazo seguro de asignaciones sin infringir claves foráneas). Mantiene intactas las capturas anteriores y notifica reactivamente a los observadores de contexto. |
| **UI de Administración (RF13)** | `EventAdminDialog` en Compose con Material 3. Formulario accesible con validación en vivo de nombre (3..100 caracteres), selección de sede autorizada, duración simple o multidía (8h / 48h), colegio anfitrión y selección múltiple de captadores asignados mediante checkboxes. |
| **Integración en Navegación y Hub** | Apertura del diálogo de administración desde el selector de contexto (`DemoContextScreen` y `DemoContextRoute`) y desde el panel de supervisión en el Hub principal (`MainHubScreen` y `MainHubRoute`). |

## Reglas de negocio y privacidad preservadas

1. **Aislamiento por rol y sede:**
   - Sólo un usuario con perfil `SUPERVISOR` puede crear o editar eventos.
   - El supervisor únicamente puede gestionar eventos para sedes que pertenecen a su alcance asignado (`command.siteId in actor.siteIds`).
2. **Inmutabilidad del contexto histórico:**
   - La edición o reasignación de captadores en un evento no altera las capturas previas registradas en `captures`.
3. **Consistencia de asignaciones de captadores:**
   - Los captadores solo pueden visualizar y capturar prospectos en eventos a los que han sido explícitamente asignados por un supervisor.
4. **Transporte estrictamente local:**
   - Toda la administración se ejecuta dentro de la base de datos SQLite/Room cifrada del dispositivo, sin interactuar con servicios remotos en E1.

## Pruebas agregadas y verificadas

- `EventAdminTest`: 3 pruebas unitarias en JVM que validan:
  - Comando de creación de evento y preservación de campos (sede, nombre, fechas, coordenadas, colegio, captadores).
  - Comando de edición de eventos existentes y soporte de eventos multidía.
  - Variantes de resultado tipado (`Success`, `Unauthorized`, `Invalid`, `StorageUnavailable`).
- `LocalCaptureAccessTest`: Pruebas de acceso para supervisores (`canSupervise`) y diferenciación de roles en eventos (`canAccessEvent`).
- `DemoContextViewModelTest`: Pruebas de delegación, guardado exitoso y retroalimentación ante denegación de permisos.
- `ContextSessionOwnerTest`: Actualización de contratos en dobles de prueba.
- Verificación de esquema SQLite: Scripts `verify_migration_sql.py` y `verify_context_migration_sql.py` ejecutados con 100% de éxito.
