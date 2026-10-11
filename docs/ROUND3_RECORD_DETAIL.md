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

## Verificación en curso

La primera ejecución posterior a propagación detectó firmas DAO duplicadas de los incrementos originales y se resolvieron conservando una sola implementación. Build posterior correcto: 157 JVM (incluidas las doce originales de bandeja y seis regresiones nuevas de edición), APK principal/instrumental y Lint sin errores. Las modificaciones finales de catálogo/foco/restauración se incluyeron en un segundo build correcto de 157 JVM, ambas APK y Lint 0 errores/25 avisos. Las ejecuciones Xiaomi siguen pendientes; no se atribuye evidencia Android nueva a este checkpoint local.

Las seis regresiones de `RecordDetailStateTest` comprueban intención de confirmar el mismo ID, preservación ante conflicto/duplicado/error, guardado excluyente, identidad/consentimiento y cancelación de lectura/editor. Se agregan dos casos Room de contexto autorizado y tres recorridos `RecordDetailRoomUiTest` con repositorios reales/base cifrada test-detail-*; todavía pendientes de ejecución en este corte. No se borra la base instalada del usuario.

Pendientes: controles finales y Xiaomi, giro físico de MainActivity, TalkBack/visual completa, aceptación cruzada de contrato/v4 y ensayo P3. El resultado del GPS corregido se registra en la rama nativa por separado. Ninguna prueba con doble equivale a comprobación física.
