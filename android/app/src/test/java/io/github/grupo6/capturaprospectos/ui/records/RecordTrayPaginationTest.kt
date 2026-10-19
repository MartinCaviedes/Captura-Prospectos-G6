package io.github.grupo6.capturaprospectos.ui.records

import io.github.grupo6.capturaprospectos.domain.capture.*
import io.github.grupo6.capturaprospectos.domain.records.*
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class RecordTrayPaginationTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }
    private fun row(i: Long) = RecordSummary(UUID(0, i), 1, Instant.EPOCH, UUID(0, 99),
        "Persona ficticia $i", "", CaptureState.DRAFT, RecordContract.F2027)
    private class Repo : RecordRepository {
        val queries = mutableListOf<RecordQuery>()
        var listResult: suspend (RecordQuery) -> RecordPageResult = { RecordPageResult.Page(emptyList(), null) }
        var mutation: suspend () -> RecordMutationResult = { RecordMutationResult.StorageUnavailable }
        var cancellations = 0
        override suspend fun get(id: java.util.UUID): io.github.grupo6.capturaprospectos.domain.admission.ReadAdmissionResult = io.github.grupo6.capturaprospectos.domain.admission.ReadAdmissionResult.Unavailable
            override suspend fun list(query: RecordQuery): RecordPageResult { queries += query; return listResult(query) }
        override suspend fun cancel(command: CancelRecord): RecordMutationResult { cancellations++; return mutation() }
        override suspend fun edit(command: EditAdmission) = RecordMutationResult.UnsupportedContract
    }
    @Test fun nextPageUsesExclusiveCursorAndChangingFilterRestartsIt() = runTest(dispatcher) {
        val repo = Repo()
        val cursor = RecordCursor(Instant.EPOCH, UUID(0, 50))
        repo.listResult = { q -> if (q.after == null) RecordPageResult.Page((1L..50L).map(::row), cursor)
            else RecordPageResult.Page((51L..75L).map(::row), null) }
        val vm = RecordTrayViewModel(repo); runCurrent()
        assertEquals(50, vm.uiState.value.items.size)
        vm.loadMore()!!.join()
        assertEquals(cursor, repo.queries.last().after)
        assertEquals(75, vm.uiState.value.items.size)
        assertNull(vm.loadMore())
        vm.updateQuery("Persona")!!.join()
        assertNull(repo.queries.last().after)
        assertEquals(50, vm.uiState.value.items.size)
    }
    @Test fun slowObsoleteReadCannotReplaceNewFilterResults() = runTest(dispatcher) {
        val repo = Repo()
        val old = CompletableDeferred<RecordPageResult>()
        repo.listResult = { q -> if (q.filter.query.isBlank()) withContext(NonCancellable) { old.await() }
            else RecordPageResult.Page(listOf(row(2)), null) }
        val vm = RecordTrayViewModel(repo); runCurrent()
        vm.updateQuery("nueva")!!.join()
        old.complete(RecordPageResult.Page(listOf(row(1)), null)); runCurrent()
        assertEquals(listOf(UUID(0, 2)), vm.uiState.value.items.map { it.id })
        assertEquals("nueva", vm.uiState.value.filter.query)
    }
    @Test fun pendingCancellationCannotBeRepeatedOrDiscardedAndFailureKeepsDialog() = runTest(dispatcher) {
        val repo = Repo(); val result = CompletableDeferred<RecordMutationResult>()
        repo.mutation = { result.await() }
        val vm = RecordTrayViewModel(repo); runCurrent()
        vm.openCancelDialog(row(1)); val job = vm.confirmCancellation("Motivo ficticio")!!; runCurrent()
        vm.closeCancelDialog(); assertNotNull(vm.uiState.value.targetForCancellation)
        assertNull(vm.confirmCancellation("Segundo motivo"))
        assertNull(vm.updateQuery("otro")); assertTrue(vm.uiState.value.isMutating)
        result.complete(RecordMutationResult.StorageUnavailable); job.join()
        assertEquals(1, repo.cancellations); assertFalse(vm.uiState.value.isMutating)
        assertNotNull(vm.uiState.value.targetForCancellation)
    }
    @Test fun failedNextPagePreservesItemsAndCursorForRetry() = runTest(dispatcher) {
        val repo = Repo(); val cursor = RecordCursor(Instant.EPOCH, UUID(0, 1))
        repo.listResult = { q -> if (q.after == null) RecordPageResult.Page(listOf(row(1)), cursor) else RecordPageResult.StorageUnavailable }
        val vm = RecordTrayViewModel(repo); runCurrent(); vm.loadMore()!!.join()
        assertEquals(listOf(row(1)), vm.uiState.value.items); assertEquals(cursor, vm.uiState.value.nextCursor)
        assertTrue(vm.uiState.value.message!!.contains("no disponible"))
    }
}
