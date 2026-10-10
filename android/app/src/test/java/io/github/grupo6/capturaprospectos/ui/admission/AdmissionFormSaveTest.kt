package io.github.grupo6.capturaprospectos.ui.admission

import io.github.grupo6.capturaprospectos.domain.admission.*
import io.github.grupo6.capturaprospectos.domain.capture.*
import io.github.grupo6.capturaprospectos.domain.context.EstablishmentSuggestion
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdmissionFormSaveTest {
    private val dispatcher = StandardTestDispatcher()
    private val now = Instant.parse("2026-10-10T15:00:00Z")
    private val actor = LocalActor(UUID(0, 1), LocalRole.CAPTOR, setOf(UUID(0, 2)))
    private val event = CaptureEvent(
        UUID(0, 3), UUID(0, 2), "Evento de prueba", now, now.plusSeconds(7200), ZoneOffset.UTC, setOf(actor.id), null
    )
    private val programs = (10L..12L).map { ProgramOption(UUID(0, it), "Carrera $it", "v1", "Sec") }
    private val sites = (20L..21L).map { SiteOption(UUID(0, it), "Sede $it", "v1") }
    private val environment = AdmissionEnvironment(
        CaptureEnvironment(actor, event, UUID(0, 99), emptyList(), "v1", "Texto de consentimiento"),
        programs,
        sites
    )

    private val fakeRepo = FakeAdmissionRepository()

    private fun vm(repo: AdmissionRepository? = fakeRepo) = AdmissionFormViewModel(
        environment = environment,
        suggestion = EstablishmentSuggestion("Colegio Central", "Viña del Mar"),
        clock = Clock.fixed(now, ZoneOffset.UTC),
        repository = repo
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun saveWithoutRepositoryReportsError() = runTest(dispatcher) {
        val vm = vm(repo = null)
        vm.save(SaveIntent.CONFIRM_REGISTRATION)
        runCurrent()
        assertFalse(vm.uiState.value.isSaved)
        assertTrue(vm.uiState.value.message!!.contains("Repositorio local no disponible"))
    }

    @Test
    fun saveWithValidInputAndConsentPersistsReady() = runTest(dispatcher) {
        val saveId = UUID.randomUUID()
        fakeRepo.resultToReturn = CreateAdmissionResult.Saved(saveId, 1, CaptureState.READY)
        val vm = vm()

        vm.mode(AcceptanceMode.DIRECT)
        vm.consent(true)
        val job = vm.save(SaveIntent.CONFIRM_REGISTRATION)
        job?.join()
        runCurrent()

        val state = vm.uiState.value
        assertTrue(state.isSaved)
        assertEquals(saveId, state.savedId)
        assertEquals(CaptureState.READY, state.savedState)
        assertTrue(state.message!!.contains("guardada localmente"))
        assertNotNull(fakeRepo.lastRequest)
        assertEquals(SaveIntent.CONFIRM_REGISTRATION, fakeRepo.lastRequest?.intent)
    }

    @Test
    fun saveDraftPersistsDraft() = runTest(dispatcher) {
        val saveId = UUID.randomUUID()
        fakeRepo.resultToReturn = CreateAdmissionResult.Saved(saveId, 1, CaptureState.DRAFT)
        val vm = vm()

        vm.mode(AcceptanceMode.VERBAL)
        vm.consent(true)
        val job = vm.save(SaveIntent.SAVE_DRAFT)
        job?.join()
        runCurrent()

        val state = vm.uiState.value
        assertTrue(state.isSaved)
        assertEquals(saveId, state.savedId)
        assertEquals(CaptureState.DRAFT, state.savedState)
        assertEquals(SaveIntent.SAVE_DRAFT, fakeRepo.lastRequest?.intent)
    }

    @Test
    fun saveDuplicateOwnReportsDuplicateError() = runTest(dispatcher) {
        val existingId = UUID.randomUUID()
        fakeRepo.resultToReturn = CreateAdmissionResult.DuplicateOwn(existingId)
        val vm = vm()

        vm.mode(AcceptanceMode.DIRECT)
        vm.consent(true)
        val job = vm.save(SaveIntent.CONFIRM_REGISTRATION)
        job?.join()
        runCurrent()

        val state = vm.uiState.value
        assertFalse(state.isSaved)
        assertTrue(state.message!!.contains("Ya existe una inscripción registrada"))
    }

    @Test
    fun saveDuplicateRestrictedReportsGenericErrorWithoutPii() = runTest(dispatcher) {
        fakeRepo.resultToReturn = CreateAdmissionResult.DuplicateRestricted
        val vm = vm()

        vm.mode(AcceptanceMode.DIRECT)
        vm.consent(true)
        val job = vm.save(SaveIntent.CONFIRM_REGISTRATION)
        job?.join()
        runCurrent()

        val state = vm.uiState.value
        assertFalse(state.isSaved)
        assertTrue(state.message!!.contains("ya se encuentra registrado"))
        assertFalse(state.message!!.contains("00000000"))
    }

    @Test
    fun saveStorageUnavailablePreservesInputWithoutCrash() = runTest(dispatcher) {
        fakeRepo.resultToReturn = CreateAdmissionResult.StorageUnavailable
        val vm = vm()

        vm.text(AdmissionField.GIVEN_NAMES, "Nombre Prospecto")
        vm.mode(AcceptanceMode.DIRECT)
        vm.consent(true)
        val job = vm.save(SaveIntent.CONFIRM_REGISTRATION)
        job?.join()
        runCurrent()

        val state = vm.uiState.value
        assertFalse(state.isSaved)
        assertEquals("Nombre Prospecto", state.input.givenNames)
        assertTrue(state.message!!.contains("Almacenamiento local no disponible"))
    }

    @Test
    fun saveConsentRequiredSetsIssue() = runTest(dispatcher) {
        fakeRepo.resultToReturn = CreateAdmissionResult.ConsentRequired
        val vm = vm()

        val job = vm.save(SaveIntent.CONFIRM_REGISTRATION)
        job?.join()
        runCurrent()

        val state = vm.uiState.value
        assertFalse(state.isSaved)
        assertEquals(AdmissionField.CONSENT, state.issues.single().field)
    }

    @Test
    fun resetClearsFormAndSavedState() = runTest(dispatcher) {
        val saveId = UUID.randomUUID()
        fakeRepo.resultToReturn = CreateAdmissionResult.Saved(saveId, 1, CaptureState.READY)
        val vm = vm()

        vm.mode(AcceptanceMode.DIRECT)
        vm.consent(true)
        vm.save(SaveIntent.CONFIRM_REGISTRATION)?.join()
        runCurrent()
        assertTrue(vm.uiState.value.isSaved)

        vm.reset()
        assertEquals(AdmissionFormState(), vm.uiState.value)
    }

    @Test
    fun editingInputResetsSavedState() = runTest(dispatcher) {
        val saveId = UUID.randomUUID()
        fakeRepo.resultToReturn = CreateAdmissionResult.Saved(saveId, 1, CaptureState.READY)
        val vm = vm()

        vm.mode(AcceptanceMode.DIRECT)
        vm.consent(true)
        vm.save(SaveIntent.CONFIRM_REGISTRATION)?.join()
        runCurrent()
        assertTrue(vm.uiState.value.isSaved)

        vm.text(AdmissionField.GIVEN_NAMES, "Nuevo nombre")
        assertFalse(vm.uiState.value.isSaved)
        assertNull(vm.uiState.value.savedId)
    }
}

private class FakeAdmissionRepository : AdmissionRepository {
    var lastRequest: NewAdmission? = null
    var resultToReturn: CreateAdmissionResult = CreateAdmissionResult.StorageUnavailable

    override suspend fun create(request: NewAdmission): CreateAdmissionResult {
        lastRequest = request
        return resultToReturn
    }

    override suspend fun get(id: UUID): ReadAdmissionResult = ReadAdmissionResult.Unavailable
}
