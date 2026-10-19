package io.github.grupo6.capturaprospectos.ui.access

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.grupo6.capturaprospectos.data.local.DemoAccessFixtures
import io.github.grupo6.capturaprospectos.domain.admission.*
import io.github.grupo6.capturaprospectos.domain.capture.*
import io.github.grupo6.capturaprospectos.domain.records.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock

data class DemoDataState(val busy: Boolean = false, val message: String? = null)

/** Carga voluntaria de fixtures mediante altas/anulaciones reales, sin sobrescribir registros. */
class DemoDataViewModel(private val repository: AdmissionRepository?, private val records: RecordRepository?,
    private val clock: Clock = Clock.systemUTC()) : ViewModel() {
    private val mutable = MutableStateFlow(DemoDataState())
    val uiState = mutable.asStateFlow()
    fun clearMessage() { if (!mutable.value.busy) mutable.update { it.copy(message = null) } }
    fun load(environment: AdmissionEnvironment) {
        val c = environment.context
        if (mutable.value.busy || !DemoAccessFixtures.canLoad(c.actor.id, c.event.id)) return
        mutable.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            var created = 0
            var existing = 0
            fun stop() { mutable.update { it.copy(message = "$created creados, $existing ya existentes. La carga se detuvo; conserva los datos y reintenta en este contexto.") } }
            try {
                val programs = environment.programs.filter { it.enabled && it.mentions.isEmpty() }
                if (programs.isEmpty() || repository == null || records == null) {
                    mutable.update { it.copy(message = "No está disponible la carga de datos ficticios en este contexto.") }
                    return@launch
                }
                // Conservar las primeras 24 fixtures evita reescribir demos de versiones anteriores.
                val fixtures = (1..24).map { it to CaptureState.NEEDS_CONTEXT } +
                    (101..118).map { it to CaptureState.READY } +
                    (119..130).map { it to CaptureState.DRAFT } +
                    (131..136).map { it to CaptureState.CANCELLED }
                for ((index, target) in fixtures) {
                    val choices = if (index <= 24) programs.take(if (index % 2 == 0) 2 else 1)
                        else (programs.drop(index % programs.size) + programs.take(index % programs.size)).take(1 + index % 3)
                    val input = AdmissionInput(givenNames = "Persona Ficticia $index", familyNames = "Evaluación Demo",
                        rut = syntheticRut(index), email = "evaluacion$index@example.invalid", phone = "+56900000000",
                        birthDate = "2000-01-01", residenceCommune = "Comuna ficticia",
                        currentActivity = CurrentActivity.SECONDARY_GRADUATE,
                        interests = choices.mapIndexed { priority, p -> RankedInterest(p.id, priority + 1) })
                    val page = records.list(RecordQuery(RecordFilter(query = input.rut, eventId = c.event.id), limit = 100))
                    if (page !is RecordPageResult.Page) { stop(); return@launch }
                    val previous = page.items.firstOrNull { it.rut == RutValidator.normalize(input.rut) }
                    if (previous != null) {
                        // Reanudar sólo una anulación fixture interrumpida entre alta y cancelación.
                        // Una revisión posterior puede ser trabajo manual: nunca la sobrescribe la carga.
                        if (target == CaptureState.CANCELLED && previous.revision == 1L && previous.state == CaptureState.NEEDS_CONTEXT) {
                            val stored = (repository.get(previous.id) as? ReadAdmissionResult.Found)?.capture
                            if (stored == null) { stop(); return@launch }
                            if (stored.ownerId == c.actor.id && stored.eventId == c.event.id &&
                                stored.input.givenNames == input.givenNames && stored.input.familyNames == input.familyNames &&
                                stored.input.email == input.email && stored.consent.version == c.consentVersion) {
                                if (records.cancel(CancelRecord(stored.id, stored.revision, "Anulación de muestra ficticia")) !is RecordMutationResult.Saved) { stop(); return@launch }
                            }
                        }
                        existing++
                        continue
                    }
                    val now = clock.instant()
                    // Coordenadas de fixture declaradas en la confirmación, no una lectura GPS del equipo.
                    val location = if (target == CaptureState.READY) CaptureLocation.Device(Coordinates(0.0, 0.0), now, 5.0) else null
                    val intent = if (target == CaptureState.DRAFT) SaveIntent.SAVE_DRAFT else SaveIntent.CONFIRM_REGISTRATION
                    val request = NewAdmission(c.event.id, input, intent,
                        ConsentEvidence(c.consentVersion, now, c.actor.id, AcceptanceMode.DIRECT), location)
                    when (val result = repository.create(request)) {
                        is CreateAdmissionResult.Saved -> {
                            if (target == CaptureState.CANCELLED &&
                                records.cancel(CancelRecord(result.id, result.revision, "Anulación de muestra ficticia")) !is RecordMutationResult.Saved) { stop(); return@launch }
                            created++
                        }
                        is CreateAdmissionResult.DuplicateOwn -> existing++
                        else -> { stop(); return@launch }
                    }
                }
                mutable.update { it.copy(message = "$created prospectos ficticios creados; $existing ya existían. Muestra completa: 24 sin contexto, 18 listas, 12 borradores y 6 anuladas. Ubicaciones simuladas; los registros modificados se conservan.") }
            } finally { mutable.update { it.copy(busy = false) } }
        }
    }
    private fun syntheticRut(index: Int): String {
        val body = (90_000_000 + index).toString()
        var weight = 2
        val sum = body.reversed().sumOf { digit -> (digit.digitToInt() * weight).also { weight = if (weight == 7) 2 else weight + 1 } }
        return "$body-${when (val dv = 11 - sum % 11) { 11 -> "0"; 10 -> "K"; else -> dv.toString() }}"
    }
}
