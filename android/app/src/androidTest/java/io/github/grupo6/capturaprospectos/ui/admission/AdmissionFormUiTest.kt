package io.github.grupo6.capturaprospectos.ui.admission

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import io.github.grupo6.capturaprospectos.domain.admission.*
import io.github.grupo6.capturaprospectos.domain.capture.*
import io.github.grupo6.capturaprospectos.ui.theme.CapturaProspectosG6Theme
import java.time.*
import java.util.UUID
import org.junit.*
import org.junit.Assert.*

class AdmissionFormUiTest {
    @get:Rule val compose = createComposeRule()
    private val store = ViewModelStore()
    @After fun cleanup() { compose.runOnIdle { store.clear() } }
    @Test fun volatileFieldsActivityConfirmationAndConsentGate() {
        val actor = LocalActor(UUID(0, 1), LocalRole.CAPTOR, setOf(UUID(0, 2)))
        val event = CaptureEvent(UUID(0, 3), UUID(0, 2), "Evento ficticio", Instant.EPOCH, Instant.EPOCH.plusSeconds(3600), ZoneOffset.UTC, setOf(actor.id), null)
        val environment = AdmissionEnvironment(CaptureEnvironment(actor, event, UUID(0, 4), emptyList(), "test", "Consentimiento ficticio"), emptyList(), emptyList())
        lateinit var vm: AdmissionFormViewModel
        compose.runOnIdle { vm = AdmissionFormViewModel(environment); store.put("form", vm) }
        compose.setContent { CapturaProspectosG6Theme { AdmissionFormRoute(vm) } }
        compose.onNodeWithText("Nombres").performScrollTo().performTextInput("Persona ficticia")
        compose.onNodeWithText("Otra actividad").performScrollTo().performClick()
        compose.onNodeWithText("Detalle de otra actividad").performScrollTo().performTextInput("Detalle ficticio")
        compose.onNodeWithText("4° medio").performScrollTo().performClick()
        compose.onNodeWithText("Conservar").performClick()
        compose.runOnIdle { assertEquals(CurrentActivity.OTHER, vm.uiState.value.input.currentActivity) }
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Validar inscripción"))
        compose.onNodeWithText("Validar inscripción").performClick()
        compose.onNodeWithText("Registra el consentimiento para validar.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Descartar formulario").performScrollTo().performClick()
        compose.onNodeWithText("Descartar datos").performClick()
        compose.runOnIdle { assertEquals(AdmissionFormState(), vm.uiState.value) }
    }
}
