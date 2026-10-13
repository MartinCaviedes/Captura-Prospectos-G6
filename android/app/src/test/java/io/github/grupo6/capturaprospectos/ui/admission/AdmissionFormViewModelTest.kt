package io.github.grupo6.capturaprospectos.ui.admission

import androidx.lifecycle.ViewModelStore
import io.github.grupo6.capturaprospectos.domain.admission.*
import io.github.grupo6.capturaprospectos.domain.capture.*
import io.github.grupo6.capturaprospectos.domain.context.EstablishmentSuggestion
import java.time.*
import java.util.UUID
import org.junit.Test
import org.junit.Assert.*

class AdmissionFormViewModelTest {
    private fun id(n: Long) = UUID(0, n)
    private val now = Instant.parse("2026-10-09T12:00:00Z")
    private val actor = LocalActor(id(1), LocalRole.CAPTOR, setOf(id(2)))
    private val event = CaptureEvent(id(3), id(2), "Evento ficticio", now, now.plusSeconds(3600), ZoneOffset.UTC, setOf(actor.id), null)
    private val programs = (10L..13L).map { ProgramOption(id(it), "Carrera ficticia $it", "test", "test", mentions = mapOf(id(30) to "Mención ficticia")) }
    private val sites = (20L..23L).map { SiteOption(id(it), "Sede ficticia $it", "test") }
    private fun vm() = AdmissionFormViewModel(AdmissionEnvironment(CaptureEnvironment(actor, event, id(4), emptyList(), "test", "Texto ficticio"), programs, sites),
        EstablishmentSuggestion("Colegio ficticio", "Comuna ficticia"), Clock.fixed(now, ZoneOffset.UTC))
    @Test fun startsWithoutAnswersConsentOrEventSitePreferences() {
        val state = vm().uiState.value
        assertEquals(AdmissionInput(), state.input); assertNull(state.consent); assertNull(state.mode)
    }
    @Test fun consentNeedsExplicitModeAndChangingModeRemovesEvidence() {
        val vm = vm(); vm.consent(true); assertNull(vm.uiState.value.consent)
        vm.mode(AcceptanceMode.VERBAL); vm.consent(true)
        assertEquals(ConsentEvidence("test", now, actor.id, AcceptanceMode.VERBAL), vm.uiState.value.consent)
        vm.mode(AcceptanceMode.DIRECT); assertNull(vm.uiState.value.consent)
    }
    @Test fun activityDetailRequiresConfirmationBeforeDiscard() {
        val vm = vm(); vm.activity(CurrentActivity.OTHER); vm.text(AdmissionField.ACTIVITY_OTHER, "Detalle ficticio")
        vm.activity(CurrentActivity.FOURTH_SECONDARY)
        assertEquals(CurrentActivity.OTHER, vm.uiState.value.input.currentActivity)
        vm.confirmActivityDiscard(false); assertEquals("Detalle ficticio", vm.uiState.value.input.currentActivityOther)
        vm.activity(CurrentActivity.FOURTH_SECONDARY); vm.confirmActivityDiscard(true)
        assertEquals(CurrentActivity.FOURTH_SECONDARY, vm.uiState.value.input.currentActivity)
        assertNull(vm.uiState.value.input.currentActivityOther)
    }
    @Test fun programsAreUniqueLimitedAndReindexedWithoutLosingMention() {
        val vm = vm(); programs.forEach { vm.program(it.id) }
        assertEquals(3, vm.uiState.value.input.interests.size)
        vm.mention(id(10), id(30)); vm.moveProgram(id(10), 1)
        assertEquals(listOf(id(11), id(10), id(12)), vm.uiState.value.input.interests.map { it.programId })
        assertEquals(id(30), vm.uiState.value.input.interests[1].mentionId)
        vm.program(id(11)); assertEquals(listOf(1, 2), vm.uiState.value.input.interests.map { it.priority })
        vm.program(id(999)); vm.mention(id(10), id(999))
        assertEquals(2, vm.uiState.value.input.interests.size); assertEquals(id(30), vm.uiState.value.input.interests.first().mentionId)
    }
    @Test fun allSitesOptionalIndependentAndPersonalNotStatedIsExplicit() {
        val vm = vm(); sites.forEach { vm.site(it.id) }
        assertEquals(4, vm.uiState.value.input.preferredSiteIds.size)
        assertTrue(vm.uiState.value.input.interests.isEmpty())
        vm.personal(PersonalOption.NOT_STATED); assertEquals(PersonalOption.NOT_STATED, vm.uiState.value.input.formPersonalOption)
        vm.personal(null); assertNull(vm.uiState.value.input.formPersonalOption)
    }
    @Test fun suggestionNeedsActionAndRemainsEditable() {
        val vm = vm(); assertNull(vm.uiState.value.input.establishment)
        vm.acceptEstablishmentSuggestion(); assertEquals("Colegio ficticio", vm.uiState.value.input.establishment)
        vm.text(AdmissionField.ESTABLISHMENT, "Otro ficticio"); assertEquals("Otro ficticio", vm.uiState.value.input.establishment)
    }
    @Test fun delegatesConsentAndValidationWithoutClaimingSave() {
        val vm = vm(); vm.validate(SaveIntent.SAVE_DRAFT)
        assertEquals(AdmissionField.CONSENT, vm.uiState.value.issues.single().field)
        vm.mode(AcceptanceMode.DIRECT); vm.consent(true); vm.validate(SaveIntent.CONFIRM_REGISTRATION)
        assertTrue(vm.uiState.value.issues.any { it.field == AdmissionField.BIRTH_DATE })
        vm.validate(SaveIntent.SAVE_DRAFT)
        assertTrue(vm.uiState.value.issues.isEmpty())
        assertTrue(vm.uiState.value.message!!.contains("No se ha guardado"))
    }
    @Test fun discardAndOwnerClearRemoveVolatileData() {
        val vm = vm(); vm.text(AdmissionField.GIVEN_NAMES, "Persona ficticia")
        assertFalse(vm.uiState.value.toString().contains("Persona ficticia"))
        vm.discard(); assertEquals(AdmissionFormState(), vm.uiState.value)
        vm.text(AdmissionField.GIVEN_NAMES, "Persona ficticia")
        ViewModelStore().apply { put("form", vm); clear() }
        assertEquals(AdmissionFormState(), vm.uiState.value)
    }
    @Test fun discardResetAndOwnerReleaseClearProvisionalBirthCalendar() {
        val vm = vm()
        val target = io.github.grupo6.capturaprospectos.ui.components.DatePickerTarget.BIRTH
        vm.birthCalendar.open(target, "2000-01-01"); vm.discard()
        assertNull(vm.birthCalendar.draft.value)
        vm.birthCalendar.open(target, "2000-01-01"); vm.reset()
        assertNull(vm.birthCalendar.draft.value)
        val store = ViewModelStore(); store.put("form", vm)
        vm.birthCalendar.open(target, "2000-01-01"); store.clear()
        assertNull(vm.birthCalendar.draft.value)
    }
}
