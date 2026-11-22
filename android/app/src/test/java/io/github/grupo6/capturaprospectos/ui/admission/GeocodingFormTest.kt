package io.github.grupo6.capturaprospectos.ui.admission

import androidx.lifecycle.ViewModelStore
import io.github.grupo6.capturaprospectos.domain.admission.*
import io.github.grupo6.capturaprospectos.domain.capture.*
import io.github.grupo6.capturaprospectos.domain.device.*
import java.time.*
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class GeocodingFormTest {
    private val dispatcher=StandardTestDispatcher()
    private val store=ViewModelStore()
    private var now=Instant.parse("2026-11-22T16:00:00Z")
    private val clock=object:Clock() {
        override fun getZone():ZoneId=ZoneOffset.UTC
        override fun withZone(zone:ZoneId):Clock=this
        override fun instant():Instant=now
    }
    private val actor=LocalActor(UUID(0,1),LocalRole.CAPTOR,setOf(UUID(0,2)))
    private val position=CaptureLocation.Device(Coordinates(-33.0,-71.0),now,10.0)
    private val event=CaptureEvent(UUID(0,3),UUID(0,2),"Evento ficticio",now,now.plusSeconds(3600),ZoneOffset.UTC,setOf(actor.id),position.coordinates)
    private val environment=AdmissionEnvironment(CaptureEnvironment(actor,event,UUID(0,4),emptyList(),"v1","Texto ficticio"),emptyList(),emptyList())
    private var calls=0
    private var addressCalls=0
    private var gate:CompletableDeferred<Unit>?=null
    private var result:GeocodingResult=GeocodingResult.Found("Localidad ficticia, Chile")
    private val geocoder=ReverseGeocoder { calls++;gate?.await();result }
    private val addressGeocoder=ReverseGeocoder { addressCalls++;gate?.await();GeocodingResult.Found("Calle ficticia 123") }
    private val repo=object:AdmissionRepository {
        var saved:NewAdmission?=null
        override suspend fun create(request:NewAdmission):CreateAdmissionResult {saved=request;return CreateAdmissionResult.Saved(UUID(0,9),1,CaptureState.DRAFT)}
        override suspend fun get(id:UUID)=ReadAdmissionResult.Unavailable
    }
    private fun model()=AdmissionFormViewModel(environment,clock=clock,repository=repo,
        locationProvider=LocationProvider { LocationResult.Candidate(position) },reverseGeocoder=geocoder,addressGeocoder=addressGeocoder).also {store.put(UUID.randomUUID().toString(),it)}
    private fun tick()=dispatcher.scheduler.runCurrent()
    private fun confirm(vm:AdmissionFormViewModel) {vm.requestLocation();tick();vm.confirmDeviceLocation()}
    @Before fun setup() {Dispatchers.setMain(dispatcher)}
    @After fun cleanup() {store.clear();tick();Dispatchers.resetMain()}
    @Test fun noAutomaticRequestOnOpenGpsAcquisitionOrConfirmation() {
        val vm=model();assertEquals(0,calls);vm.requestLocation();tick()
        vm.lookupLocality();tick();assertEquals(0,calls)
        vm.confirmDeviceLocation();tick();assertEquals(0,calls);assertTrue(vm.uiState.value.geocoding.available)
        vm.lookupLocality();tick();assertEquals(1,calls);assertNotNull(vm.uiState.value.geocoding.label)
    }
    @Test fun resultDoesNotChangeProspectDataConsentOrGpsEvidence() {
        val vm=model();vm.text(AdmissionField.RESIDENCE_COMMUNE,"Comuna ingresada");vm.text(AdmissionField.GIVEN_NAMES,"Persona ficticia")
        vm.mode(AcceptanceMode.DIRECT);vm.consent(true);confirm(vm)
        val before=vm.uiState.value;vm.lookupLocality();tick();val after=vm.uiState.value
        assertEquals(before.input,after.input);assertEquals(before.consent,after.consent);assertEquals(before.location,after.location)
        assertEquals("Localidad ficticia, Chile",after.geocoding.label)
    }
    @Test fun duplicateTapsDoNotStartAnotherRequestAndSuccessIsReusedInMemory() {
        val vm=model();confirm(vm);gate=CompletableDeferred();vm.lookupLocality();tick();vm.lookupLocality();tick()
        assertEquals(1,calls);gate!!.complete(Unit);tick();vm.lookupLocality();tick();assertEquals(1,calls)
    }
    @Test fun failureAllowsSavingWithoutLabelOrAdditionalRequest() {
        val vm=model();confirm(vm);result=GeocodingResult.Failed(GeocodingFailure.NETWORK)
        vm.lookupLocality();tick();assertNotNull(vm.uiState.value.geocoding.message);assertFalse(vm.uiState.value.geocoding.busy)
        vm.mode(AcceptanceMode.DIRECT);vm.consent(true);vm.save(SaveIntent.SAVE_DRAFT);tick()
        assertTrue(vm.uiState.value.isSaved);assertEquals(position,repo.saved?.location);assertEquals(1,calls)
    }
    @Test fun eventCoordinatesAndOldPositionCannotTriggerExternalApi() {
        val vm=model();vm.confirmEventLocation();vm.lookupLocality();tick();assertEquals(0,calls)
        confirm(vm);now=now.plusSeconds(61);vm.lookupLocality();tick()
        assertEquals(0,calls);assertFalse(vm.uiState.value.geocoding.available)
    }
    @Test fun clearingOrReplacingLocationDiscardsPendingResult() {
        val vm=model();confirm(vm);gate=CompletableDeferred();vm.lookupLocality();tick()
        vm.clearLocation();gate!!.complete(Unit);tick()
        assertNull(vm.uiState.value.geocoding.label);assertFalse(vm.uiState.value.geocoding.busy)
        confirm(vm);vm.lookupLocality();tick();assertNotNull(vm.uiState.value.geocoding.label)
        vm.requestLocation();tick();assertNull(vm.uiState.value.geocoding.label);assertFalse(vm.uiState.value.geocoding.available)
    }
    @Test fun savedOrDiscardedFormCannotBeRepopulatedByLateResult() {
        val vm=model();confirm(vm);gate=CompletableDeferred();vm.lookupLocality();tick()
        vm.mode(AcceptanceMode.DIRECT);vm.consent(true);vm.save(SaveIntent.SAVE_DRAFT);tick()
        gate!!.complete(Unit);tick();assertTrue(vm.uiState.value.isSaved);assertNull(vm.uiState.value.geocoding.label)
        val another=model();confirm(another);gate=CompletableDeferred();another.lookupLocality();tick();another.discard()
        gate!!.complete(Unit);tick();assertNull(another.uiState.value.geocoding.label);assertNull(another.uiState.value.location)
    }
    @Test fun processRecreationDoesNotRestoreLocalityOrPermitHistoricalLookup() {
        val vm=model();confirm(vm);vm.lookupLocality();tick();assertNotNull(vm.uiState.value.geocoding.label)
        store.clear();tick();val fresh=model();fresh.lookupLocality();tick()
        assertNull(fresh.uiState.value.geocoding.label);assertEquals(1,calls)
    }
    @Test fun positionThatExpiresDuringLookupDoesNotDisplayStaleLabel() {
        val vm=model();confirm(vm);gate=CompletableDeferred();vm.lookupLocality();tick()
        now=now.plusSeconds(61);gate!!.complete(Unit);tick()
        assertNull(vm.uiState.value.geocoding.label);assertFalse(vm.uiState.value.geocoding.available)
    }
    @Test fun streetIsSeparateExplicitAndCannotReplaceProspectsCommuneOrLocality() {
        val vm=model();vm.lookupAddress();tick();assertEquals(0,addressCalls)
        vm.text(AdmissionField.RESIDENCE_COMMUNE,"Comuna del prospecto");confirm(vm)
        assertEquals(0,addressCalls);vm.lookupLocality();tick();val before=vm.uiState.value
        vm.lookupAddress();tick();assertEquals(1,addressCalls)
        assertEquals("Calle ficticia 123",vm.uiState.value.addressGeocoding.label)
        assertEquals(before.input,vm.uiState.value.input);assertEquals(before.location,vm.uiState.value.location)
        assertEquals(before.geocoding,vm.uiState.value.geocoding)
    }
    @Test fun streetLookupIsNotParallelAndIsCancelledWithGpsRemoval() {
        val vm=model();confirm(vm);gate=CompletableDeferred();vm.lookupAddress();tick()
        vm.lookupLocality();vm.lookupAddress();tick();assertEquals(0,calls);assertEquals(1,addressCalls)
        vm.clearLocation();gate!!.complete(Unit);tick()
        assertNull(vm.uiState.value.addressGeocoding.label);assertFalse(vm.uiState.value.addressGeocoding.busy)
    }
    @Test fun streetLookupCannotUseEventPositionOrRecreatedForm() {
        val vm=model();vm.confirmEventLocation();vm.lookupAddress();tick();assertEquals(0,addressCalls)
        confirm(vm);vm.lookupAddress();tick();assertNotNull(vm.uiState.value.addressGeocoding.label)
        store.clear();tick();val another=model();another.lookupAddress();tick()
        assertEquals(1,addressCalls);assertNull(another.uiState.value.addressGeocoding.label)
    }
}
