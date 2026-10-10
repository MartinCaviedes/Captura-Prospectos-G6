package io.github.grupo6.capturaprospectos.ui.records

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.grupo6.capturaprospectos.domain.admission.AdmissionInput
import io.github.grupo6.capturaprospectos.domain.capture.CaptureState
import io.github.grupo6.capturaprospectos.domain.records.RecordContract
import io.github.grupo6.capturaprospectos.domain.records.RecordSummary
import io.github.grupo6.capturaprospectos.ui.theme.CapturaProspectosG6Theme
import java.time.Instant
import java.util.UUID

@Composable
fun RecordTrayScreen(
    state: RecordTrayUiState,
    onQueryChanged: (String) -> Unit,
    onStateFilterSelected: (CaptureState?) -> Unit,
    onOpenCancel: (RecordSummary) -> Unit,
    onCloseCancel: () -> Unit,
    onConfirmCancel: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenDetail: (UUID) -> Unit = {},
    onCloseDetail: () -> Unit = {},
    onStartEdit: () -> Unit = {},
    onCancelEdit: () -> Unit = {},
    onSaveEdit: (AdmissionInput, String?) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    var reasonInput by remember { mutableStateOf("") }

    // Modal de Detalle y Edición de Ficha (RF07)
    if (state.isDetailOpen && state.selectedRecord != null) {
        RecordDetailDialog(
            admission = state.selectedRecord,
            isEditing = state.isEditing,
            isSaving = state.isSavingEdit,
            onClose = onCloseDetail,
            onStartEdit = onStartEdit,
            onCancelEdit = onCancelEdit,
            onSaveEdit = onSaveEdit,
            onCancelRecord = {
                val sum = state.items.firstOrNull { it.id == state.selectedRecord.id }
                    ?: RecordSummary(
                        id = state.selectedRecord.id,
                        revision = state.selectedRecord.revision,
                        capturedAt = state.selectedRecord.capturedAt,
                        eventId = state.selectedRecord.eventId,
                        displayName = state.selectedRecord.fullNameSnapshot,
                        rut = state.selectedRecord.input.rut,
                        state = state.selectedRecord.state,
                        contract = RecordContract.F2027,
                    )
                onOpenCancel(sum)
            },
        )
    }

    if (state.targetForCancellation != null) {
        AlertDialog(
            onDismissRequest = onCloseCancel,
            title = { Text("Anular Inscripción") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Se anulará el registro de ${state.targetForCancellation.displayName} (${state.targetForCancellation.rut}). " +
                            "La anulación libera la reserva del RUT para volver a registrarlo si es necesario y deja constancia en auditoría."
                    )
                    OutlinedTextField(
                        value = reasonInput,
                        onValueChange = { reasonInput = it },
                        label = { Text("Motivo de la anulación (5 a 300 caracteres)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 4,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirmCancel(reasonInput)
                        reasonInput = ""
                    },
                    enabled = reasonInput.trim().length in 5..300,
                ) {
                    Text("Confirmar anulación")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    reasonInput = ""
                    onCloseCancel()
                }) {
                    Text("Cancelar")
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Bandeja de Registros",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text("Gestión local en este dispositivo", style = MaterialTheme.typography.bodyMedium)
                }
                OutlinedButton(onClick = onNavigateBack) {
                    Text("Volver")
                }
            }
        }

        if (state.isLoading) {
            item {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        state.message?.let { msg ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (msg.contains("correctamente")) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                    ),
                ) {
                    Text(
                        msg,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = state.filter.query,
                onValueChange = onQueryChanged,
                label = { Text("Buscar por nombre o RUT...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        item {
            Text("Filtrar por estado:", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val currentStates = state.filter.states
                FilterChip(
                    selected = currentStates.isEmpty(),
                    onClick = { onStateFilterSelected(null) },
                    label = { Text("Todos") },
                )
                FilterChip(
                    selected = CaptureState.READY in currentStates,
                    onClick = { onStateFilterSelected(CaptureState.READY) },
                    label = { Text("Confirmadas") },
                )
                FilterChip(
                    selected = CaptureState.DRAFT in currentStates,
                    onClick = { onStateFilterSelected(CaptureState.DRAFT) },
                    label = { Text("Borradores") },
                )
                FilterChip(
                    selected = CaptureState.CANCELLED in currentStates,
                    onClick = { onStateFilterSelected(CaptureState.CANCELLED) },
                    label = { Text("Anuladas") },
                )
            }
        }

        if (state.items.isEmpty() && !state.isLoading) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Text(
                        "No se encontraron registros locales que coincidan con el filtro.",
                        modifier = Modifier.padding(24.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        } else {
            items(state.items, key = { it.id }) { item ->
                RecordItemCard(
                    item = item,
                    onDetailClick = { onOpenDetail(item.id) },
                    onCancelClick = { onOpenCancel(item) },
                )
            }
        }
    }
}

@Composable
private fun RecordItemCard(
    item: RecordSummary,
    onDetailClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    Card(
        onClick = onDetailClick,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    item.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                StateBadge(item.state)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("RUT: ${item.rut}", style = MaterialTheme.typography.bodyMedium)
                Text("Versión: ${item.contract.name}", style = MaterialTheme.typography.bodySmall)
            }

            Text("Capturado: ${item.capturedAt}", style = MaterialTheme.typography.labelSmall)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDetailClick) {
                    Text("Ver ficha completa")
                }
                if (item.state != CaptureState.CANCELLED) {
                    TextButton(onClick = onCancelClick) {
                        Text("Anular", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
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

@Preview(showBackground = true)
@Composable
private fun RecordTrayPreview() {
    CapturaProspectosG6Theme {
        Surface {
            RecordTrayScreen(
                state = RecordTrayUiState(
                    items = listOf(
                        RecordSummary(
                            UUID.randomUUID(), 1, Instant.now(), UUID.randomUUID(),
                            "Juan Pérez Soto", "12345678-5", CaptureState.READY, RecordContract.F2027
                        ),
                        RecordSummary(
                            UUID.randomUUID(), 1, Instant.now(), UUID.randomUUID(),
                            "María González", "87654321-4", CaptureState.DRAFT, RecordContract.F2027
                        ),
                    ),
                ),
                onQueryChanged = {},
                onStateFilterSelected = {},
                onOpenCancel = {},
                onCloseCancel = {},
                onConfirmCancel = {},
                onNavigateBack = {},
            )
        }
    }
}
