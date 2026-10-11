package io.github.grupo6.capturaprospectos.ui.context

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.grupo6.capturaprospectos.domain.capture.LocalRole
import io.github.grupo6.capturaprospectos.domain.context.DemoContextState
import io.github.grupo6.capturaprospectos.ui.theme.CapturaProspectosG6Theme
import java.util.UUID

/** El consumidor confirma cambios con un nuevo estado; un clic no simula persistencia. */
@Composable
fun DemoContextScreen(
    state: DemoContextUiState,
    onInitialize: () -> Unit,
    onRetry: () -> Unit,
    onProfileSelected: (UUID) -> Unit,
    onEventSelected: (UUID) -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier,
    onContinueToHub: () -> Unit = {},
    onCreateEvent: () -> Unit = {},
    showProfiles: Boolean = true,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Captura de prospectos", style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() })
                Text("Demo local · Grupo 6", style = MaterialTheme.typography.titleMedium)
                Text("Perfiles y eventos ficticios. Seleccionar un perfil no autentica tu identidad.")
                Text("Envío no habilitado en esta entrega", style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (state.busy) item {
            Text("Procesando operación local…", modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        state.message?.takeIf { state.eventEditor == null }?.let { message -> item {
            Text(message, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            if (state.context != DemoContextState.StorageUnavailable) {
                TextButton(onClick = onRetry, enabled = !state.busy) { Text("Reintentar lectura") }
            }
        } }
        when (val context = state.context) {
            null -> item {
                Text("Cargando contexto…", modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            DemoContextState.NotInitialized -> item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Preparar la demostración", style = MaterialTheme.typography.titleLarge)
                    Text("Esta acción crea el contexto ficticio. No crea prospectos ni selecciona un perfil por ti.")
                    Button(onClick = onInitialize, enabled = !state.busy,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary)) {
                        Text("Preparar demo local")
                    }
                }
            }
            DemoContextState.StorageUnavailable -> item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("No se pudo leer el contexto local", style = MaterialTheme.typography.titleLarge)
                    Text("Los datos se conservan. Reintenta la lectura; si el problema continúa, solicita revisión sin borrar la aplicación.")
                    OutlinedButton(onClick = onRetry, enabled = !state.busy) { Text("Reintentar lectura") }
                }
            }
            is DemoContextState.Ready -> {
                val snapshot = context.snapshot
                if (showProfiles) item { Text("1. Selecciona un perfil", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() }) }
                if (snapshot.profiles.isEmpty()) item { Text("No hay perfiles disponibles. Solicita revisión del contexto.") }
                if (showProfiles) items(snapshot.profiles, key = { "profile-${it.actor.id}" }) { profile ->
                    ContextChoice(profile.displayName, snapshot.currentProfile?.actor?.id == profile.actor.id,
                        enabled = !state.busy, onClick = { onProfileSelected(profile.actor.id) })
                }
                when {
                    snapshot.currentProfile == null -> item { Text("Elige un perfil para consultar sus eventos.") }
                    snapshot.currentProfile.actor.role == LocalRole.SUPERVISOR -> {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            ) {
                                Text("2. Evento para supervisar", style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.semantics { heading() })
                                FilledTonalButton(onClick = onCreateEvent, enabled = !state.busy) {
                                    Text("Nuevo evento")
                                }
                            }
                        }
                        if (snapshot.availableEvents.isEmpty()) item {
                            Text("No hay eventos configurados para tus sedes. Pulsa 'Nuevo evento' para crear uno.")
                        }
                        items(snapshot.availableEvents, key = { "event-${it.id}" }) { event ->
                            ContextChoice(event.name, snapshot.selectedEvent?.id == event.id,
                                enabled = !state.busy, onClick = { onEventSelected(event.id) })
                        }
                        snapshot.selectedEvent?.let { event -> item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Evento en supervisión: ${event.name}.", style = MaterialTheme.typography.bodyMedium)
                                Button(
                                    onClick = onContinueToHub,
                                    enabled = !state.busy,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("Ir al panel de supervisión")
                                }
                            }
                        } }
                    }
                    else -> {
                        item { Text("2. Selecciona un evento", style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.semantics { heading() }) }
                        if (snapshot.availableEvents.isEmpty()) item { Text("Este perfil no tiene eventos disponibles para capturar.") }
                        items(snapshot.availableEvents, key = { "event-${it.id}" }) { event ->
                            ContextChoice(event.name, snapshot.selectedEvent?.id == event.id,
                                enabled = !state.busy, onClick = { onEventSelected(event.id) })
                        }
                        snapshot.selectedEvent?.let { event -> item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Contexto seleccionado: ${event.name}.", style = MaterialTheme.typography.bodyMedium)
                                Button(
                                    onClick = onContinueToHub,
                                    enabled = !state.busy,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("Ir al panel de captura")
                                }
                            }
                        } }
                    }
                }
                if (showProfiles && snapshot.currentProfile != null) item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onClearSelection, enabled = !state.busy) { Text("Salir de la selección") }
                        Text("Salir conserva los registros guardados en este dispositivo.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ContextChoice(label: String, isSelected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled,
        modifier = Modifier.fillMaxWidth().semantics { selected = isSelected; role = Role.RadioButton },
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
    ) {
        Text(if (isSelected) "$label · Seleccionado" else label, modifier = Modifier.padding(vertical = 4.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun InitialContextPreview() {
    CapturaProspectosG6Theme {
        Surface { DemoContextScreen(DemoContextUiState(DemoContextState.NotInitialized), {}, {}, {}, {}, {}) }
    }
}

@Preview(showBackground = true, fontScale = 1.5f)
@Composable
private fun UnavailableContextPreview() {
    CapturaProspectosG6Theme(darkTheme = true) {
        Surface { DemoContextScreen(DemoContextUiState(DemoContextState.StorageUnavailable), {}, {}, {}, {}, {}) }
    }
}
