package io.github.grupo6.capturaprospectos.ui.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import io.github.grupo6.capturaprospectos.R
import io.github.grupo6.capturaprospectos.domain.capture.*
import io.github.grupo6.capturaprospectos.domain.context.DemoProfile
import io.github.grupo6.capturaprospectos.domain.device.ConnectivityStatus
import io.github.grupo6.capturaprospectos.domain.records.DeviceMetrics
import io.github.grupo6.capturaprospectos.ui.components.*
import java.time.format.DateTimeFormatter
import java.util.Locale

data class MainHubUiState(
    val profile: DemoProfile, val event: CaptureEvent,
    val connectivity: ConnectivityStatus = ConnectivityStatus.UNKNOWN,
    val metrics: DeviceMetrics? = null, val isLoadingMetrics: Boolean = false,
    val metricsError: String? = null, val fromDate: String = "", val untilDate: String = "",
    val showRange: Boolean = false,
)

@Composable fun MainHubScreen(
    state: MainHubUiState, onNewCapture: () -> Unit, onChangeContext: () -> Unit,
    modifier: Modifier = Modifier, onOpenTray: () -> Unit = {}, onRefreshMetrics: () -> Unit = {},
    onCreateEvent: () -> Unit = {}, onEditEvent: () -> Unit = {},
    onFromDate: (String) -> Unit = {}, onUntilDate: (String) -> Unit = {},
    onToday: () -> Unit = {}, onAll: () -> Unit = {}, onRange: () -> Unit = {},
    summaryOnly: Boolean = false, onOpenSummary: () -> Unit = {},
    calendar: VolatileDatePickerController = remember { VolatileDatePickerController() }, onToggleRange: () -> Unit = {},
) {
    val m = state.metrics
    val draft by calendar.draft.collectAsState()
    draft?.let { current ->
        val from = current.target == DatePickerTarget.FROM
        VolatileDatePickerDialog(calendar, if (from) "Fecha desde" else "Fecha hasta inclusive",
            onDismiss = calendar::close, onConfirm = {
                if (from) onFromDate(it) else onUntilDate(it)
                calendar.close()
            })
    }
    LazyColumn(modifier, contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (!summaryOnly) item { ProductIdentity() }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (summaryOnly) "Tu impacto" else "Hola, ${state.profile.displayName}",
                        style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                    Text(if (summaryOnly) state.event.name else "Todo listo para tu jornada.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                }
                if (!summaryOnly) GlassCard(Modifier.size(56.dp)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { DesignIcon(R.drawable.g6_home_frame) }
                }
            }
        }
        if (!summaryOnly) {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("TU JORNADA", style = MaterialTheme.typography.bodySmall)
                        Text(state.event.name, style = MaterialTheme.typography.titleLarge)
                        val start = state.event.startsAt.atZone(state.event.timeZone)
                        val end = state.event.endsAt.atZone(state.event.timeZone)
                        Text("${start.format(DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", Locale.forLanguageTag("es-CL")))} – ${end.format(DateTimeFormatter.ofPattern("d MMM · HH:mm", Locale.forLanguageTag("es-CL")))}",
                            style = MaterialTheme.typography.labelLarge)
                        ConnectivityBanner(state.connectivity)
                    }
                }
            }
            item { Button(onNewCapture, enabled = state.profile.actor.role == LocalRole.CAPTOR,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Nueva captura") } }
            item { Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetricTile(m?.totalCaptures?.toString() ?: "—", "Inscripciones activas", Modifier.weight(1f))
                MetricTile(m?.draftCount?.toString() ?: "—", "Borradores", Modifier.weight(1f))
            } }
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (state.isLoadingMetrics) LinearProgressIndicator(Modifier.fillMaxWidth())
                        state.metricsError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        Text("Tu avance", style = MaterialTheme.typography.titleLarge)
                        if (m == null) Text("Resumen aún no disponible.")
                        else {
                            Text("Personas distintas: ${m.distinctPeople}", style = MaterialTheme.typography.bodySmall)
                            Text("${m.readyCount} listas · ${m.needsContextCount} por completar contexto")
                            LinearProgressIndicator(progress = { if (m.totalCaptures == 0) 0f else m.readyCount.toFloat() / m.totalCaptures },
                                modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
                            if (state.metricsError != null) Text("Último cálculo disponible; revisa su período en Resumen.")
                        }
                        Button(onOpenSummary) { Text("Ver resumen") }
                    }
                }
            }
            item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onOpenTray) { Text("Ver registros") }
                TextButton(onChangeContext) { Text("Cambiar evento") }
            } }
        } else {
            item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(false, onToday, enabled = !state.isLoadingMetrics, label = { Text("Hoy") })
                FilterChip(false, onAll, enabled = !state.isLoadingMetrics, label = { Text("Todo el evento") })
                FilterChip(state.showRange, onToggleRange, label = { Text("Elegir período") },
                    trailingIcon = { DesignIcon(R.drawable.g6_login_frame2) })
                TextButton(onRefreshMetrics, enabled = !state.isLoadingMetrics) { Text("Actualizar") }
            } }
            if (state.showRange) item {
                GlassCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(state.fromDate, onFromDate, label = { Text("Desde (aaaa-mm-dd)") },
                            trailingIcon = { IconButton({ calendar.open(DatePickerTarget.FROM, state.fromDate) }, enabled = !state.isLoadingMetrics) {
                                Icon(painterResource(R.drawable.g6_capture_frame2), "Elegir fecha desde")
                            } }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(state.untilDate, onUntilDate, label = { Text("Hasta inclusive (aaaa-mm-dd)") },
                            trailingIcon = { IconButton({ calendar.open(DatePickerTarget.UNTIL, state.untilDate) }, enabled = !state.isLoadingMetrics) {
                                Icon(painterResource(R.drawable.g6_capture_frame2), "Elegir fecha hasta")
                            } }, modifier = Modifier.fillMaxWidth())
                        OutlinedButton(onRange, enabled = !state.isLoadingMetrics) { Text("Aplicar rango") }
                    }
                }
            }
            item {
                if (state.isLoadingMetrics) LinearProgressIndicator(Modifier.fillMaxWidth())
                state.metricsError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (state.metricsError != null && m != null) Text("Se conserva el último cálculo; revisa su período antes de usarlo.")
                if (m == null && !state.isLoadingMetrics && state.metricsError == null) Text("Métricas aún desconocidas.")
                if (m != null) {
                    Text("Personas distintas: ${m.distinctPeople}", style = MaterialTheme.typography.labelLarge)
                    Text(if (m.fromInclusive == null) "Período calculado: todo el evento" else
                        "Período calculado: ${m.fromInclusive.atZone(state.event.timeZone).toLocalDate()} a ${m.untilExclusive!!.minusNanos(1).atZone(state.event.timeZone).toLocalDate()} (${state.event.timeZone.id})",
                        style = MaterialTheme.typography.bodySmall)
                }
                Text("Registros de este dispositivo.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (m != null) {
                item { Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MetricTile(m.totalCaptures.toString(), "Inscripciones activas", Modifier.weight(1f))
                    MetricTile(m.distinctPeople.toString(), "Personas", Modifier.weight(1f))
                } }
                item { GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Estado de las inscripciones", style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.semantics { heading() })
                        val values = listOf("Confirmadas" to m.readyCount, "Borradores" to m.draftCount,
                            "Falta contexto" to m.needsContextCount, "Anuladas" to m.cancelledCount)
                        val maximum = values.maxOf { it.second }.coerceAtLeast(1)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            values.forEach { (label, count) ->
                                Column(Modifier.weight(1f).semantics(mergeDescendants = true) {},
                                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(count.toString(), style = MaterialTheme.typography.labelLarge)
                                    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.BottomCenter) {
                                        if (count > 0) Box(Modifier.width(30.dp).height((100f * count / maximum).dp)
                                            .clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.primary).clearAndSetSemantics {})
                                    }
                                    Text(label, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                } }
                item { GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Interés por carrera", style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.semantics { heading() })
                        Text("Cada persona puede elegir varias carreras.", style = MaterialTheme.typography.bodySmall)
                        if (m.programMetrics.isEmpty()) Text("Sin intereses en este período.")
                        val maximum = m.programMetrics.maxOfOrNull { it.registrations }?.coerceAtLeast(1) ?: 1
                        m.programMetrics.forEach { program ->
                            Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${program.snapshotName}: ${program.registrations} interés(es)")
                                LinearProgressIndicator(progress = { program.registrations.toFloat() / maximum },
                                    modifier = Modifier.fillMaxWidth().height(12.dp).clearAndSetSemantics {},
                                    color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                            }
                        }
                    }
                } }
            }
        }
        // Panel de Supervisión Local (RF10)
        if (state.profile.actor.role == LocalRole.SUPERVISOR) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            "Panel de Supervisión Local",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            "Supervisión sobre este dispositivo para las sedes autorizadas en tu perfil. Excluye borradores ajenos por privacidad.",
                            style = MaterialTheme.typography.bodySmall,
                        )

                        Text("Distribución por Sedes:", style = MaterialTheme.typography.titleSmall)
                        val siteList = state.metrics?.siteMetrics.orEmpty()
                        if (siteList.isEmpty()) {
                            Text(if (state.metrics == null) "Desglose desconocido; requiere cálculo correcto." else "Sin registros autorizados en este filtro.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            siteList.forEach { s ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(s.siteName, style = MaterialTheme.typography.titleSmall)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Text("Total: ${s.totalCount}", style = MaterialTheme.typography.bodyMedium)
                                            Text("Confirmadas: ${s.readyCount}", style = MaterialTheme.typography.bodySmall)
                                            Text("Anuladas: ${s.cancelledCount}", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text("Equipo en Terreno (Evento):", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${state.event.assignedCaptorIds.size} captador(es) asignados a ${state.event.name}.",
                            style = MaterialTheme.typography.bodySmall,
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text("Administración de Eventos (RF13):", style = MaterialTheme.typography.titleSmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = onEditEvent,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Editar evento")
                            }
                            FilledTonalButton(
                                onClick = onCreateEvent,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Nuevo evento")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun ConnectivityBanner(status: ConnectivityStatus) {
    val label = when (status) {
        ConnectivityStatus.VALIDATED -> "Conexión a internet verificada"
        ConnectivityStatus.CONNECTED_UNVALIDATED -> "Red conectada (sin validación completa)"
        ConnectivityStatus.OFFLINE -> "Sin conexión a red (Modo offline)"
        ConnectivityStatus.UNKNOWN -> "Estado de red desconocido"
    }
    GlassCard(Modifier.fillMaxWidth()) {
        Text(label, Modifier.padding(16.dp), style = MaterialTheme.typography.labelLarge)
    }
}
