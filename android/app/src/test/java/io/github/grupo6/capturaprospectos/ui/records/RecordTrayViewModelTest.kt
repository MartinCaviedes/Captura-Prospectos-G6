package io.github.grupo6.capturaprospectos.ui.records

import io.github.grupo6.capturaprospectos.domain.capture.CaptureState
import io.github.grupo6.capturaprospectos.domain.records.*
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecordTrayViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val fakeRepo = FakeRecordRepository()

    private val sampleSummary1 = RecordSummary(
        id = UUID.randomUUID(),
        revision = 1,
        capturedAt = Instant.now(),
        eventId = UUID.randomUUID(),
        displayName = "Prospecto Alfa",
        rut = "12345678-5",
        state = CaptureState.READY,
        contract = RecordContract.F2027,
    )
    private val sampleSummary2 = RecordSummary(
        id = UUID.randomUUID(),
        revision = 1,
        capturedAt = Instant.now().minusSeconds(60),
        eventId = UUID.randomUUID(),
        displayName = "Prospecto Beta",
        rut = "87654321-4",
        state = CaptureState.DRAFT,
        contract = RecordContract.F2027,
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        fakeRepo.listResult = RecordPageResult.Page(listOf(sampleSummary1, sampleSummary2), null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadInitialPopulatesItems() = runTest(dispatcher) {
        val vm = RecordTrayViewModel(fakeRepo)
        runCurrent()

        val state = vm.uiState.value
        assertEquals(2, state.items.size)
        assertFalse(state.isLoading)
        assertNull(state.message)
    }

    @Test
    fun updateQueryUpdatesFilterAndReloads() = runTest(dispatcher) {
        val vm = RecordTrayViewModel(fakeRepo)
        runCurrent()

        vm.updateQuery("Alfa")
        runCurrent()

        assertEquals("Alfa", vm.uiState.value.filter.query)
        assertEquals("Alfa", fakeRepo.lastQuery?.filter?.query)
    }

    @Test
    fun filterStateUpdatesStatesAndReloads() = runTest(dispatcher) {
        val vm = RecordTrayViewModel(fakeRepo)
        runCurrent()

        vm.filterState(CaptureState.READY)
        runCurrent()

        assertEquals(setOf(CaptureState.READY), vm.uiState.value.filter.states)
        assertEquals(setOf(CaptureState.READY), fakeRepo.lastQuery?.filter?.states)

        vm.filterState(null)
        runCurrent()
        assertTrue(vm.uiState.value.filter.states.isEmpty())
    }

    @Test
    fun openAndCloseCancelDialog() = runTest(dispatcher) {
        val vm = RecordTrayViewModel(fakeRepo)
        runCurrent()

        vm.openCancelDialog(sampleSummary1)
        assertEquals(sampleSummary1, vm.uiState.value.targetForCancellation)

        vm.closeCancelDialog()
        assertNull(vm.uiState.value.targetForCancellation)
    }

    @Test
    fun confirmCancellationWithShortReasonFails() = runTest(dispatcher) {
        val vm = RecordTrayViewModel(fakeRepo)
        runCurrent()

        vm.openCancelDialog(sampleSummary1)
        vm.confirmCancellation("123") // 3 chars < 5
        runCurrent()

        assertNotNull(vm.uiState.value.targetForCancellation)
        assertTrue(vm.uiState.value.message!!.contains("entre 5 y 300 caracteres"))
        assertNull(fakeRepo.lastCancel)
    }

    @Test
    fun confirmCancellationSuccessUpdatesMessageAndReloads() = runTest(dispatcher) {
        fakeRepo.cancelResult = RecordMutationResult.Saved(sampleSummary1.id, 2, CaptureState.CANCELLED)
        val vm = RecordTrayViewModel(fakeRepo)
        runCurrent()

        vm.openCancelDialog(sampleSummary1)
        vm.confirmCancellation("Registro duplicado en feria")?.join()
        runCurrent()

        assertNull(vm.uiState.value.targetForCancellation)
        assertTrue(vm.uiState.value.message!!.contains("anulada correctamente"))
        assertEquals(sampleSummary1.id, fakeRepo.lastCancel?.id)
        assertEquals("Registro duplicado en feria", fakeRepo.lastCancel?.reason)
    }

    @Test
    fun confirmCancellationConflictReportsConflict() = runTest(dispatcher) {
        fakeRepo.cancelResult = RecordMutationResult.RevisionConflict
        val vm = RecordTrayViewModel(fakeRepo)
        runCurrent()

        vm.openCancelDialog(sampleSummary1)
        vm.confirmCancellation("Registro duplicado en feria")?.join()
        runCurrent()

        assertNull(vm.uiState.value.targetForCancellation)
        assertTrue(vm.uiState.value.message!!.contains("modificado externamente"))
    }

    @Test
    fun nullRepositoryHandlesGracefully() = runTest(dispatcher) {
        val vm = RecordTrayViewModel(repository = null)
        runCurrent()

        assertTrue(vm.uiState.value.items.isEmpty())
        assertFalse(vm.uiState.value.isLoading)

        vm.openCancelDialog(sampleSummary1)
        vm.confirmCancellation("Motivo válido de prueba")
        runCurrent()

        assertTrue(vm.uiState.value.message!!.contains("Repositorio local no disponible"))
    }
}

private class FakeRecordRepository : RecordRepository {
    var lastQuery: RecordQuery? = null
    var listResult: RecordPageResult = RecordPageResult.Page(emptyList(), null)

    var lastCancel: CancelRecord? = null
    var cancelResult: RecordMutationResult = RecordMutationResult.StorageUnavailable

    var lastEdit: EditAdmission? = null
    var editResult: RecordMutationResult = RecordMutationResult.StorageUnavailable

    override suspend fun list(query: RecordQuery): RecordPageResult {
        lastQuery = query
        return listResult
    }

    override suspend fun cancel(command: CancelRecord): RecordMutationResult {
        lastCancel = command
        return cancelResult
    }

    override suspend fun edit(command: EditAdmission): RecordMutationResult {
        lastEdit = command
        return editResult
    }
}
