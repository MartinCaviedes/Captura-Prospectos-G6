package io.github.grupo6.capturaprospectos.ui.access

import androidx.activity.ComponentActivity
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import io.github.grupo6.capturaprospectos.AppScreen
import io.github.grupo6.capturaprospectos.ui.shell.AppNavigation
import io.github.grupo6.capturaprospectos.ui.theme.CapturaProspectosG6Theme
import org.junit.*
import org.junit.Assert.*

class DemoVisualUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun credentialsAreRequiredAndExternalProvidersStayDisabledWithLargeText() {
        var submitted: Pair<String,String>? = null
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,1.5f)) {
                CapturaProspectosG6Theme(darkTheme = true) { DemoAccessScreen(DemoAccessState(welcome=false),true,false,
                    { email,password -> submitted = email to password }) }
            }
        }
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Ingresar a la demo"))
        compose.onNodeWithText("Ingresar a la demo").performClick()
        compose.runOnIdle { assertEquals("" to "", submitted) }
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Correo demo"))
        compose.onNode(hasText("Correo demo") and hasSetTextAction()).performTextInput("captador.a@g6.example")
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Contraseña demo"))
        compose.onNode(hasText("Contraseña demo") and hasSetTextAction()).performTextInput("DemoG6!2026")
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Ingresar a la demo"))
        compose.onNodeWithText("Ingresar a la demo").performClick()
        compose.runOnIdle { assertEquals("captador.a@g6.example" to "DemoG6!2026",submitted) }
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Google · Próximamente"))
        compose.onNodeWithText("Google · Próximamente").assertIsNotEnabled()
        compose.onNodeWithText("Microsoft · Próximamente").performScrollTo().assertIsNotEnabled()
    }
    @Test fun phoneNavigationChangesDestinationsAndDisablesDuringOperation() = navigation(false)
    @Test fun expandedLayoutUsesSideMenuWithSimulatedWindowWidthAndLargeText() = navigation(true)
    private fun navigation(expanded: Boolean) {
        val selected = mutableStateOf(AppScreen.HUB)
        val enabled = mutableStateOf(true)
        compose.setContent {
            // Density is a layout harness, not evidence of a physical Android tablet.
            CompositionLocalProvider(LocalDensity provides if(expanded) Density(.75f,1.5f) else LocalDensity.current) {
                CapturaProspectosG6Theme { AppNavigation(selected.value,enabled.value,{selected.value=it}) {
                    Surface(it) { Text("Vista ${selected.value}") }
                } }
            }
        }
        if(expanded) compose.onNodeWithText("CAPTURA G6").assertIsDisplayed()
        else compose.onNodeWithText("CAPTURA G6").assertDoesNotExist()
        compose.onNodeWithText("Perfil").performClick()
        compose.onNodeWithText("Vista PROFILE").assertIsDisplayed()
        compose.runOnIdle { enabled.value = false }
        compose.onNodeWithText("Resumen").performClick()
        compose.runOnIdle { assertEquals(AppScreen.PROFILE,selected.value) }
    }
}
