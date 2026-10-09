package io.github.grupo6.capturaprospectos.ui.admission

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import io.github.grupo6.capturaprospectos.domain.admission.*
import io.github.grupo6.capturaprospectos.domain.capture.*
import io.github.grupo6.capturaprospectos.domain.context.EstablishmentSuggestion
import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AdmissionFormState(
    val input: AdmissionInput = AdmissionInput(),
    val mode: AcceptanceMode? = null, val consent: ConsentEvidence? = null,
    val pendingActivity: CurrentActivity? = null, val issues: List<AdmissionIssue> = emptyList(),
    val message: String? = null,
) { override fun toString() = "AdmissionFormState(redacted)" }

/** R1: estado volátil y validación. No recibe repositorios, SavedStateHandle ni almacenamiento. */
class AdmissionFormViewModel(
    val environment: AdmissionEnvironment,
    val suggestion: EstablishmentSuggestion? = null,
    private val clock: Clock = Clock.systemUTC(),
    private val prepare: PrepareAdmission = PrepareAdmission(),
) : ViewModel() {
    private val state = MutableStateFlow(AdmissionFormState())
    val uiState = state.asStateFlow()
    private fun change(transform: (AdmissionInput) -> AdmissionInput) {
        state.value = state.value.copy(input = transform(state.value.input), issues = emptyList(), message = null)
    }
    @MainThread fun text(field: AdmissionField, value: String) = change { input -> when (field) {
        AdmissionField.GIVEN_NAMES -> input.copy(givenNames = value)
        AdmissionField.FAMILY_NAMES -> input.copy(familyNames = value)
        AdmissionField.RUT -> input.copy(rut = value)
        AdmissionField.EMAIL -> input.copy(email = value)
        AdmissionField.PHONE -> input.copy(phone = value)
        AdmissionField.BIRTH_DATE -> input.copy(birthDate = value)
        AdmissionField.RESIDENCE_COMMUNE -> input.copy(residenceCommune = value)
        AdmissionField.ACTIVITY_OTHER -> if (input.currentActivity == CurrentActivity.OTHER) input.copy(currentActivityOther = value) else input
        AdmissionField.ESTABLISHMENT -> input.copy(establishment = value)
        AdmissionField.ESTABLISHMENT_COMMUNE -> input.copy(establishmentCommune = value)
        AdmissionField.NOTES -> input.copy(notes = value)
        else -> input // Sin catálogo de modalidad/canal: no inventar opciones.
    } }
    @MainThread fun activity(value: CurrentActivity) {
        val input = state.value.input
        if (input.currentActivity == CurrentActivity.OTHER && value != CurrentActivity.OTHER && !input.currentActivityOther.isNullOrBlank()) {
            state.value = state.value.copy(pendingActivity = value); return
        }
        change { it.copy(currentActivity = value, currentActivityOther = if (value == CurrentActivity.OTHER) it.currentActivityOther else null) }
    }
    @MainThread fun confirmActivityDiscard(confirmed: Boolean) {
        val pending = state.value.pendingActivity ?: return
        if (confirmed) change { it.copy(currentActivity = pending, currentActivityOther = null) }
        state.value = state.value.copy(pendingActivity = null)
    }
    @MainThread fun acceptEstablishmentSuggestion() {
        val value = suggestion ?: return
        change { it.copy(establishment = value.name, establishmentCommune = value.commune) }
    }
    @MainThread fun personal(value: PersonalOption?) = change { it.copy(formPersonalOption = value) }
    @MainThread fun schedule(value: PreferredSchedule?) = change { it.copy(preferredSchedule = value) }
    @MainThread fun interestLevel(value: InterestLevel?) = change { it.copy(interestLevel = value) }
    @MainThread fun motivation(value: Motivation?) = change { it.copy(motivation = value) }
    @MainThread fun program(id: UUID) {
        if (environment.programs.none { it.id == id && it.enabled }) return
        change { input ->
            val current = input.interests
            val next = if (current.any { it.programId == id }) current.filterNot { it.programId == id }
                else if (current.size < 3) current + RankedInterest(id, current.size + 1) else current
            input.copy(interests = next.mapIndexed { index, item -> item.copy(priority = index + 1) })
        }
    }
    @MainThread fun moveProgram(id: UUID, direction: Int) {
        if (direction != -1 && direction != 1) return
        change { input ->
            val items = input.interests.toMutableList(); val from = items.indexOfFirst { it.programId == id }
            val to = from + direction
            if (from >= 0 && to in items.indices) { val item = items.removeAt(from); items.add(to, item) }
            input.copy(interests = items.mapIndexed { index, item -> item.copy(priority = index + 1) })
        }
    }
    @MainThread fun mention(programId: UUID, mentionId: UUID?) {
        val option = environment.programs.singleOrNull { it.id == programId } ?: return
        if (mentionId != null && mentionId !in option.mentions) return
        change { input -> input.copy(interests = input.interests.map { if (it.programId == programId) it.copy(mentionId = mentionId) else it }) }
    }
    @MainThread fun site(id: UUID) {
        if (environment.sites.none { it.id == id && it.enabled }) return
        change { input -> input.copy(preferredSiteIds = if (id in input.preferredSiteIds) input.preferredSiteIds - id else input.preferredSiteIds + id) }
    }
    @MainThread fun mode(value: AcceptanceMode) {
        state.value = state.value.copy(mode = value, consent = null, issues = emptyList(), message = null)
    }
    @MainThread fun consent(accepted: Boolean) {
        val current = state.value
        val evidence = if (accepted && current.mode != null) ConsentEvidence(environment.context.consentVersion,
            clock.instant(), environment.context.actor.id, current.mode) else null
        state.value = current.copy(consent = evidence, issues = emptyList(), message = null)
    }
    @MainThread fun validate(intent: SaveIntent) {
        val current = state.value; val context = environment.context
        val result = prepare.prepare(current.input, intent, current.consent, context.consentVersion,
            context.actor.id, environment.programs, environment.sites, null, context.event.location,
            clock.instant(), context.event.timeZone)
        state.value = when (result) {
            AdmissionPreparation.ConsentRequired -> current.copy(issues = listOf(AdmissionIssue(AdmissionField.CONSENT, ValidationCode.REQUIRED)), message = "Registra el consentimiento para validar.")
            is AdmissionPreparation.Invalid -> current.copy(issues = result.issues, message = "Revisa los campos indicados.")
            is AdmissionPreparation.Prepared -> current.copy(issues = emptyList(), message = "Validación local correcta. No se ha guardado ni enviado la inscripción.")
        }
    }
    @MainThread fun discard() { state.value = AdmissionFormState() }
    override fun onCleared() { state.value = AdmissionFormState() }
}
