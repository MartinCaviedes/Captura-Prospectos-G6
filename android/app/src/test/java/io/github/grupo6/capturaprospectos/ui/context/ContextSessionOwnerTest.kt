package io.github.grupo6.capturaprospectos.ui.context

import androidx.lifecycle.ViewModelStore
import io.github.grupo6.capturaprospectos.domain.admission.AdmissionEnvironment
import io.github.grupo6.capturaprospectos.domain.capture.LocalActor
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
    private fun session() = object : ContextSession {
        override val repository = repo
        override fun close() { assertEquals(0, repo.observers); closes++ }
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
}
