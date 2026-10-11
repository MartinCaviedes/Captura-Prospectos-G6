# Ronda 3 · RF13: administración local de eventos

Candidato sobre 020b464 y las correcciones de las rondas anteriores. Sin merge ni cierre E1/RF13; RECORD_CONTRACTS y auditoría Room v4 pendientes de aceptación humana.

El editor usa una copia retenida en DemoContextViewModel para el actor/contexto vigente. Al editar se precargan sede, fechas exactas y zona, coordenadas, colegio/comuna y asignaciones existentes; al crear no se seleccionan sede ni fechas automáticamente. Inicio y fin se introducen explícitamente con offset compatible con la zona; no se reemplazan fechas por ahora + 8/48 horas. Se comprueban nombre, intervalo positivo, par de coordenadas y metadatos coherentes con F2027. Un error conserva la copia. Guardar bloquea campos, salidas y operaciones de contexto; cancelar exige confirmación. Después del éxito se selecciona el evento guardado y se actualiza el contexto antes de cerrar el editor.

RoomDemoContextRepository valida dentro de una misma transacción al supervisor vigente, la sede original de una edición, la sede destino habilitada y el rol/alcance de cada captador asignado. Un ID inexistente o fuera de alcance no se convierte en un alta ni se sobrescribe. Eventos y asignaciones se escriben atómicamente; cancelación se propaga y el fallo revierte todo. Las capturas existentes conservan sus snapshots y revisiones. Retirar una asignación impide nuevas altas/selección al captador sin eliminar capturas previas. Una selección almacenada que dejó de ser accesible no se activa ni se borra automáticamente.

El mismo editor se abre desde selector y hub; errores se muestran una vez en el diálogo. Supervisión sigue siendo exclusivamente local a las sedes del perfil sintético. Sin cuentas reales, API, ACK, reinicialización de catálogos ni nueva migración.

## Corrección y comprobación de administración · 11/10/2026

185 JVM, ambas APK, Lint 0 errores/25 avisos. Xiaomi 11T Android 14/API 34: 47 cifradas (60.545 s) y 11 UI (76.858 s), en grupos separados y con apertura asistida MIUI en UI. Cuatro nuevas de almacenamiento verifican autorización de origen/destino/captadores, preservación histórica, rollback y revocación de asignación. Dos nuevas UI prueban edición desde el hub, fechas multidía explícitas, metadatos, validación y confirmación de descarte. Cuatro nuevas JVM prueban preparación y dos estado/bloqueo de guardado; se conservan las pruebas originales. [Evidencia y APK](evidencias/events-correction-2026-10-11.json).

Se corrigieron una expectativa histórica que omitía la selección tras guardar y un selector UI ambiguo entre la sede del hub y el diálogo. El grupo UI final completo pasó; sin pruebas omitidas ni reducción de controles.

Pendientes: revisión humana cruzada, giro real/TalkBack/visual completa, aceptación contractual y ensayo P3. La copia del editor se retiene en ViewModel; no se serializan datos personales en SavedState.
