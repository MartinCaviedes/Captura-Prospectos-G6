package io.github.grupo6.capturaprospectos.ui.records

import io.github.grupo6.capturaprospectos.domain.admission.*
import io.github.grupo6.capturaprospectos.domain.capture.*
import io.github.grupo6.capturaprospectos.domain.records.*
import java.time.Instant
import java.time.ZoneId
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

    private val sampleId1 = UUID.randomUUID()
    private val sampleSummary1 = RecordSummary(
        id = sampleId1,
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

    private val sampleAdmission1 = StoredAdmission(
        id = sampleId1,
        revision = 1,
        ownerId = UUID.randomUUID(),
        siteId = UUID.randomUUID(),
        eventId = UUID.randomUUID(),
        deviceId = UUID.randomUUID(),
        capturedAt = Instant.now(),
        eventName = "Feria Central",
        eventTimeZone = ZoneId.of("America/Santiago"),
        input = AdmissionInput(
            givenNames = "Prospecto",
            familyNames = "Alfa",
            rut = "12345678-5",
            email = "alfa@correo.test",
            phone = "+56912345678",
            birthDate = "2006-03-15",
            residenceCommune = "Viña del Mar",
        ),
        fullNameSnapshot = "Prospecto Alfa",
        consent = ConsentEvidence("v1", Instant.now(), UUID.randomUUID(), AcceptanceMode.DIRECT),
        programs = emptyList(),
        sites = emptyList(),
        location = null,
        state = CaptureState.READY,
        transportState = TransportState.NOT_QUEUED,
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        fakeRepo.listResult = RecordPageResult.Page(listOf(sampleSummary1, sampleSummary2), null)
        fakeRepo.getResult = ReadAdmissionResult.Found(sampleAdmission1)
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
    fun openDetailLoadsStoredAdmissionAndOpensDialog() = runTest(dispatcher) {
        val vm = RecordTrayViewModel(fakeRepo)
        runCurrent()

        vm.openDetail(sampleId1)
        runCurrent()

        val state = vm.uiState.value
        assertTrue(state.isDetailOpen)
        assertNotNull(state.selectedRecord)
        assertEquals(sampleId1, state.selectedRecord?.id)
        assertFalse(state.isEditing)
    }

    @Test
    fun closeDetailResetsSelectedRecord() = runTest(dispatcher) {
        val vm = RecordTrayViewModel(fakeRepo)
        runCurrent()

        vm.openDetail(sampleId1)
        runCurrent()
        assertTrue(vm.uiState.value.isDetailOpen)

        vm.closeDetail()
        assertFalse(vm.uiState.value.isDetailOpen)
        assertNull(vm.uiState.value.selectedRecord)
    }

    @Test
    fun toggleEditState() = runTest(dispatcher) {
        val vm = RecordTrayViewModel(fakeRepo)
        runCurrent()

        vm.openDetail(sampleId1)
        runCurrent()

        vm.startEditing()
        assertTrue(vm.uiState.value.isEditing)

        vm.cancelEditing()
        assertFalse(vm.uiState.value.isEditing)
    }

    @Test
    fun saveEditSuccessUpdatesMessageAndReloads() = runTest(dispatcher) {
        fakeRepo.editResult = RecordMutationResult.Saved(sampleId1, 2, CaptureState.READY)
        val vm = RecordTrayViewModel(fakeRepo)
        runCurrent()

        vm.openDetail(sampleId1)
        runCurrent()

        val editedInput = sampleAdmission1.input.copy(email = "nuevo@correo.test")
        vm.saveEdit(editedInput, "Corrección de correo")?.join()
        runCurrent()

        val state = vm.uiState.value
        assertFalse(state.isEditing)
        assertTrue(state.message!!.contains("modificada correctamente"))
        assertEquals(sampleId1, fakeRepo.lastEdit?.id)
        assertEquals("nuevo@correo.test", fakeRepo.lastEdit?.input?.email)
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

    var getResult: ReadAdmissionResult = ReadAdmissionResult.Unavailable

    var lastCancel: CancelRecord? = null
    var cancelResult: RecordMutationResult = RecordMutationResult.StorageUnavailable

    var lastEdit: EditAdmission? = null
    var editResult: RecordMutationResult = RecordMutationResult.StorageUnavailable

    override suspend fun list(query: RecordQuery): RecordPageResult {
        lastQuery = query
        return listResult
    }

    override suspend fun get(id: UUID): ReadAdmissionResult = getResult

    override suspend fun cancel(command: CancelRecord): RecordMutationResult {
        lastCancel = command
        return cancelResult
    }

    override suspend fun edit(command: EditAdmission): RecordMutationResult {
        lastEdit = command
        return editResult
    }
}
