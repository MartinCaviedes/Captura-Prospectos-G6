package io.github.grupo6.capturaprospectos.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

/** Selección local: cancelar no cambia el campo y confirmar sólo entrega ISO.
 * La fecha, el mes y el modo de ingreso no se guardan en SavedState. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun VolatileDatePickerDialog(
    controller: VolatileDatePickerController, title: String,
    yearRange: IntRange = 1900..2100,
    onDismiss: () -> Unit, onConfirm: (String) -> Unit,
) {
    val draft by controller.draft.collectAsState()
    val current = draft ?: return
    val picker = remember(current.id, yearRange) {
        DatePickerState(locale = Locale.getDefault(),
            initialSelectedDateMillis = current.selectedMillis?.takeIf {
                Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).year in yearRange
            }, initialDisplayedMonthMillis = current.displayedMonthMillis ?: current.selectedMillis?.takeIf {
                Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).year in yearRange
            }, yearRange = yearRange,
            initialDisplayMode = if (current.inputMode) DisplayMode.Input else DisplayMode.Picker)
    }
    LaunchedEffect(picker, controller, current.id) {
        snapshotFlow { Triple(picker.selectedDateMillis, picker.displayedMonthMillis, picker.displayMode == DisplayMode.Input) }
            .collect { (selected, month, input) -> controller.update(current.id, selected, month, input) }
    }
    DatePickerDialog(onDismissRequest = onDismiss,
        confirmButton = { TextButton(enabled = picker.selectedDateMillis != null, onClick = {
            picker.selectedDateMillis?.let {
                onConfirm(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString())
            }
        }) { Text("Usar fecha") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar fecha") } }) {
        CompositionLocalProvider(LocalSaveableStateRegistry provides null) {
            DatePicker(picker, modifier = Modifier.verticalScroll(rememberScrollState()),
                title = { Text(title, Modifier.padding(24.dp)) })
        }
    }
}
