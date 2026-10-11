package io.github.grupo6.capturaprospectos.ui.context

import androidx.lifecycle.ViewModelStore
import io.github.grupo6.capturaprospectos.domain.admission.AdmissionEnvironment
import io.github.grupo6.capturaprospectos.domain.capture.*
import io.github.grupo6.capturaprospectos.domain.context.*
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DemoContextViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val repo = FakeContextRepository()
    private lateinit var vm: DemoContextViewModel

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        vm = DemoContextViewModel(repo)
        store.put("context", vm)
    }
    @After fun cleanup() { store.clear(); dispatcher.scheduler.runCurrent(); Dispatchers.resetMain() }
    private fun ready() { repo.state.value = DemoContextState.Ready(repo.base); dispatcher.scheduler.runCurrent() }

    @Test fun openingOnlyObservesWithoutInitializingOrSelecting() {
        assertNull(vm.uiState.value.context)
        dispatcher.scheduler.runCurrent()
        assertEquals(DemoContextState.NotInitialized, vm.uiState.value.context)
        assertEquals(emptyList<String>(), repo.calls)
    }
    @Test fun initializesOnceAndConfirmsRepositorySnapshot() = runTest(dispatcher) {
        runCurrent(); vm.initialize(); advanceUntilIdle()
        assertEquals(listOf("initialize", "snapshot"), repo.calls)
        assertEquals(DemoContextState.Ready(repo.base), vm.uiState.value.context)
        assertFalse(vm.uiState.value.busy)
        assertNull(vm.initialize())
    }
    @Test fun profileChangeUsesConfirmedStateAndRemovesOldEvent() = runTest(dispatcher) {
        repo.state.value = DemoContextState.Ready(repo.base.copy(currentProfile = repo.profile, selectedEvent = repo.event))
        runCurrent(); vm.selectProfile(repo.other.actor.id); advanceUntilIdle()
        val current = (vm.uiState.value.context as DemoContextState.Ready).snapshot
        assertEquals(repo.other, current.currentProfile)
        assertNull(current.selectedEvent)
        assertEquals(listOf("profile:${repo.other.actor.id}", "snapshot"), repo.calls)
    }
    @Test fun eventSelectionWaitsForRepositoryConfirmation() = runTest(dispatcher) {
        ready(); repo.gate = CompletableDeferred()
        vm.selectEvent(repo.event.id); runCurrent()
        assertTrue(vm.uiState.value.busy)
        assertNull((vm.uiState.value.context as DemoContextState.Ready).snapshot.selectedEvent)
        repo.gate!!.complete(Unit); advanceUntilIdle()
        assertEquals(repo.event, (vm.uiState.value.context as DemoContextState.Ready).snapshot.selectedEvent)
    }
    @Test fun rapidSecondActionIsNotQueuedAgainstOldContext() = runTest(dispatcher) {
        ready(); repo.gate = CompletableDeferred()
        vm.selectProfile(repo.other.actor.id); runCurrent()
        assertNull(vm.selectEvent(repo.event.id))
        assertNull(vm.clearSelection())
        assertNull(vm.retry())
        repo.gate!!.complete(Unit); advanceUntilIdle()
        assertEquals(listOf("profile:${repo.other.actor.id}", "snapshot"), repo.calls)
    }
    @Test fun rejectedSelectionKeepsSnapshotAndExplainsFailure() = runTest(dispatcher) {
        ready(); repo.result = ContextChange.INVALID_SELECTION
        vm.selectEvent(UUID(0, 99)); advanceUntilIdle()
        assertEquals(DemoContextState.Ready(repo.base), vm.uiState.value.context)
        assertTrue(vm.uiState.value.message!!.contains("no es válida"))
    }
    @Test fun incompatibleSeedDoesNotResetOrAutomaticallyRetry() = runTest(dispatcher) {
        runCurrent(); repo.result = ContextChange.INCOMPATIBLE_SEED
        vm.initialize(); advanceUntilIdle()
        assertEquals(DemoContextState.NotInitialized, vm.uiState.value.context)
        assertTrue(vm.uiState.value.message!!.contains("requiere revisión"))
        assertEquals(listOf("initialize", "snapshot"), repo.calls)
    }
    @Test fun storageFailureIsShownWithoutClaimingSuccess() = runTest(dispatcher) {
        ready(); repo.result = ContextChange.STORAGE_UNAVAILABLE
        vm.selectEvent(repo.event.id); advanceUntilIdle()
        assertTrue(vm.uiState.value.message!!.contains("No se pudo"))
        assertNull((vm.uiState.value.context as DemoContextState.Ready).snapshot.selectedEvent)
    }
    @Test fun retryReadsAndRestartsObservationWithoutSeeding() = runTest(dispatcher) {
        repo.terminateObservation = true; repo.state.value = DemoContextState.StorageUnavailable
        runCurrent()
        repo.terminateObservation = false; repo.state.value = DemoContextState.NotInitialized
        vm.retry(); advanceUntilIdle()
        assertEquals(listOf("snapshot"), repo.calls)
        assertEquals(DemoContextState.NotInitialized, vm.uiState.value.context)
        repo.state.value = DemoContextState.Ready(repo.base); runCurrent()
        assertEquals(DemoContextState.Ready(repo.base), vm.uiState.value.context)
    }
    @Test fun clearSelectionDelegatesWithoutReinitializing() = runTest(dispatcher) {
        repo.state.value = DemoContextState.Ready(repo.base.copy(currentProfile = repo.profile, selectedEvent = repo.event))
        runCurrent(); vm.clearSelection(); advanceUntilIdle()
        val snapshot = (vm.uiState.value.context as DemoContextState.Ready).snapshot
        assertNull(snapshot.currentProfile); assertNull(snapshot.selectedEvent)
        assertEquals(repo.base.deviceId, snapshot.deviceId)
        assertEquals(listOf("clear", "snapshot"), repo.calls)
    }
    @Test fun cancellationPropagatesAndReleasesBusyState() = runTest(dispatcher) {
        ready(); repo.gate = CompletableDeferred()
        val job = vm.selectEvent(repo.event.id)!!; runCurrent()
        job.cancel(); advanceUntilIdle()
        assertTrue(job.isCancelled)
        assertFalse(vm.uiState.value.busy)
        assertNull(vm.uiState.value.message)
        assertEquals(DemoContextState.Ready(repo.base), vm.uiState.value.context)
        assertFalse(repo.calls.contains("snapshot"))
    }
    @Test fun clearingViewModelCancelsObservationAndActiveOperation() = runTest(dispatcher) {
        ready(); assertEquals(1, repo.observers)
        repo.gate = CompletableDeferred()
        val job = vm.selectEvent(repo.event.id)!!; runCurrent()
        store.clear(); advanceUntilIdle()
        assertTrue(job.isCancelled); assertEquals(0, repo.observers)
        assertFalse(repo.calls.contains("snapshot"))
    }
    @Test fun actionReplacesObserverWithoutConcurrentCollectors() = runTest(dispatcher) {
        ready(); assertEquals(1, repo.observers)
        repo.gate = CompletableDeferred()
        vm.selectEvent(repo.event.id); runCurrent()
        assertEquals(0, repo.observers)
        repo.gate!!.complete(Unit); advanceUntilIdle()
        assertEquals(1, repo.observers)
        repo.state.value = DemoContextState.Ready(repo.base.copy(currentProfile = repo.other))
        runCurrent()
        assertEquals(repo.other, (vm.uiState.value.context as DemoContextState.Ready).snapshot.currentProfile)
        assertNull((vm.uiState.value.context as DemoContextState.Ready).snapshot.selectedEvent)
    }

    @Test fun saveEventDelegatesToRepositoryAndUpdatesMessage() = runTest(dispatcher) {
        ready()
        val cmd = SaveDemoEvent(
            siteId = repo.profile.actor.siteIds.first(),
            name = "Nuevo Evento Demo",
            startsAt = Instant.EPOCH,
            endsAt = Instant.EPOCH.plusSeconds(3600),
        )
        var callbackInvoked = false
        vm.saveEvent(cmd) { result ->
            callbackInvoked = true
            assertTrue(result is SaveEventResult.Success)
        }
        advanceUntilIdle()
        assertTrue(callbackInvoked)
        assertEquals(listOf("saveEvent:Nuevo Evento Demo", "event:${repo.event.id}", "snapshot"), repo.calls)
        assertTrue(vm.uiState.value.message!!.contains("guardado correctamente"))
    }

    @Test fun saveEventUnauthorizedDisplaysFeedback() = runTest(dispatcher) {
        ready()
        repo.saveResult = SaveEventResult.Unauthorized
        val cmd = SaveDemoEvent(
            siteId = UUID.randomUUID(),
            name = "Evento No Autorizado",
            startsAt = Instant.EPOCH,
            endsAt = Instant.EPOCH.plusSeconds(3600),
        )
        vm.saveEvent(cmd)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.message!!.contains("No tienes permisos"))
    }
    private fun supervisorEditing() {
        val supervisor=repo.profile.copy(actor=repo.profile.actor.copy(role=LocalRole.SUPERVISOR))
        repo.state.value=DemoContextState.Ready(repo.base.copy(currentProfile=supervisor,selectedEvent=repo.event,
            establishmentSuggestion=EstablishmentSuggestion("Colegio ficticio","Comuna ficticia")))
        dispatcher.scheduler.runCurrent();vm.beginEventEditor(edit=true)
    }
    @Test fun editorRetainsFieldsOnErrorAndDiscardMustBeConfirmed()=runTest(dispatcher) {
        supervisorEditing();val original=vm.uiState.value.eventEditor!!
        vm.eventInput { it.copy(name="Evento corregido ficticio") }
        vm.dismissEventEditor();assertTrue(vm.uiState.value.confirmEventDiscard);vm.confirmEventDiscard(false)
        assertEquals("Evento corregido ficticio",vm.uiState.value.eventEditor!!.name)
        repo.saveResult=SaveEventResult.Unauthorized;vm.saveEventEditor()!!.join()
        assertEquals("Colegio ficticio",vm.uiState.value.eventEditor!!.establishment)
        assertEquals(original.startsAt,vm.uiState.value.eventEditor!!.startsAt)
        assertEquals(repo.event.id,repo.lastSaveCommand!!.eventId)
        assertTrue(vm.uiState.value.message!!.contains("No tienes permisos"))
        vm.dismissEventEditor();vm.confirmEventDiscard(true);assertNull(vm.uiState.value.eventEditor)
    }
    @Test fun pendingEventSaveBlocksWritesFieldChangesDismissAndContextSwitch()=runTest(dispatcher) {
        supervisorEditing();val original=vm.uiState.value.eventEditor!!
        repo.gate=CompletableDeferred();repo.saveResult=SaveEventResult.StorageUnavailable
        val save=vm.saveEventEditor()!!;runCurrent();assertTrue(vm.uiState.value.busy)
        assertNull(vm.saveEventEditor());vm.eventInput { it.copy(name="Tardío") };vm.dismissEventEditor()
        assertNull(vm.selectProfile(repo.other.actor.id));assertEquals(original,vm.uiState.value.eventEditor)
        assertFalse(vm.uiState.value.confirmEventDiscard)
        repo.gate!!.complete(Unit);save.join();assertFalse(vm.uiState.value.busy)
        assertEquals(original,vm.uiState.value.eventEditor)
        assertEquals(1,repo.calls.count { it.startsWith("saveEvent:") })
    }

}

