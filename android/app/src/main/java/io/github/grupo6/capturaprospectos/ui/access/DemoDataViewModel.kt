package io.github.grupo6.capturaprospectos.ui.access

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.grupo6.capturaprospectos.data.local.DemoAccessFixtures
import io.github.grupo6.capturaprospectos.domain.admission.*
import io.github.grupo6.capturaprospectos.domain.capture.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock

data class DemoDataState(val busy: Boolean = false, val message: String? = null)

/** Sólo carga voluntaria de fixtures, a través del mismo contrato de alta y sus validaciones. */
class DemoDataViewModel(private val repository: AdmissionRepository?, private val clock: Clock = Clock.systemUTC()) : ViewModel() {
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
            try {
                val programs = environment.programs.filter { it.enabled && it.mentions.isEmpty() }
                if (programs.isEmpty() || repository == null) {
                    mutable.update { it.copy(message = "No está disponible la carga de datos ficticios en este contexto.") }
                    return@launch
                }
                for (index in 1..24) {
                    val choices = programs.take(if (index % 2 == 0) 2 else 1)
                    val input = AdmissionInput(givenNames = "Persona Ficticia $index", familyNames = "Evaluación Demo",
                        rut = syntheticRut(index), email = "evaluacion$index@example.invalid", phone = "+56900000000",
                        birthDate = "2000-01-01", residenceCommune = "Comuna ficticia",
                        currentActivity = CurrentActivity.SECONDARY_GRADUATE,
                        interests = choices.mapIndexed { priority, p -> RankedInterest(p.id, priority + 1) })
                    val request = NewAdmission(c.event.id, input, SaveIntent.CONFIRM_REGISTRATION,
                        ConsentEvidence(c.consentVersion, clock.instant(), c.actor.id, AcceptanceMode.DIRECT), null)
                    when (repository.create(request)) {
                        is CreateAdmissionResult.Saved -> created++
                        is CreateAdmissionResult.DuplicateOwn -> existing++
                        else -> {
                            mutable.update { it.copy(message = "$created creados, $existing ya existentes. La carga se detuvo; conserva los datos y reintenta en este contexto.") }
                            return@launch
                        }
                    }
                }
                mutable.update { it.copy(message = "$created prospectos ficticios creados; $existing ya existían. Sin ubicación confirmada: revisa el estado «Falta contexto» en registros y resumen.") }
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
