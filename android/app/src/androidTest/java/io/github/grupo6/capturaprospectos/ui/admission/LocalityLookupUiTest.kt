package io.github.grupo6.capturaprospectos.ui.admission

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelStore
import io.github.grupo6.capturaprospectos.domain.admission.*
import io.github.grupo6.capturaprospectos.domain.capture.*
import io.github.grupo6.capturaprospectos.domain.device.*
import io.github.grupo6.capturaprospectos.ui.theme.CapturaProspectosG6Theme
import java.time.*
import java.util.UUID
import org.junit.*
import org.junit.Assert.*

class LocalityLookupUiTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private val store=ViewModelStore()
    private val button="Consultar localidad con BigDataCloud"
    private fun card(state:GeocodingState,onLookup:()->Unit={}) {
        compose.setContent { CapturaProspectosG6Theme { Surface(Modifier.fillMaxSize()) {
            Column(Modifier.verticalScroll(rememberScrollState())) { LocalityLookupCard(state,true,onLookup) }
        } } }
    }
    @After fun cleanup() {compose.runOnIdle {store.clear()}}
    @Test fun disclosurePrecedesExplicitRequestAndMentionsProviderAndIp() {
        var calls=0;card(GeocodingState(available=true)) {calls++}
        compose.runOnIdle {assertEquals(0,calls)}
        compose.onNodeWithText("Consulta opcional: se enviarán las coordenadas actuales",substring=true).assertIsDisplayed()
        compose.onNodeWithText("Privacidad de BigDataCloud").assertIsDisplayed()
        compose.onNodeWithText(button).performClick();compose.runOnIdle {assertEquals(1,calls)}
    }
    @Test fun busyStateDisablesDuplicateRequests() {
        card(GeocodingState(available=true,busy=true))
        compose.onNodeWithText("Consultando localidad…").assertIsNotEnabled()
    }
    @Test fun resultHasAttributionAndDoesNotOfferAnotherAutomaticLookup() {
        card(GeocodingState(available=true,label="Localidad ficticia, Chile"))
        compose.onNodeWithText("Localidad aproximada: Localidad ficticia, Chile").assertIsDisplayed()
        compose.onNodeWithText("Fuente: BigDataCloud. No corresponde al domicilio del prospecto.").assertIsDisplayed()
        compose.onNodeWithText(button).assertDoesNotExist()
    }
    @Test fun networkFailureAllowsExplicitRetry() {
        var calls=0;card(GeocodingState(available=true,message="Puedes continuar sin consultar la localidad.")) {calls++}
        compose.onNodeWithText("Puedes continuar sin consultar la localidad.").assertIsDisplayed()
        compose.onNodeWithText(button).assertIsEnabled().performClick();compose.runOnIdle {assertEquals(1,calls)}
    }
    @Test fun streetLookupIsExplicitAndExplainsPossibleNetworkUse() {
        var calls=0
        compose.setContent { CapturaProspectosG6Theme { Surface(Modifier.fillMaxSize()) {
            Column(Modifier.verticalScroll(rememberScrollState())) {StreetAddressLookupCard(GeocodingState(available=true),true) {calls++}}
        } } }
        compose.runOnIdle {assertEquals(0,calls)}
        compose.onNodeWithText("Puede usar Internet y enviar estas coordenadas",substring=true).assertIsDisplayed()
        compose.onNodeWithText("Consultar dirección aproximada").performClick()
        compose.runOnIdle {assertEquals(1,calls)}
    }
    @Test fun streetResultNeverClaimsExactAddressOrProspectsResidence() {
        compose.setContent { CapturaProspectosG6Theme { Surface(Modifier.fillMaxSize()) {
            Column {StreetAddressLookupCard(GeocodingState(available=true,label="Calle ficticia 123"),true) {}}
        } } }
        compose.onNodeWithText("Dirección aproximada: Calle ficticia 123").assertIsDisplayed()
        compose.onNodeWithText("No es el domicilio del prospecto.",substring=true).assertIsDisplayed()
        compose.onNodeWithText("Consultar dirección aproximada").assertDoesNotExist()
    }
    @Test fun actualFormRequiresFreshGpsConfirmationAndPreservesEnteredCommune() {
        val now=Instant.parse("2026-11-22T16:00:00Z")
        val actor=LocalActor(UUID(0,1),LocalRole.CAPTOR,setOf(UUID(0,2)))
        val event=CaptureEvent(UUID(0,3),UUID(0,2),"Evento ficticio",now,now.plusSeconds(3600),ZoneOffset.UTC,setOf(actor.id),null)
        val environment=AdmissionEnvironment(CaptureEnvironment(actor,event,UUID(0,4),emptyList(),"v1","Texto ficticio"),emptyList(),emptyList())
        var calls=0
        var addressCalls=0
        lateinit var vm:AdmissionFormViewModel
        compose.runOnIdle {
            vm=AdmissionFormViewModel(environment,clock=Clock.fixed(now,ZoneOffset.UTC),
                locationProvider=LocationProvider {LocationResult.Candidate(CaptureLocation.Device(Coordinates(-33.0,-71.0),now,10.0))},
                reverseGeocoder=ReverseGeocoder {calls++;GeocodingResult.Found("Localidad ficticia, Chile")},
                addressGeocoder=ReverseGeocoder {addressCalls++;GeocodingResult.Found("Calle ficticia 123")})
            store.put("form",vm);vm.text(AdmissionField.RESIDENCE_COMMUNE,"Comuna del prospecto")
        }
        compose.setContent { CapturaProspectosG6Theme {AdmissionFormRoute(vm)} }
        compose.onNodeWithText(button).assertDoesNotExist()
        compose.runOnIdle {vm.requestLocation()}
        compose.waitUntil(5000) {vm.uiState.value.locationCandidate!=null}
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Confirmar GPS candidato"))
        compose.onNodeWithText("Confirmar GPS candidato").performClick()
        compose.runOnIdle {assertEquals(0,calls)}
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(button))
        compose.onNodeWithText(button).performClick()
        compose.waitUntil(5000) {vm.uiState.value.geocoding.label!=null}
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Localidad aproximada: Localidad ficticia, Chile"))
        compose.onNodeWithText("Localidad aproximada: Localidad ficticia, Chile").assertIsDisplayed()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Consultar dirección aproximada"))
        compose.runOnIdle {assertEquals(0,addressCalls)}
        compose.onNodeWithText("Consultar dirección aproximada").performClick()
        compose.waitUntil(5000) {vm.uiState.value.addressGeocoding.label!=null}
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Dirección aproximada: Calle ficticia 123"))
        compose.onNodeWithText("Dirección aproximada: Calle ficticia 123").assertIsDisplayed()
        compose.runOnIdle {assertEquals(1,addressCalls)}
        compose.runOnIdle {assertEquals("Comuna del prospecto",vm.uiState.value.input.residenceCommune);assertEquals(1,calls)}
    }
}
