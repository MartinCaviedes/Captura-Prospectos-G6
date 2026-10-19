package io.github.grupo6.capturaprospectos.ui.context

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import io.github.grupo6.capturaprospectos.domain.capture.LocalActor
import io.github.grupo6.capturaprospectos.domain.capture.LocalRole
import io.github.grupo6.capturaprospectos.domain.context.*
import io.github.grupo6.capturaprospectos.ui.theme.CapturaProspectosG6Theme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class SupervisorEventLayoutUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val busy = mutableStateOf(false)
    private var creates = 0
    private fun id(key: String) = UUID.nameUUIDFromBytes(key.toByteArray())
    private fun start(widthDp: Float, fontScale: Float, dark: Boolean) {
        val profile = DemoProfile(LocalActor(id("supervisor"), LocalRole.SUPERVISOR, setOf(id("site"))), "Supervisor ficticio")
        val snapshot = DemoContextSnapshot(id("device"), listOf(profile), profile, emptyList(), null, null,
            DemoCatalog("fixture", "SYNTHETIC", "Prueba visual", 2027, emptyList(), emptyList()), "fixture", "Consentimiento ficticio")
        val density = compose.activity.resources.displayMetrics.widthPixels / widthDp
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                CapturaProspectosG6Theme(darkTheme = dark) {
                    DemoContextScreen(DemoContextUiState(context = DemoContextState.Ready(snapshot), busy = busy.value),
                        {}, {}, {}, {}, {}, onCreateEvent = { creates++ }, showProfiles = false)
                }
            }
        }
    }
    private fun labelLayout(): TextLayoutResult {
        val node = compose.onNodeWithText("Nuevo evento", useUnmergedTree = true).fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        assertTrue(node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts))
        return layouts.single()
    }
    private fun assertUsable() {
        val button = compose.onNodeWithText("Nuevo evento")
        button.assertIsDisplayed().assertHasClickAction().assertIsEnabled()
        val layout = labelLayout()
        assertEquals("La etiqueta debe leerse completa en una línea", 1, layout.lineCount)
        // El párrafo conserva el ancho disponible; se comprueban los límites de la línea visible.
        assertFalse("No debe haber elipsis", layout.isLineEllipsized(0))
        assertEquals("Deben conservarse todos los caracteres", layout.layoutInput.text.length, layout.getLineEnd(0, visibleEnd = true))
        assertTrue("La línea debe caber horizontalmente", layout.getLineLeft(0) >= -1f && layout.getLineRight(0) <= layout.size.width + 1f)
        assertTrue("La línea debe caber verticalmente", layout.getLineBottom(0) <= layout.size.height + 1f)
        val heading = compose.onNodeWithText("2. Evento para supervisar").fetchSemanticsNode().boundsInRoot
        val bounds = button.fetchSemanticsNode().boundsInRoot
        assertFalse("Título y acción no deben superponerse", heading.overlaps(bounds))
        button.performClick()
        compose.runOnIdle { assertEquals(1, creates) }
    }
    @Test fun compactWidthKeepsCreateLabelOnOneLine() {
        start(360f, 1f, true); assertUsable()
    }
    @Test fun narrowWidthWithLargeTextKeepsActionReadableInLightTheme() {
        start(320f, 1.5f, false); assertUsable()
    }
    @Test fun wideWidthKeepsTitleAndActionOnTheSameRow() {
        start(800f, 1f, true); assertUsable()
        val heading = compose.onNodeWithText("2. Evento para supervisar").fetchSemanticsNode().boundsInRoot
        val button = compose.onNodeWithText("Nuevo evento").fetchSemanticsNode().boundsInRoot
        assertTrue("La acción debe conservarse a la derecha cuando hay espacio", button.left >= heading.right)
    }
    @Test fun wideWidthWithLargeTextKeepsActionReadable() {
        start(800f, 2f, false); assertUsable()
    }
    @Test fun busyStatePreventsCreatingAnotherEvent() {
        start(360f, 1f, true)
        compose.runOnIdle { busy.value = true }
        compose.onNodeWithText("Nuevo evento").assertIsDisplayed().assertIsNotEnabled()
        compose.runOnIdle { assertEquals(0, creates) }
    }
}
