package io.github.grupo6.capturaprospectos.ui.records

import androidx.lifecycle.ViewModelStore
import io.github.grupo6.capturaprospectos.domain.admission.*
import io.github.grupo6.capturaprospectos.domain.capture.*
import io.github.grupo6.capturaprospectos.domain.records.*
import java.time.*
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class RecordDetailStateTest {
    private val dispatcher = StandardTestDispatcher()
    private val now = Instant.parse("2026-10-10T15:00:00Z")
    private val actor = LocalActor(UUID(0,1),LocalRole.CAPTOR,setOf(UUID(0,2)))
    private val event = CaptureEvent(UUID(0,3),UUID(0,2),"Evento ficticio",now,now.plusSeconds(3600),ZoneOffset.UTC,setOf(actor.id),Coordinates(-33.0,-71.0))
    private val program = ProgramOption(UUID(0,4),"Carrera ficticia","v1","Demo")
    private val env = AdmissionEnvironment(CaptureEnvironment(actor,event,UUID(0,5),emptyList(),"v1","Consentimiento ficticio"),listOf(program),emptyList())
    private val record = StoredAdmission(UUID(0,6),1,actor.id,event.siteId,event.id,UUID(0,5),now,event.name,event.timeZone,
        AdmissionInput(),"",ConsentEvidence("v1",now,actor.id,AcceptanceMode.DIRECT),emptyList(),emptyList(),null,CaptureState.DRAFT)
    private val store = ViewModelStore()
    private class Repo(var record: StoredAdmission, val env: AdmissionEnvironment): RecordRepository {
        var result: RecordMutationResult = RecordMutationResult.StorageUnavailable
        var wait: CompletableDeferred<Unit>? = null
        var last: EditAdmission? = null
        var gets = 0
        var edits = 0
        var readWait: CompletableDeferred<Unit>? = null
        override suspend fun list(query: RecordQuery) = RecordPageResult.Page(emptyList(),null)
        override suspend fun get(id: UUID): ReadAdmissionResult { gets++; readWait?.await(); return ReadAdmissionResult.Found(record) }
        override suspend fun editContext(id: UUID) = RecordEditContextResult.Available(env)
        override suspend fun edit(command: EditAdmission): RecordMutationResult { edits++; last=command; wait?.await(); return result }
        override suspend fun cancel(command: CancelRecord) = RecordMutationResult.StorageUnavailable
    }
    private lateinit var repo: Repo
    private fun vm(): RecordTrayViewModel = RecordTrayViewModel(repo).also { store.put("tray",it) }
    private suspend fun editing(vm: RecordTrayViewModel) { vm.openDetail(record.id)?.join(); vm.startEditing()?.join() }
    @Before fun setup() { Dispatchers.setMain(dispatcher); repo=Repo(record,env) }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }

    @Test fun draftEditorRetainsAllFieldsAndCanConfirmInsteadOfCreatingAnotherRecord() = runTest(dispatcher) {
        val vm=vm(); editing(vm)
        val form=vm.uiState.value.editForm!!
        form.text(AdmissionField.GIVEN_NAMES,"Persona Ficticia"); form.text(AdmissionField.FAMILY_NAMES,"Demo")
        form.text(AdmissionField.RUT,"12345678-5"); form.activity(CurrentActivity.SECONDARY_GRADUATE)
        form.program(program.id); form.schedule(PreferredSchedule.DAY); form.confirmEventLocation()
        vm.saveEditor(SaveIntent.CONFIRM_REGISTRATION)?.join()
        assertEquals(SaveIntent.CONFIRM_REGISTRATION,repo.last!!.intent)
        assertEquals("Persona Ficticia",repo.last!!.input.givenNames)
        assertEquals(program.id,repo.last!!.input.interests.single().programId)
        assertTrue(repo.last!!.location is CaptureLocation.EventConfirmed)
        assertEquals(record.consent,form.uiState.value.consent)
        assertNull(form.save(SaveIntent.SAVE_DRAFT))
    }
    @Test fun conflictPreservesWorkingCopyReasonAndRevisionUntilConfirmedReload() = runTest(dispatcher) {
        repo.result=RecordMutationResult.RevisionConflict
        val vm=vm(); editing(vm); val form=vm.uiState.value.editForm!!
        form.text(AdmissionField.NOTES,"Cambios ficticios"); vm.updateEditReason("Motivo ficticio")
        val reads=repo.gets
        vm.saveEditor(SaveIntent.SAVE_DRAFT)?.join()
        assertTrue(vm.uiState.value.isEditing); assertTrue(vm.uiState.value.editConflict)
        assertSame(form,vm.uiState.value.editForm); assertEquals(reads,repo.gets)
        assertEquals("Cambios ficticios",form.uiState.value.input.notes)
        assertEquals("Motivo ficticio",vm.uiState.value.editReason)
        assertEquals(1L,vm.uiState.value.selectedRecord!!.revision)
        assertNull(vm.saveEditor(SaveIntent.SAVE_DRAFT)); assertEquals(1,repo.edits)
        vm.requestEditDiscard(reload=true); vm.confirmEditDiscard(false)
        assertSame(form,vm.uiState.value.editForm)
        repo.record=record.copy(revision=2)
        vm.requestEditDiscard(reload=true); vm.confirmEditDiscard(true); runCurrent()
        assertNull(vm.uiState.value.editForm); assertEquals(2L,vm.uiState.value.selectedRecord!!.revision)
    }
    @Test fun invalidAndDuplicateResultsPreserveFieldsAndExposeSpecificFeedback() = runTest(dispatcher) {
        val vm=vm(); editing(vm); val form=vm.uiState.value.editForm!!
        form.text(AdmissionField.EMAIL,"bad")
        val issue=AdmissionIssue(AdmissionField.EMAIL,ValidationCode.FORMAT)
        repo.result=RecordMutationResult.Invalid(listOf(issue)); vm.saveEditor(SaveIntent.SAVE_DRAFT)?.join()
        assertEquals(listOf(issue),form.uiState.value.issues); assertEquals("bad",form.uiState.value.input.email)
        repo.result=RecordMutationResult.DuplicateRestricted; vm.saveEditor(SaveIntent.SAVE_DRAFT)?.join()
        assertTrue(vm.uiState.value.message!!.contains("Ya existe")); assertTrue(vm.uiState.value.isEditing)
        assertFalse(vm.uiState.value.isMutating)
    }
    @Test fun inFlightCommitBlocksSecondWriteFieldsReasonDismissAndCancellation() = runTest(dispatcher) {
        val vm=vm(); editing(vm); val form=vm.uiState.value.editForm!!
        form.text(AdmissionField.NOTES,"Nota anterior"); vm.updateEditReason("Motivo anterior")
        repo.wait=CompletableDeferred()
        val saving=vm.saveEditor(SaveIntent.SAVE_DRAFT); runCurrent()
        assertTrue(vm.uiState.value.isMutating); assertTrue(form.uiState.value.isSaving)
        assertNull(vm.saveEditor(SaveIntent.SAVE_DRAFT))
        form.text(AdmissionField.NOTES,"Otra"); vm.updateEditReason("Otro"); vm.closeDetail(); vm.cancelEditing()
        assertEquals("Nota anterior",form.uiState.value.input.notes); assertEquals("Motivo anterior",vm.uiState.value.editReason)
        assertTrue(vm.uiState.value.isEditing); assertEquals(1,repo.edits)
        repo.wait!!.complete(Unit); saving?.join()
        assertFalse(vm.uiState.value.isMutating); assertFalse(form.uiState.value.isSaving)
    }
    @Test fun confirmedIdentityAndConsentCannotBeChangedAndOldGpsIsNotRevalidated() = runTest(dispatcher) {
        repo.record=record.copy(state=CaptureState.READY,input=AdmissionInput(rut="12345678-5"),
            location=CaptureLocation.Device(Coordinates(-33.0,-71.0),now.minusSeconds(600),20.0))
        val vm=vm(); editing(vm); val form=vm.uiState.value.editForm!!
        form.text(AdmissionField.RUT,"11111111-1"); form.consent(false); form.mode(AcceptanceMode.VERBAL); form.openQrScanner()
        assertEquals("12345678-5",form.uiState.value.input.rut); assertEquals(record.consent,form.uiState.value.consent)
        assertFalse(form.uiState.value.isQrScannerOpen)
        form.clearLocation(); assertEquals(repo.record.location,form.uiState.value.location)
        vm.saveEditor(SaveIntent.CONFIRM_REGISTRATION)?.join()
        assertEquals(repo.record.location,repo.last!!.location)
    }
    @Test fun closingCancelsLateDetailReadAndClearingOwnerCancelsEditor() = runTest(dispatcher) {
        val vm=vm(); editing(vm); val form=vm.uiState.value.editForm!!
        vm.cancelEditing(); vm.closeDetail()
        repo.readWait=CompletableDeferred(); vm.openDetail(record.id); runCurrent(); vm.closeDetail()
        repo.readWait!!.complete(Unit); runCurrent()
        assertFalse(vm.uiState.value.isDetailOpen)
        form.text(AdmissionField.NOTES,"Tardio"); assertNull(form.uiState.value.input.notes)
        store.clear(); assertFalse(vm.uiState.value.isDetailOpen)
    }
}