/** Doble de contrato: no acredita permisos, Room ni persistencia real. */
private class FakeContextRepository : DemoContextRepository {
    val profile = DemoProfile(LocalActor(UUID(0, 1), LocalRole.CAPTOR, setOf(UUID(0, 2))), "Perfil A ficticio")
    val other = profile.copy(actor = profile.actor.copy(id = UUID(0, 5)), displayName = "Perfil B ficticio")
    val event = CaptureEvent(UUID(0, 3), UUID(0, 2), "Evento ficticio", Instant.EPOCH,
        Instant.EPOCH.plusSeconds(60), ZoneId.of("UTC"), setOf(profile.actor.id), null)
    val base = DemoContextSnapshot(UUID(0, 4), listOf(profile, other), null, listOf(event), null, null,
        DemoCatalog("test", "SYNTHETIC", "test", 2027, emptyList(), emptyList()), "test", "Texto ficticio")
    val state = MutableStateFlow<DemoContextState>(DemoContextState.NotInitialized)
    val calls = mutableListOf<String>()
    var result = ContextChange.APPLIED
    var gate: CompletableDeferred<Unit>? = null
    var terminateObservation = false
    var observers = 0
    override fun observe(): Flow<DemoContextState> = flow {
        observers++
        try { if (terminateObservation) emit(state.value) else emitAll(state) }
        finally { observers-- }
    }
    override suspend fun snapshot(): DemoContextState { calls += "snapshot"; return state.value }
    private suspend fun change(name: String, block: () -> Unit): ContextChange {
        calls += name; gate?.await()
        if (result == ContextChange.APPLIED) block()
        return result
    }
    override suspend fun initialize() = change("initialize") { state.value = DemoContextState.Ready(base) }
    override suspend fun selectProfile(id: UUID) = change("profile:$id") {
        state.value = DemoContextState.Ready(base.copy(currentProfile = base.profiles.single { it.actor.id == id }))
    }
    override suspend fun selectEvent(id: UUID) = change("event:$id") {
        state.value = DemoContextState.Ready(base.copy(currentProfile = profile, selectedEvent = event))
    }
    override suspend fun clearSelection() = change("clear") { state.value = DemoContextState.Ready(base) }
    override suspend fun currentActor(): LocalActor? = error("Fuera del alcance del ViewModel")
    override suspend fun environment(eventId: UUID): AdmissionEnvironment? = error("Fuera del alcance del ViewModel")

    var saveResult: SaveEventResult = SaveEventResult.Success(UUID(0, 3))
    var lastSaveCommand: SaveDemoEvent? = null
    override suspend fun saveEvent(command: SaveDemoEvent): SaveEventResult {
        calls += "saveEvent:${command.name}"
        gate?.await()
        lastSaveCommand = command
        return saveResult
    }
}
