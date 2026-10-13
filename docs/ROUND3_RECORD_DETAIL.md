# Ronda 3 · RECORDS-01-C: detalle y edición local (RF07)

Candidato técnico en `feature/e1-ronda3-record-detail`, sobre el incremento original 290e479 y las correcciones de alta/hub, registros y recursos nativos. Sin merge ni cierre de E1. RECORD_CONTRACTS y la auditoría Room v4 conservan su aceptación humana pendiente.

## Corrección preparada

- `get(id)` conserva lectura F2027 tipada y autorización; LEGACY se informa sin convertirlo a admisión.
- `editContext(id)` prepara un contexto autorizado para el registro: su evento real, texto de consentimiento original, catálogo actual y snapshots seleccionados conservados. Devuelve Available, Unavailable o StorageUnavailable; no usa el evento seleccionado como si fuera el de la captura. Extensión técnica propuesta de RECORD_CONTRACTS, pendiente de aceptación.
- `RecordDetailDialog` reutiliza el formulario F2027 para todos sus campos, carreras/prioridades/menciones y preferencias. No crea otra admisión: guarda mediante `RecordRepository.edit`, cuya transacción se corrigió en la rama de registros.
- Borrador propio puede completarse y confirmar; NEEDS_CONTEXT puede adquirir/confirmar ubicación nueva para READY. El RUT activo permanece bloqueado, también para QR; consentimiento, propietario y contexto original no son editables. La ubicación original no se descarta accidentalmente al retirar una candidata nueva.
- ViewModel retiene editor y motivos sólo en memoria, sin SavedState de datos personales. Campos/acciones y salida se bloquean durante commit. Cancelar el editor retira sus trabajos/listeners.
- Invalid muestra los errores por campo y lleva al primer campo inválido; si es textual, solicita foco. Duplicados propios/restringidos y fallos de almacenamiento conservan lo escrito.
- RevisionConflict conserva editor, motivo y revisión; no recarga ni sobreescribe silenciosamente. Reintento bloqueado hasta recarga explícita con confirmación de descarte. Atrás/salida en edición exige descarte explícito.
- Bandeja conserva la paginación y las correcciones previas. El motivo de anulación también pertenece al ViewModel en el consumidor real, para retención durante recreación.
- El detalle presenta los datos de forma desplazable y botones adaptables; alcance de transporte NOT_QUEUED, sin backend E2.

## Verificación nueva · 11/10/2026

**165 JVM**, ambas APK y Lint **0 errores/25 avisos**. Incluye las doce pruebas originales de bandeja, seis regresiones nuevas de edición y la corrección GPS propagada. Xiaomi 11T Android 14/API 34: **39 cifradas (49.303 s)** sobre ef940a4, concluidas durante el corte; **11 UI (107.106 s)** sobre el build final, en un grupo separado con asistencia MIUI. Tres UI nuevas usan Room cifrado real: completar borrador y confirmar el mismo registro, error/foco/reintento con retirada de notas opcionales, conflicto con descarte explícito. Ocho regresiones de formulario, recursos y bandeja también aprobadas. Fuentes/APK identificadas en [evidencia](evidencias/detail-correction-2026-10-11.json).

La primera ejecución después de la pausa encontró APK instaladas diferentes y clases ausentes; se reinstalaron los builds propios con -r -t y SHA256 verificado. No se toma aquel intento como fallo de producto. Los intentos válidos posteriores detectaron un selector incorrecto del RUT deshabilitado y feedback repetido: corregidos antes de la suite final. Se conservan las pruebas originales; no se omiten aserciones de campos, persistencia o revisión.

Giro real de MainActivity, TalkBack/visual completa, aceptación humana de contrato/v4 y ensayo P3 pendientes. La cámara/GPS físicos fueron comprobados por el usuario en el incremento nativo; no se extrapola ese resultado a todo el recorrido E1. Sin merge ni cierre de RF.
