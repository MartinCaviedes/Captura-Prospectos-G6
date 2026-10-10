package io.github.grupo6.capturaprospectos.ui.admission

import io.github.grupo6.capturaprospectos.domain.admission.*
import io.github.grupo6.capturaprospectos.domain.capture.*
import io.github.grupo6.capturaprospectos.domain.context.EstablishmentSuggestion
import io.github.grupo6.capturaprospectos.domain.device.*
import java.time.Clock
import java.time.Duration
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
class AdmissionFormDeviceTest {
    private val dispatcher = StandardTestDispatcher()
    private val now = Instant.parse("2026-10-10T16:00:00Z")
    private val actor = LocalActor(UUID(0, 1), LocalRole.CAPTOR, setOf(UUID(0, 2)))
    private val eventCoords = Coordinates(-33.0245, -71.5518)
    private val event = CaptureEvent(
        UUID(0, 3), UUID(0, 2), "Evento de prueba", now, now.plusSeconds(7200), ZoneOffset.UTC, setOf(actor.id), eventCoords
    )
    private val programs = (10L..12L).map { ProgramOption(UUID(0, it), "Carrera $it", "v1", "Sec") }
    private val sites = (20L..21L).map { SiteOption(UUID(0, it), "Sede $it", "v1") }
    private val environment = AdmissionEnvironment(
        CaptureEnvironment(actor, event, UUID(0, 99), emptyList(), "v1", "Texto de consentimiento"),
        programs,
        sites
    )

    private val fakeRepo = FakeAdmissionRepository()

    private fun vm(
        repo: AdmissionRepository? = fakeRepo,
        locationProvider: LocationProvider? = null,
        qrParser: QrParser = DemoQrParser(),
    ) = AdmissionFormViewModel(
        environment = environment,
        suggestion = EstablishmentSuggestion("Colegio Central", "Viña del Mar"),
        clock = Clock.fixed(now, ZoneOffset.UTC),
        repository = repo,
        locationProvider = locationProvider,
        qrParser = qrParser,
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
    fun requestLocationAppliesCandidateWhenValid() = runTest(dispatcher) {
        val deviceLoc = CaptureLocation.Device(
            coordinates = eventCoords,
            measuredAt = now.minusSeconds(10),
            accuracyMeters = 15.0,
        )
        val provider = LocationProvider { LocationResult.Candidate(deviceLoc) }
        val vm = vm(locationProvider = provider)

        vm.requestLocation()
        runCurrent()

        val state = vm.uiState.value
        assertFalse(state.isAcquiringLocation)
        assertEquals(deviceLoc, state.location)
        assertTrue(state.locationStatus!!.contains("Ubicación GPS confirmada"))
    }

    @Test
    fun requestLocationReportsPermissionDenied() = runTest(dispatcher) {
        val provider = LocationProvider { LocationResult.PermissionDenied }
        val vm = vm(locationProvider = provider)

        vm.requestLocation()
        runCurrent()

        val state = vm.uiState.value
        assertFalse(state.isAcquiringLocation)
        assertNull(state.location)
        assertTrue(state.locationStatus!!.contains("Permiso de ubicación denegado"))
    }

    @Test
    fun requestLocationReportsServicesDisabled() = runTest(dispatcher) {
        val provider = LocationProvider { LocationResult.ServicesDisabled }
        val vm = vm(locationProvider = provider)

        vm.requestLocation()
        runCurrent()

        val state = vm.uiState.value
        assertFalse(state.isAcquiringLocation)
        assertNull(state.location)
        assertTrue(state.locationStatus!!.contains("desactivados"))
    }

    @Test
    fun requestLocationReportsTimedOut() = runTest(dispatcher) {
        val provider = LocationProvider { LocationResult.TimedOut }
        val vm = vm(locationProvider = provider)

        vm.requestLocation()
        runCurrent()

        val state = vm.uiState.value
        assertFalse(state.isAcquiringLocation)
        assertNull(state.location)
        assertTrue(state.locationStatus!!.contains("Tiempo de espera agotado"))
    }

    @Test
    fun confirmEventLocationSetsConfirmedLocation() = runTest(dispatcher) {
        val vm = vm()
        vm.confirmEventLocation()

        val state = vm.uiState.value
        val loc = state.location as? CaptureLocation.EventConfirmed
        assertNotNull(loc)
        assertEquals(eventCoords, loc!!.coordinates)
        assertEquals(actor.id, loc.confirmedBy)
        assertTrue(state.locationStatus!!.contains("sede del evento"))
    }

    @Test
    fun clearLocationRemovesLocation() = runTest(dispatcher) {
        val vm = vm()
        vm.confirmEventLocation()
        assertNotNull(vm.uiState.value.location)

        vm.clearLocation()
        assertNull(vm.uiState.value.location)
        assertNull(vm.uiState.value.locationStatus)
    }

    @Test
    fun qrScannerParsesValidPayloadAndAppliesCandidate() = runTest(dispatcher) {
        val vm = vm()
        vm.openQrScanner()
        assertTrue(vm.uiState.value.isQrScannerOpen)

        vm.processQrPayload("G6DEMO:1:{\"fullName\":\"Persona Ficticia Alfa\",\"rut\":\"12345678-5\"}")
        val stateWithCandidate = vm.uiState.value
        assertNotNull(stateWithCandidate.qrCandidate)
        assertEquals("12345678-5", stateWithCandidate.qrCandidate!!.rut)
        assertEquals("Persona Ficticia Alfa", stateWithCandidate.qrCandidate!!.fullName)

        vm.applyQrCandidate()
        val finalState = vm.uiState.value
        assertFalse(finalState.isQrScannerOpen)
        assertNull(finalState.qrCandidate)
        assertEquals("12345678-5", finalState.input.rut)
        assertEquals("Persona Ficticia Alfa", finalState.input.givenNames)
    }

    @Test
    fun qrScannerReportsErrorOnUnsupportedOrInvalidPayload() = runTest(dispatcher) {
        val vm = vm()
        vm.openQrScanner()

        vm.processQrPayload("https://malicious.test")
        assertNull(vm.uiState.value.qrCandidate)
        assertTrue(vm.uiState.value.qrError!!.contains("no compatible"))

        vm.processQrPayload("G6DEMO:1:invalid-json")
        assertNull(vm.uiState.value.qrCandidate)
        assertTrue(vm.uiState.value.qrError!!.contains("inválido"))
    }

    @Test
    fun closeQrScannerClearsCandidateWithoutApplying() = runTest(dispatcher) {
        val vm = vm()
        vm.openQrScanner()
        vm.processQrPayload("G6DEMO:1:{\"fullName\":\"Persona Ficticia Alfa\",\"rut\":\"12345678-5\"}")
        assertNotNull(vm.uiState.value.qrCandidate)

        vm.closeQrScanner()
        assertFalse(vm.uiState.value.isQrScannerOpen)
        assertNull(vm.uiState.value.qrCandidate)
        assertEquals("", vm.uiState.value.input.rut)
    }
}
