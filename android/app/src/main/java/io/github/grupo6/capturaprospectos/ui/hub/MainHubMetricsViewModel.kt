package io.github.grupo6.capturaprospectos.ui.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.grupo6.capturaprospectos.domain.capture.CaptureEvent
import io.github.grupo6.capturaprospectos.domain.records.*
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class HubMetricsState(val metrics: DeviceMetrics? = null, val isLoading: Boolean = false,
    val fromDate: String = "", val untilDate: String = "", val error: String? = null, val showRange: Boolean = false)

/** Retiene el último cálculo del contexto; error/desconocido nunca se convierten en ceros. */
class MainHubMetricsViewModel(private val repository: RecordRepository?, private val event: CaptureEvent,
    private val clock: Clock = Clock.systemUTC()): ViewModel() {
    private val mutable=MutableStateFlow(HubMetricsState())
    val uiState=mutable.asStateFlow()
    val calendar = io.github.grupo6.capturaprospectos.ui.components.VolatileDatePickerController()
    fun toggleRange() { mutable.update { it.copy(showRange = !it.showRange) } }
    private var reading:Job?=null
    private var query=MetricsQuery(event.id)
    init { refresh() }
    fun fromDate(value:String) { mutable.update { it.copy(fromDate=value,error=null) } }
    fun untilDate(value:String) { mutable.update { it.copy(untilDate=value,error=null) } }
    fun all():Job { mutable.update { it.copy(fromDate="",untilDate="") }; query=MetricsQuery(event.id); return refresh() }
    fun today():Job {
        val date=clock.instant().atZone(event.timeZone).toLocalDate().toString()
        mutable.update { it.copy(fromDate=date,untilDate=date) }; return range()!!
    }
    fun range():Job? {
        val current=mutable.value
        val from=parseDate(current.fromDate); val until=parseDate(current.untilDate)
        if (from == null || until == null || from > until) {
            reading?.cancel()
            mutable.update { it.copy(isLoading=false,error="Revisa las fechas aaaa-mm-dd: desde debe ser anterior o igual a hasta.") }; return null
        }
        query=MetricsQuery(event.id,from.atStartOfDay(event.timeZone).toInstant(),until.plusDays(1).atStartOfDay(event.timeZone).toInstant())
        return refresh()
    }
    fun refresh():Job {
        reading?.cancel(); val request=query
        mutable.update { it.copy(isLoading=true,error=null) }
        return viewModelScope.launch {
            val result=repository?.getMetrics(request) ?: MetricsResult.StorageUnavailable
            currentCoroutineContext().ensureActive()
            when (result) {
                is MetricsResult.Calculated -> mutable.update { it.copy(metrics=result.metrics,isLoading=false,error=null) }
                MetricsResult.InvalidQuery -> mutable.update { it.copy(isLoading=false,error="El rango de consulta no es válido.") }
                MetricsResult.Unavailable -> mutable.update { it.copy(metrics=null,isLoading=false,error="Métricas no disponibles para este contexto autorizado.") }
                MetricsResult.StorageUnavailable -> mutable.update { it.copy(isLoading=false,error="No se pudo actualizar el cálculo local. No se interpreta como cero.") }
            }
        }.also { reading=it }
    }
    private fun parseDate(value:String):LocalDate? = if (!Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(value)) null
        else try { LocalDate.parse(value) } catch (_:DateTimeException) { null }
    override fun onCleared() { calendar.close(); reading?.cancel(); mutable.value=HubMetricsState() }
}
