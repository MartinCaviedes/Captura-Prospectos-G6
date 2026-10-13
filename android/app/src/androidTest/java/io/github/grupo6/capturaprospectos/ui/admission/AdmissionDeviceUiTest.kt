package io.github.grupo6.capturaprospectos.ui.admission

import androidx.activity.ComponentActivity
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

/** Candidatos con doble explícito: no concede permisos ni afirma enfoque/GPS físicos. */
class AdmissionDeviceUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val store = ViewModelStore()
    private lateinit var vm: AdmissionFormViewModel
    private val now = Instant.parse("2026-10-10T16:00:00Z")
    private fun start(configure: (AdmissionFormViewModel) -> Unit = {}) {
        val actor = LocalActor(UUID(0,1), LocalRole.CAPTOR, setOf(UUID(0,2)))
        val event = CaptureEvent(UUID(0,3),UUID(0,2),"Evento ficticio",now.minusSeconds(60),now.plusSeconds(3600),ZoneOffset.UTC,setOf(actor.id),null)
        val env = AdmissionEnvironment(CaptureEnvironment(actor,event,UUID(0,4),emptyList(),"v1","Consentimiento ficticio"),emptyList(),emptyList())
        compose.runOnIdle {
            vm = AdmissionFormViewModel(env, clock = Clock.fixed(now,ZoneOffset.UTC))
            store.put("form",vm); configure(vm)
        }
        compose.setContent { CapturaProspectosG6Theme { AdmissionFormRoute(vm) } }
    }
    @After fun cleanup() { compose.runOnIdle { store.clear() } }

    @Test fun gpsCandidateRequiresVisibleConfirmationAndDoesNotInferOtherFields() {
        start()
        compose.runOnIdle { vm.requestLocation(LocationProvider { LocationResult.Candidate(CaptureLocation.Device(Coordinates(-33.0,-71.0),now,15.0)) }) }
        compose.waitUntil(5000) { vm.uiState.value.locationCandidate != null }
        compose.runOnIdle { assertNull(vm.uiState.value.location) }
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Guardar inscripción"))
        compose.onNodeWithText("Guardar inscripción").assertIsNotEnabled()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Confirmar GPS candidato"))
        compose.onNodeWithText("Confirmar GPS candidato").performClick()
        compose.runOnIdle {
            assertTrue(vm.uiState.value.location is CaptureLocation.Device)
            assertNull(vm.uiState.value.locationCandidate)
            assertEquals("", vm.uiState.value.input.residenceCommune)
            assertNull(vm.uiState.value.input.establishment)
        }
    }

    @Test fun qrReplacementRequiresCheckboxAndPreservesManualNames() {
        start { model ->
            model.text(AdmissionField.RUT,"11111111-1")
            model.text(AdmissionField.GIVEN_NAMES,"Nombres manuales")
            model.text(AdmissionField.FAMILY_NAMES,"Apellidos manuales")
            model.openQrScanner()
            model.processQrPayload("G6DEMO:1:{\"fullName\":\"Persona Ficticia Alfa\",\"rut\":\"12345678-5\"}")
        }
        compose.onNodeWithText("Confirmar RUT candidato").assertIsNotEnabled()
        compose.runOnIdle { assertEquals("11111111-1", vm.uiState.value.input.rut) }
        compose.onNodeWithText("Confirmo reemplazar el RUT escrito por el candidato.").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Confirmo reemplazar el RUT escrito por el candidato.").assertIsOn()
        compose.onNodeWithText("Confirmar RUT candidato").assertIsEnabled()
        compose.onNodeWithText("Confirmar RUT candidato").performClick()
        compose.waitUntil(5000) { !vm.uiState.value.isQrScannerOpen }
        compose.runOnIdle {
            assertEquals("12345678-5", vm.uiState.value.input.rut)
            assertEquals("Nombres manuales", vm.uiState.value.input.givenNames)
            assertEquals("Apellidos manuales", vm.uiState.value.input.familyNames)
            assertFalse(vm.uiState.value.isQrScannerOpen)
        }
    }
}
