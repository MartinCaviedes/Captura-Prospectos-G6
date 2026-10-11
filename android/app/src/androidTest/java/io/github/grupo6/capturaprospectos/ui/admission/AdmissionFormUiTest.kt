package io.github.grupo6.capturaprospectos.ui.admission

import androidx.compose.ui.test.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
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
    @Test fun volatileFieldsActivityConfirmationAndConsentGate() = verifyForm(false, 1f)
    @Test fun largeTextKeepsLightFormActionsUsableInsideDarkShell() = verifyForm(true, 1.5f)
    private fun scrollTo(label: String): SemanticsNodeInteraction {
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(label))
        return compose.onNodeWithText(label).performScrollTo()
    }
    private fun verifyForm(darkTheme: Boolean, fontScale: Float) {
        val actor = LocalActor(UUID(0, 1), LocalRole.CAPTOR, setOf(UUID(0, 2)))
        val event = CaptureEvent(UUID(0, 3), UUID(0, 2), "Evento ficticio", Instant.EPOCH, Instant.EPOCH.plusSeconds(3600), ZoneOffset.UTC, setOf(actor.id), null)
        val environment = AdmissionEnvironment(CaptureEnvironment(actor, event, UUID(0, 4), emptyList(), "test", "Consentimiento ficticio"), emptyList(), emptyList())
        lateinit var vm: AdmissionFormViewModel
        compose.runOnIdle { vm = AdmissionFormViewModel(environment); store.put("form", vm) }
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                CapturaProspectosG6Theme(darkTheme = darkTheme) { AdmissionFormRoute(vm) }
            }
        }
        scrollTo("Nombres").performTextInput("Persona ficticia")
        scrollTo("Teléfono").performTextInput("912345678")
        compose.onNodeWithText("+56").assertIsDisplayed()
        compose.runOnIdle { assertEquals("+56912345678", vm.uiState.value.input.phone) }
        compose.onNodeWithText("Teléfono").performTextReplacement("+56987654321")
        compose.runOnIdle { assertEquals("+56987654321", vm.uiState.value.input.phone) }
        compose.onNodeWithText("Teléfono").performTextClearance()
        compose.runOnIdle { assertEquals("", vm.uiState.value.input.phone) }
        scrollTo("Nacimiento (aaaa-mm-dd)").performTextInput("2000-01-01")
        compose.onNodeWithContentDescription("Elegir fecha de nacimiento").performClick()
        compose.onNodeWithText("Cancelar fecha").performClick()
        compose.runOnIdle { assertEquals("2000-01-01", vm.uiState.value.input.birthDate) }
        compose.onNodeWithContentDescription("Elegir fecha de nacimiento").performClick()
        compose.onNodeWithText("Usar fecha").performClick()
        compose.runOnIdle { assertEquals("2000-01-01", vm.uiState.value.input.birthDate) }
        scrollTo("Otra actividad").performClick()
        scrollTo("Detalle de otra actividad").performTextInput("Detalle ficticio")
        scrollTo("4° medio").performClick()
        compose.onNodeWithText("Conservar").performClick()
        compose.runOnIdle { assertEquals(CurrentActivity.OTHER, vm.uiState.value.input.currentActivity) }
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Validar inscripción"))
        compose.onNodeWithText("Validar inscripción").performClick()
        scrollTo("Registra el consentimiento para validar.").assertIsDisplayed()
        scrollTo("Descartar formulario").performClick()
        compose.onNodeWithText("Descartar datos").performClick()
        compose.runOnIdle { assertEquals(AdmissionFormState(), vm.uiState.value) }
    }
}
