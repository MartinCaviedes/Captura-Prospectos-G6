package io.github.grupo6.capturaprospectos.ui.context

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.grupo6.capturaprospectos.domain.admission.SiteOption
import io.github.grupo6.capturaprospectos.domain.capture.CaptureEvent
import io.github.grupo6.capturaprospectos.domain.capture.Coordinates
import io.github.grupo6.capturaprospectos.domain.context.DemoProfile
import io.github.grupo6.capturaprospectos.domain.context.SaveDemoEvent
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventAdminDialog(
    eventToEdit: CaptureEvent? = null,
    availableSites: List<SiteOption>,
    availableCaptors: List<DemoProfile>,
    onDismiss: () -> Unit,
    onSave: (SaveDemoEvent) -> Unit,
) {
    var name by remember { mutableStateOf(eventToEdit?.name ?: "") }
    var selectedSiteId by remember {
        mutableStateOf(eventToEdit?.siteId ?: availableSites.firstOrNull()?.id)
    }
    var establishment by remember { mutableStateOf(eventToEdit?.establishmentSuggestion() ?: "") }
    var establishmentCommune by remember { mutableStateOf("") }
    var selectedCaptorIds by remember {
        mutableStateOf(eventToEdit?.assignedCaptorIds ?: emptySet())
    }

    var isMultiDay by remember {
        val duration = if (eventToEdit != null) {
            ChronoUnit.HOURS.between(eventToEdit.startsAt, eventToEdit.endsAt)
        } else 8L
        mutableStateOf(duration > 24)
    }

    var validationError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = if (eventToEdit == null) "Crear Nuevo Evento (RF13)" else "Editar Evento Demo (RF13)",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                )

                Text(
                    text = "Configuración local para supervisores. No se envían datos al servidor en E1.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                HorizontalDivider()

                // Nombre del evento
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        validationError = null
                    },
                    label = { Text("Nombre del evento *") },
                    placeholder = { Text("Ej: Feria Vocacional Regional 2026") },
                    isError = name.isNotBlank() && name.trim().length < 3,
                    supportingText = {
                        Text("Entre 3 y 100 caracteres. Campo obligatorio.")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                // Selección de Sede
                Text(
                    text = "Sede de asignación *",
                    style = MaterialTheme.typography.titleSmall,
                )
                if (availableSites.isEmpty()) {
                    Text(
                        text = "No tienes sedes autorizadas para administrar eventos.",
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        availableSites.forEach { site ->
                            val isSelected = selectedSiteId == site.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedSiteId = site.id },
                                label = { Text(site.name) },
                            )
                        }
                    }
                }

                // Datos de Establecimiento (Opcional)
                Text(
                    text = "Establecimiento anfitrión (opcional)",
                    style = MaterialTheme.typography.titleSmall,
                )
                OutlinedTextField(
                    value = establishment,
                    onValueChange = { establishment = it },
                    label = { Text("Colegio o institución") },
                    placeholder = { Text("Ej: Liceo Polivalente A-1") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = establishmentCommune,
                    onValueChange = { establishmentCommune = it },
                    label = { Text("Comuna del colegio") },
                    placeholder = { Text("Ej: Viña del Mar") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                // Duración del Evento
                Text(
                    text = "Duración estimada",
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isMultiDay = !isMultiDay },
                ) {
                    Checkbox(
                        checked = isMultiDay,
                        onCheckedChange = { isMultiDay = it },
                    )
                    Text(
                        text = if (isMultiDay) "Evento multidía (48 horas)" else "Evento jornada simple (8 horas)",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                // Asignación de captadores
                Text(
                    text = "Asignación de captadores en terreno",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "Selecciona qué captadores autorizados podrán capturar en este evento:",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (availableCaptors.isEmpty()) {
                    Text(
                        text = "No hay perfiles de captador disponibles en la base demo.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        availableCaptors.forEach { captor ->
                            val isAssigned = captor.actor.id in selectedCaptorIds
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedCaptorIds = if (isAssigned) {
                                            selectedCaptorIds - captor.actor.id
                                        } else {
                                            selectedCaptorIds + captor.actor.id
                                        }
                                    },
                            ) {
                                Checkbox(
                                    checked = isAssigned,
                                    onCheckedChange = { checked ->
                                        selectedCaptorIds = if (checked) {
                                            selectedCaptorIds + captor.actor.id
                                        } else {
                                            selectedCaptorIds - captor.actor.id
                                        }
                                    },
                                )
                                Text(
                                    text = "${captor.displayName} (${captor.actor.role.name})",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }

                // Errores de validación
                validationError?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val trimmedName = name.trim()
                            val site = selectedSiteId
                            if (trimmedName.length !in 3..100) {
                                validationError = "El nombre del evento debe tener entre 3 y 100 caracteres."
                                return@Button
                            }
                            if (site == null) {
                                validationError = "Debes seleccionar una sede válida."
                                return@Button
                            }

                            val now = Instant.now()
                            val durationHours = if (isMultiDay) 48L else 8L
                            val endsAt = now.plus(durationHours, ChronoUnit.HOURS)

                            val cmd = SaveDemoEvent(
                                eventId = eventToEdit?.id,
                                siteId = site,
                                name = trimmedName,
                                startsAt = eventToEdit?.startsAt ?: now,
                                endsAt = eventToEdit?.endsAt ?: endsAt,
                                timeZone = eventToEdit?.timeZone ?: ZoneId.of("America/Santiago"),
                                coordinates = eventToEdit?.coordinates,
                                establishment = establishment.trim().ifBlank { null },
                                establishmentCommune = establishmentCommune.trim().ifBlank { null },
                                assignedCaptorIds = selectedCaptorIds,
                            )
                            onSave(cmd)
                        },
                    ) {
                        Text(if (eventToEdit == null) "Crear evento" else "Guardar cambios")
                    }
                }
            }
        }
    }
}

private fun CaptureEvent.establishmentSuggestion(): String = ""
