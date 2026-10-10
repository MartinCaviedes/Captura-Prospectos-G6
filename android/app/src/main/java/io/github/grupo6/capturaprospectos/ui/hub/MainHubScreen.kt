package io.github.grupo6.capturaprospectos.ui.hub

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.grupo6.capturaprospectos.domain.capture.CaptureEvent
import io.github.grupo6.capturaprospectos.domain.capture.LocalActor
import io.github.grupo6.capturaprospectos.domain.capture.LocalRole
import io.github.grupo6.capturaprospectos.domain.context.DemoProfile
import io.github.grupo6.capturaprospectos.domain.device.ConnectivityStatus
import io.github.grupo6.capturaprospectos.domain.records.DeviceMetrics
import io.github.grupo6.capturaprospectos.domain.records.SiteMetrics
import io.github.grupo6.capturaprospectos.ui.theme.CapturaProspectosG6Theme
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

data class MainHubUiState(
    val profile: DemoProfile,
    val event: CaptureEvent,
    val connectivity: ConnectivityStatus = ConnectivityStatus.UNKNOWN,
    val metrics: DeviceMetrics? = null,
    val isLoadingMetrics: Boolean = false,
)

@Composable
fun MainHubScreen(
    state: MainHubUiState,
    onNewCapture: () -> Unit,
    onChangeContext: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenTray: () -> Unit = {},
    onRefreshMetrics: () -> Unit = {},
    onCreateEvent: () -> Unit = {},
    onEditEvent: () -> Unit = {},
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(24.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Captura de Prospectos",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    "Duoc UC · Grupo 6",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Demostración local — Entrega 1",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Contexto de Captura Activo",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text("Usuario: ${state.profile.displayName}")
                    val roleLabel = if (state.profile.actor.role == LocalRole.SUPERVISOR) "Supervisor" else "Captador en terreno"
                    Text("Rol: $roleLabel", style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Evento: ${state.event.name}", style = MaterialTheme.typography.bodyLarge)
                    Text("Zona horaria: ${state.event.timeZone.id}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            ConnectivityBanner(state.connectivity)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Operaciones",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    Button(
                        onClick = onNewCapture,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Nueva Captura de Prospecto")
                    }
                    OutlinedButton(
                        onClick = onOpenTray,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Bandeja de Registros Locales")
                    }
                    OutlinedButton(
                        onClick = onChangeContext,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Cambiar Evento o Perfil")
                    }
                }
            }
        }

        // Resumen Operativo Local (RF09)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Resumen operativo (este dispositivo)",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.semantics { heading() },
                        )
                        TextButton(onClick = onRefreshMetrics) {
                            Text("Actualizar")
                        }
                    }

                    Text(
                        "Métricas calculadas exclusivamente sobre datos almacenados en este equipo. En E1 no se consolidan registros de otros teléfonos ni servidores.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (state.isLoadingMetrics) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    val m = state.metrics
                    if (m != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            MetricStatCard("Total", "${m.totalCaptures}", MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.weight(1f))
                            MetricStatCard("Confirmadas", "${m.readyCount}", MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.weight(1f))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            MetricStatCard("Borradores", "${m.draftCount}", MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.weight(1f))
                            MetricStatCard("Falta contexto", "${m.needsContextCount}", MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.weight(1f))
                            MetricStatCard("Anuladas", "${m.cancelledCount}", MaterialTheme.colorScheme.errorContainer, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Panel de Supervisión Local (RF10)
        if (state.profile.actor.role == LocalRole.SUPERVISOR) {
            item {
                Card(
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
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            "Supervisión sobre este dispositivo para las sedes autorizadas en tu perfil. Excluye borradores ajenos por privacidad.",
                            style = MaterialTheme.typography.bodySmall,
                        )

                        Text("Distribución por Sedes:", style = MaterialTheme.typography.titleSmall)
                        val siteList = state.metrics?.siteMetrics.orEmpty()
                        if (siteList.isEmpty()) {
                            Text("No hay sedes con capturas registradas en este evento todavía.", style = MaterialTheme.typography.bodySmall)
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

@Composable
private fun MetricStatCard(
    label: String,
    value: String,
    containerColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ConnectivityBanner(status: ConnectivityStatus) {
    val (statusText, subtitle, containerColor) = when (status) {
        ConnectivityStatus.VALIDATED -> Triple(
            "Conexión a internet verificada",
            "Red disponible. Los datos se conservan en este teléfono; el envío remoto se activará en la entrega E2.",
            MaterialTheme.colorScheme.primaryContainer,
        )
        ConnectivityStatus.CONNECTED_UNVALIDATED -> Triple(
            "Red conectada (sin validación completa)",
            "Conexión detectada sin confirmación de salida a internet. Modo local activo.",
            MaterialTheme.colorScheme.secondaryContainer,
        )
        ConnectivityStatus.OFFLINE -> Triple(
            "Sin conexión a red (Modo offline)",
            "La aplicación opera normalmente guardando de forma cifrada en este dispositivo.",
            MaterialTheme.colorScheme.surfaceVariant,
        )
        ConnectivityStatus.UNKNOWN -> Triple(
            "Estado de red desconocido",
            "Consultando estado de red del sistema operativo.",
            MaterialTheme.colorScheme.surfaceVariant,
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Detección de conectividad (E1)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = status.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MainHubPreview() {
    CapturaProspectosG6Theme {
        Surface {
            MainHubScreen(
                state = MainHubUiState(
                    profile = DemoProfile(
                        actor = LocalActor(UUID.randomUUID(), LocalRole.CAPTOR, emptySet()),
                        displayName = "Juan Captador",
                        activeEventId = UUID.randomUUID(),
                    ),
                    event = CaptureEvent(
                        id = UUID.randomUUID(),
                        siteId = UUID.randomUUID(),
                        name = "Feria Central 2026",
                        startUtc = Instant.now(),
                        endUtc = Instant.now().plusSeconds(3600),
                        timeZone = ZoneId.of("America/Santiago"),
                        assignedCaptorIds = setOf(UUID.randomUUID()),
                    ),
                    connectivity = ConnectivityStatus.VALIDATED,
                    metrics = DeviceMetrics(
                        eventId = UUID.randomUUID(),
                        totalCaptures = 15,
                        readyCount = 10,
                        draftCount = 3,
                        needsContextCount = 1,
                        cancelledCount = 1,
                    ),
                ),
                onNewCapture = {},
                onChangeContext = {},
            )
        }
    }
}
