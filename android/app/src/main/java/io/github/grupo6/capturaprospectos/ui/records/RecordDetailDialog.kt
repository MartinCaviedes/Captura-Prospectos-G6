package io.github.grupo6.capturaprospectos.ui.records

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.grupo6.capturaprospectos.domain.admission.StoredAdmission
import io.github.grupo6.capturaprospectos.domain.capture.*
import io.github.grupo6.capturaprospectos.ui.admission.AdmissionFormRoute

/** La ficha conserva lectura; la edición usa el mismo formulario F2027 y estado retenido. */
@Composable
fun RecordDetailDialog(
    admission: StoredAdmission,
    state: RecordTrayUiState,
    onClose: () -> Unit,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEditor: (SaveIntent) -> Unit,
    onEditReason: (String) -> Unit,
    onReload: () -> Unit,
    onCancelRecord: () -> Unit,
) {
    Dialog(onDismissRequest = { if (!state.isMutating) onClose() },
        properties = DialogProperties(usePlatformDefaultWidth = false,
            dismissOnBackPress = !state.isMutating, dismissOnClickOutside = !state.isMutating)) {
        Surface(Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.95f), shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ficha · Rev #${admission.revision}", style = MaterialTheme.typography.titleMedium)
                StateBadge(admission.state)
                Text("Transporte local: ${admission.transportState.name} · Sin envío remoto en E1")
                state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (state.isEditing) {
                    OutlinedTextField(state.editReason, onEditReason,
                        label = { Text("Motivo (5–300 caracteres; obligatorio al editar un registro ajeno)") },
                        enabled = !state.isMutating, modifier = Modifier.fillMaxWidth())
                    if (state.editConflict) OutlinedButton(onClick = onReload, enabled = !state.isMutating) {
                        Text("Recargar ficha y descartar cambios…")
                    }
                    val editor = state.editForm
                    if (editor != null) AdmissionFormRoute(editor, Modifier.weight(1f).fillMaxWidth(),
                        onNavigateBack = onCancelEdit, onSaveEdit = onSaveEditor, canSaveEdit = !state.editConflict)
                    else {
                        if (state.isReadingDetail) LinearProgressIndicator(Modifier.fillMaxWidth())
                        TextButton(onClick = onCancelEdit, enabled = !state.isMutating) { Text("Volver a la ficha") }
                    }
                } else {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        DetailItem("Nombre", admission.fullNameSnapshot)
                        DetailItem("RUT", admission.input.rut)
                        DetailItem("Evento", admission.eventName)
                        DetailItem("Capturado en", admission.capturedAt.toString())
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
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onClose, enabled = !state.isMutating) { Text("Volver a bandeja") }
                        if (admission.state != CaptureState.CANCELLED) {
                            TextButton(onClick = onCancelRecord, enabled = !state.isMutating) { Text("Anular") }
                            Button(onClick = onStartEdit, enabled = !state.isMutating && !state.isReadingDetail) { Text("Editar ficha") }
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
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
