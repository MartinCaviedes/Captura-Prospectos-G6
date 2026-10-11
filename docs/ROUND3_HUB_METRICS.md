# Ronda 3 · RF09/RF10: métricas locales por evento y período

Candidato en feature/e1-ronda3-hub-metrics, sobre el original 98f733d y las correcciones comprobadas previas. Sin merge ni cierre E1/RF. Contrato RECORD_CONTRACTS y auditoría v4 pendientes de aceptación humana.

MetricsQuery contiene evento y límites UTC inclusivo/exclusivo; MetricsResult separa Calculated, InvalidQuery, Unavailable y StorageUnavailable. Se consulta el actor vigente y la sede del evento dentro de la transacción, con el mismo alcance de filas que registros: captador propio y sede vigente, supervisor sus sedes sin borradores ajenos. Un evento desconocido o fuera de alcance no revela datos. Cancelación se propaga; errores de lectura/fecha/estado no se convierten en cero.

Inscripciones activas son READY + NEEDS_CONTEXT. Borradores/anuladas se presentan aparte; personas distintas = RUT normalizado dentro del filtro. Cada carrera cuenta una vez por inscripción activa, con snapshot de etiqueta conservado, por lo que intereses pueden superar personas. El desglose usa nombres de sede del catálogo sintético. Una persona en otro evento se cuenta sólo al consultar ese evento. Las agregaciones se calculan en Kotlin sobre consultas Room cifradas autorizadas; no se afirma agregación SQL optimizada.

El hub permite hoy, todo el evento y rango inclusive de fechas locales en la zona del evento; convierte el fin al inicio del día siguiente, incluyendo días de 23/25 h. Fecha inválida/invertida no consulta ni permite que una lectura anterior quite su error. El último cálculo se mantiene con aviso visible ante fallo; acceso perdido lo retira. El ViewModel se retiene por actor completo/evento y el hub actualiza al reingresar, tras volver de altas/ediciones/anulaciones.

Supervisor puede seleccionar explícitamente un evento de sus sedes e ir al panel local; esto no concede permiso de alta. No se muestran reportes inventados de otros teléfonos, consolidación remota ni ACK. Ninguna migración adicional: se consume el candidato v4 ya propagado.

## Corrección y verificación de métricas · 11/10/2026

**172 JVM**, ambas APK y Lint **0 errores/25 avisos**. Incluye cuatro regresiones nuevas de fechas/zona/DST, rango inválido que cancela una lectura, error con último cálculo y cancelación por dueño. Xiaomi 11T Android 14/API 34: **43 cifradas (55.885 s)** y **9 UI (54.032 s)** en grupos separados; asistencia MIUI en UI. Cuatro nuevas de Room verifican conteos/rango/eventos/intereses, alcance de captador/supervisor, almacenamiento corrupto y cancelación. Dos UI nuevas usan Room cifrado real para supervisor sin alta y actualización tras anulación/rango inválido; seis del selector y una de contexto+Room son regresiones. [Fuentes/APK/runners](evidencias/metrics-correction-2026-10-11.json).

El primer control cifrado rechazó un RUT ficticio con DV incorrecto en la preparación; se corrigió el fixture y el grupo final completo pasó. Lint detectó uso de ViewModelStore.put en código de producción; se sustituyó por ViewModelProvider/fábrica pública, conservando el control. Sin tests omitidos ni baseline nueva.

Pendientes: rendimiento de 400 con método/dispositivo documentados, revisión cruzada, giro real, TalkBack/visual completa y ensayo P3. La supervisión remota permanece en E2.
