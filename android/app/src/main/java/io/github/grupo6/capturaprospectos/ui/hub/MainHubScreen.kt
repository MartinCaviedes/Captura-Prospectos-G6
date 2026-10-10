package io.github.grupo6.capturaprospectos.ui.hub

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.grupo6.capturaprospectos.domain.capture.CaptureEvent
import io.github.grupo6.capturaprospectos.domain.capture.LocalActor
import io.github.grupo6.capturaprospectos.domain.capture.LocalRole
import io.github.grupo6.capturaprospectos.domain.context.DemoProfile
import io.github.grupo6.capturaprospectos.domain.device.ConnectivityStatus
import io.github.grupo6.capturaprospectos.ui.theme.CapturaProspectosG6Theme
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

data class MainHubUiState(
    val profile: DemoProfile,
    val event: CaptureEvent,
    val connectivity: ConnectivityStatus = ConnectivityStatus.UNKNOWN,
)

@Composable
fun MainHubScreen(
    state: MainHubUiState,
    onNewCapture: () -> Unit,
    onChangeContext: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenTray: () -> Unit = {},
) {
    var showTrayDialog by remember { mutableStateOf(false) }

    if (showTrayDialog) {
        AlertDialog(
            onDismissRequest = { showTrayDialog = false },
            title = { Text("Bandeja de Capturas") },
            text = {
                Text(
                    "La bandeja de consulta, edición y anulación está programada para la siguiente tarea (RECORDS-01-B). " +
                        "Todas las inscripciones y borradores se guardan con seguridad en la base de datos local cifrada de este dispositivo."
                )
            },
            confirmButton = {
                TextButton(onClick = { showTrayDialog = false }) {
                    Text("Entendido")
                }
            }
        )
    }

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
                    "Demostración local — Ronda 2",
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Estado de Red: $statusText",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "Aviso E1: Cero transmisiones remotas. La detección de conectividad es informativa y no genera sincronización automática en esta entrega.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MainHubPreview() {
    CapturaProspectosG6Theme {
        Surface {
            val actor = LocalActor(UUID.randomUUID(), LocalRole.CAPTOR, emptySet())
            val profile = DemoProfile(actor, "Captador Demo")
            val event = CaptureEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Feria Vocacional 2026",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                ZoneId.of("America/Santiago"),
                emptySet(),
                null,
            )
            MainHubScreen(
                state = MainHubUiState(profile, event, ConnectivityStatus.VALIDATED),
                onNewCapture = {},
                onChangeContext = {},
            )
        }
    }
}

@Preview(showBackground = true, fontScale = 1.5f)
@Composable
private fun MainHubDarkPreview() {
    CapturaProspectosG6Theme(darkTheme = true) {
        Surface {
            val actor = LocalActor(UUID.randomUUID(), LocalRole.CAPTOR, emptySet())
            val profile = DemoProfile(actor, "Captador Demo")
            val event = CaptureEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Feria Vocacional 2026",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                ZoneId.of("America/Santiago"),
                emptySet(),
                null,
            )
            MainHubScreen(
                state = MainHubUiState(profile, event, ConnectivityStatus.OFFLINE),
                onNewCapture = {},
                onChangeContext = {},
            )
        }
    }
}
