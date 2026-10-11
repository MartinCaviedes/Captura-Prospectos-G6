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
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

/** Selección local: cancelar no cambia el campo y confirmar sólo entrega ISO.
 * La fecha, el mes y el modo de ingreso no se guardan en SavedState. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun VolatileDatePickerDialog(
    initialValue: String, title: String,
    yearRange: IntRange = 1900..2100,
    onDismiss: () -> Unit, onConfirm: (String) -> Unit,
) {
    val picker = remember(initialValue, yearRange) {
        val date = runCatching { LocalDate.parse(initialValue) }.getOrNull()
        DatePickerState(locale = Locale.getDefault(),
            initialSelectedDateMillis = date?.takeIf { it.year in yearRange }
                ?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(), yearRange = yearRange)
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
