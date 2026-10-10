package io.github.grupo6.capturaprospectos.ui.records

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.grupo6.capturaprospectos.domain.admission.AdmissionInput
import io.github.grupo6.capturaprospectos.domain.admission.StoredAdmission
import io.github.grupo6.capturaprospectos.domain.capture.AcceptanceMode
import io.github.grupo6.capturaprospectos.domain.capture.CaptureLocation
import io.github.grupo6.capturaprospectos.domain.capture.CaptureState

@Composable
fun RecordDetailDialog(
    admission: StoredAdmission,
    isEditing: Boolean,
    isSaving: Boolean,
    onClose: () -> Unit,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: (AdmissionInput, String?) -> Unit,
    onCancelRecord: () -> Unit,
) {
    var emailInput by remember(admission.id, isEditing) { mutableStateOf(admission.input.email) }
    var phoneInput by remember(admission.id, isEditing) { mutableStateOf(admission.input.phone) }
    var residenceInput by remember(admission.id, isEditing) { mutableStateOf(admission.input.residenceCommune) }
    var establishmentInput by remember(admission.id, isEditing) { mutableStateOf(admission.input.establishment.orEmpty()) }
    var notesInput by remember(admission.id, isEditing) { mutableStateOf(admission.input.notes.orEmpty()) }
    var reasonInput by remember(admission.id, isEditing) { mutableStateOf("") }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            ) {
                // Encabezado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = admission.fullNameSnapshot,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = "RUT: ${admission.input.rut} · Rev #${admission.revision}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    StateBadge(admission.state)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Banner de estado de transporte local
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Transporte: Local en este dispositivo (${admission.transportState.name}) · Sin envío remoto en E1",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Contenido desplazable
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (isEditing) {
                        Text("Edición controlada de la ficha", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Puedes modificar datos de contacto y establecimiento. El RUT y el consentimiento original permanecen inmutables.",
                            style = MaterialTheme.typography.bodySmall,
                        )

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Correo electrónico") },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = { Text("Teléfono (+56)") },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        OutlinedTextField(
                            value = residenceInput,
                            onValueChange = { residenceInput = it },
                            label = { Text("Comuna de residencia") },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        OutlinedTextField(
                            value = establishmentInput,
                            onValueChange = { establishmentInput = it },
                            label = { Text("Establecimiento") },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        OutlinedTextField(
                            value = notesInput,
                            onValueChange = { notesInput = it },
                            label = { Text("Notas") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                        )

                        OutlinedTextField(
                            value = reasonInput,
                            onValueChange = { reasonInput = it },
                            label = { Text("Motivo de edición (opcional si es propio, 5-300 si supervisor)") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                        )
                    } else {
                        // Vista Detallada de Campos
                        SectionHeader("Datos de Contacto y Personales")
                        DetailItem("Correo", admission.input.email)
                        DetailItem("Teléfono", admission.input.phone)
                        DetailItem("Fecha de Nacimiento", admission.input.birthDate)
                        DetailItem("Comuna de Residencia", admission.input.residenceCommune)
                        admission.input.formPersonalOption?.let {
                            DetailItem("Opción Personal", it.name)
                        }

                        SectionHeader("Datos Académicos y Actividad")
                        DetailItem("Actividad Actual", admission.input.currentActivity?.name ?: "No indicada")
                        admission.input.currentActivityOther?.let {
                            DetailItem("Detalle Otra Actividad", it)
                        }
                        DetailItem("Establecimiento", admission.input.establishment ?: "Sin establecimiento")
                        admission.input.establishmentCommune?.let {
                            DetailItem("Comuna Establecimiento", it)
                        }

                        SectionHeader("Preferencias e Intereses")
                        admission.input.preferredSchedule?.let {
                            DetailItem("Jornada Preferida", it.name)
                        }
                        admission.input.interestLevel?.let {
                            DetailItem("Nivel de Interés", it.name)
                        }
                        admission.input.motivation?.let {
                            DetailItem("Motivación", it.name)
                        }

                        Text("Carreras Priorizadas (hasta 3):", style = MaterialTheme.typography.titleSmall)
                        if (admission.programs.isEmpty()) {
                            Text("Sin carreras seleccionadas", style = MaterialTheme.typography.bodySmall)
                        } else {
                            admission.programs.forEach { p ->
                                val mentionText = if (p.mentionName != null) " (Mención: ${p.mentionName})" else ""
                                Text(
                                    "${p.priority}. ${p.name}$mentionText",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }

                        if (admission.sites.isNotEmpty()) {
                            Text("Sedes de Interés:", style = MaterialTheme.typography.titleSmall)
                            Text(
                                admission.sites.joinToString(", ") { it.name },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }

                        admission.input.notes?.takeIf { it.isNotBlank() }?.let {
                            SectionHeader("Notas")
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }

                        SectionHeader("Ubicación y Contexto de Captura")
                        when (val loc = admission.location) {
                            is CaptureLocation.Device -> {
                                DetailItem(
                                    "Tipo de Ubicación",
                                    "GPS Nativo (${loc.coordinates.latitude}, ${loc.coordinates.longitude}) · Precisión: ${loc.accuracyMeters.toInt()}m",
                                )
                                DetailItem("Instante de Medición", loc.measuredAt.toString())
                            }
                            is CaptureLocation.EventConfirmed -> {
                                DetailItem(
                                    "Tipo de Ubicación",
                                    "Confirmada por Evento (${loc.coordinates.latitude}, ${loc.coordinates.longitude})",
                                )
                                DetailItem("Confirmada a las", loc.confirmedAt.toString())
                            }
                            null -> {
                                Text("Sin ubicación capturada (NEEDS_CONTEXT)", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        SectionHeader("Consentimiento Informado")
                        DetailItem("Versión", admission.consent.version)
                        DetailItem("Modalidad", if (admission.consent.mode == AcceptanceMode.DIRECT) "Directa" else "Verbal registrada")
                        DetailItem("Aceptado en", admission.consent.acceptedAt.toString())
                        DetailItem("Registrado por Actor ID", admission.consent.recordedBy.toString())
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botones de acción inferiores
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isEditing) {
                        TextButton(onClick = onCancelEdit, enabled = !isSaving) {
                            Text("Cancelar edición")
                        }
                        Button(
                            onClick = {
                                val updatedInput = admission.input.copy(
                                    email = emailInput.trim(),
                                    phone = phoneInput.trim(),
                                    residenceCommune = residenceInput.trim(),
                                    establishment = establishmentInput.trim().ifBlank { null },
                                    notes = notesInput.trim().ifBlank { null },
                                )
                                onSaveEdit(updatedInput, reasonInput.trim().ifBlank { null })
                            },
                            enabled = !isSaving,
                        ) {
                            Text(if (isSaving) "Guardando…" else "Guardar cambios")
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onClose) {
                                Text("Volver a bandeja")
                            }
                            if (admission.state != CaptureState.CANCELLED) {
                                TextButton(onClick = onCancelRecord) {
                                    Text("Anular", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        if (admission.state != CaptureState.CANCELLED) {
                            Button(onClick = onStartEdit) {
                                Text("Editar ficha")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun StateBadge(state: CaptureState) {
    val (label, containerColor, contentColor) = when (state) {
        CaptureState.READY -> Triple("CONFIRMADA", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        CaptureState.DRAFT -> Triple("BORRADOR", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        CaptureState.NEEDS_CONTEXT -> Triple("FALTA CONTEXTO", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        CaptureState.CANCELLED -> Triple("ANULADA", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
    }

    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
