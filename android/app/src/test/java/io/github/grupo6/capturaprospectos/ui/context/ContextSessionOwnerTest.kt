package io.github.grupo6.capturaprospectos.ui.context

import androidx.lifecycle.ViewModelStore
import io.github.grupo6.capturaprospectos.domain.admission.*
import io.github.grupo6.capturaprospectos.domain.capture.LocalActor
import io.github.grupo6.capturaprospectos.domain.capture.*
import io.github.grupo6.capturaprospectos.ui.admission.AdmissionFormState
import java.time.Instant
import java.time.ZoneOffset
import io.github.grupo6.capturaprospectos.domain.context.*
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class ContextSessionOwnerTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val repo = SessionRepository()
    private var closes = 0
    private var opens = 0
    private var activeSaves = 0
    private var admission: AdmissionRepository? = null
    private fun session() = object : ContextSession {
        override val repository = repo
        override val admissionRepository = admission
        override fun close() { assertEquals(0, repo.observers); assertEquals(0, activeSaves); closes++ }
    }
    private fun owner(open: () -> ContextSession = { opens++; session() }) =
        ContextSessionOwner(open, dispatcher).also { store.put("owner", it) }
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { store.clear(); dispatcher.scheduler.runCurrent(); Dispatchers.resetMain() }

    @Test fun openObservesWithoutSeedingAndClosesAfterConsumer() = runTest(dispatcher) {
        val vm = owner(); runCurrent()
        assertTrue(vm.uiState.value is ContextSessionState.Ready)
        assertEquals(1, repo.observers); assertEquals(0, repo.mutations)
        vm.retry(); runCurrent(); assertEquals(1, opens)
        store.clear(); advanceUntilIdle()
        assertEquals(1, closes); assertEquals(0, repo.observers)
    }
    @Test fun failedOpenNeedsExplicitRetryAndDoesNotReset() = runTest(dispatcher) {
        val vm = owner { opens++; if (opens == 1) throw ContextOpenFailure() else session() }
        runCurrent(); assertEquals(ContextSessionState.Unavailable, vm.uiState.value)
        assertEquals(1, opens); assertEquals(0, repo.mutations)
        vm.retry(); vm.retry(); runCurrent()
        assertTrue(vm.uiState.value is ContextSessionState.Ready); assertEquals(2, opens)
    }
    @Test fun cancellationBeforeOpenDoesNotAcquireResource() = runTest(dispatcher) {
        owner(); store.clear(); advanceUntilIdle()
        assertEquals(0, opens); assertEquals(0, closes)
    }
    @Test fun cancellationWhileReturningResourceStillClosesIt() = runTest(dispatcher) {
        owner { opens++; store.clear(); session() }; advanceUntilIdle()
        assertEquals(1, opens); assertEquals(1, closes); assertEquals(0, repo.observers)
    }
    @Test fun formIsRetainedUntilExplicitExitThenItsMemoryIsCleared() = runTest(dispatcher) {
        val owner = owner(); runCurrent()
        val env = formEnvironment()
        val form = owner.form(env, null)
        form.text(AdmissionField.GIVEN_NAMES, "Persona ficticia")
        assertSame(form, owner.form(env, null))
        assertEquals("Persona ficticia", owner.form(env, null).uiState.value.input.givenNames)
        assertTrue(owner.releaseForm())
        assertEquals(AdmissionFormState(), form.uiState.value)
        assertNotSame(form, owner.form(env, null))
        val next = owner.form(env, null)
        next.text(AdmissionField.GIVEN_NAMES, "Otro ficticio")
        store.clear(); advanceUntilIdle()
        assertEquals(AdmissionFormState(), next.uiState.value)
        assertEquals(1, closes)
    }
    @Test fun closingOwnerCancelsAndWaitsForFormBeforeClosingDatabase() = runTest(dispatcher) {
        admission = object : AdmissionRepository {
            override suspend fun create(request: NewAdmission): CreateAdmissionResult {
                activeSaves++
                try { awaitCancellation() }
                finally { withContext(NonCancellable) { delay(20); activeSaves-- } }
            }
            override suspend fun get(id: UUID) = ReadAdmissionResult.Unavailable
        }
        val owner = owner(); runCurrent()
        val form = owner.form(formEnvironment(), null)
        form.mode(AcceptanceMode.DIRECT); form.consent(true)
        form.save(SaveIntent.SAVE_DRAFT); runCurrent()
        assertEquals(1, activeSaves)
        assertFalse(owner.releaseForm())
        store.clear(); runCurrent()
        assertEquals(0, closes)
        advanceUntilIdle()
        assertEquals(0, activeSaves); assertEquals(1, closes)
        assertEquals(AdmissionFormState(), form.uiState.value)
    }
    private fun formEnvironment(): AdmissionEnvironment {
        val actor = LocalActor(UUID(0, 1), LocalRole.CAPTOR, setOf(UUID(0, 2)))
        val event = CaptureEvent(UUID(0, 3), UUID(0, 2), "Ficticio", Instant.EPOCH,
            Instant.EPOCH.plusSeconds(3600), ZoneOffset.UTC, setOf(actor.id), null)
        return AdmissionEnvironment(CaptureEnvironment(actor, event, UUID(0, 4),
            emptyList(), "v1", "Consentimiento ficticio"), emptyList(), emptyList())
    }
}
private class SessionRepository : DemoContextRepository {
    var observers = 0
    var mutations = 0
    override fun observe() = flow<DemoContextState> {
        observers++
        try { emit(DemoContextState.NotInitialized); awaitCancellation() } finally { observers-- }
    }
    override suspend fun snapshot() = DemoContextState.NotInitialized
    override suspend fun initialize(): ContextChange { mutations++; return ContextChange.APPLIED }
    override suspend fun selectProfile(id: UUID) = error("Sin selección implícita")
    override suspend fun selectEvent(id: UUID) = error("Sin selección implícita")
    override suspend fun clearSelection() = error("Sin reset implícito")
    override suspend fun currentActor(): LocalActor? = null
    override suspend fun environment(eventId: UUID): AdmissionEnvironment? = null
    override suspend fun saveEvent(command: io.github.grupo6.capturaprospectos.domain.context.SaveDemoEvent): io.github.grupo6.capturaprospectos.domain.context.SaveEventResult =
        io.github.grupo6.capturaprospectos.domain.context.SaveEventResult.Success(UUID.randomUUID())
}
