package io.github.grupo6.capturaprospectos.ui.components

import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DatePickerTarget { BIRTH, FROM, UNTIL }

/** Sólo memoria del dueño de pantalla; no tiene Saver ni escribe en almacenamiento. */
data class DatePickerDraft(val id: Long, val target: DatePickerTarget,
    val selectedMillis: Long?, val displayedMonthMillis: Long? = null, val inputMode: Boolean = false) {
    override fun toString() = "DatePickerDraft(redacted)"
}

class VolatileDatePickerController {
    private val mutable = MutableStateFlow<DatePickerDraft?>(null)
    val draft = mutable.asStateFlow()
    private var generation = 0L

    fun open(target: DatePickerTarget, initialValue: String) {
        val date = runCatching { LocalDate.parse(initialValue) }.getOrNull()
        mutable.value = DatePickerDraft(++generation, target,
            date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli())
    }
    fun update(id: Long, selectedMillis: Long?, displayedMonthMillis: Long, inputMode: Boolean) {
        val current = mutable.value ?: return
        if (current.id == id) mutable.value = current.copy(selectedMillis = selectedMillis,
            displayedMonthMillis = displayedMonthMillis, inputMode = inputMode)
    }
    fun close() { mutable.value = null }
}
