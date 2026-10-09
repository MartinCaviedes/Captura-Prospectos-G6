package io.github.grupo6.capturaprospectos.ui.context

import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.github.grupo6.capturaprospectos.domain.capture.*
import io.github.grupo6.capturaprospectos.domain.context.*
import io.github.grupo6.capturaprospectos.ui.theme.CapturaProspectosG6Theme
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DemoContextScreenTest {
    @get:Rule val compose = createComposeRule()
    private val profile = DemoProfile(LocalActor(UUID(0, 1), LocalRole.CAPTOR, setOf(UUID(0, 2))), "Captador ficticio")
    private val event = CaptureEvent(UUID(0, 3), UUID(0, 2), "Evento ficticio", Instant.EPOCH,
        Instant.EPOCH.plusSeconds(3600), ZoneId.of("UTC"), setOf(profile.actor.id), null)
    private fun ready(supervisor: Boolean = false, selected: Boolean = false): DemoContextState.Ready {
        val current = if (supervisor) profile.copy(actor = profile.actor.copy(role = LocalRole.SUPERVISOR)) else profile
        return DemoContextState.Ready(DemoContextSnapshot(UUID(0, 4), listOf(current), current,
            if (supervisor) emptyList() else listOf(event), if (selected) event else null, null,
            DemoCatalog("test", "SYNTHETIC", "test", 2027, emptyList(), emptyList()), "test", "Texto ficticio"))
    }
    private fun render(state: DemoContextUiState, initialize: () -> Unit = {}, retry: () -> Unit = {},
        selectProfile: (UUID) -> Unit = {}, selectEvent: (UUID) -> Unit = {}, clear: () -> Unit = {}) {
        compose.setContent {
            CapturaProspectosG6Theme { Surface {
                DemoContextScreen(state, initialize, retry, selectProfile, selectEvent, clear)
            } }
        }
    }
    @Test fun initializationRequiresExplicitAction() {
        var calls = 0
        render(DemoContextUiState(DemoContextState.NotInitialized), initialize = { calls++ })
        compose.runOnIdle { assertEquals(0, calls) }
        compose.onNodeWithText("Preparar demo local").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, calls) }
    }
    @Test fun unavailableOffersReadRetryNotInitialization() {
        var calls = 0
        render(DemoContextUiState(DemoContextState.StorageUnavailable), retry = { calls++ })
        compose.onNodeWithText("Preparar demo local").assertDoesNotExist()
        compose.onNodeWithText("Reintentar lectura").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, calls) }
    }
    @Test fun selectionEmitsIdWithoutClaimingPersistence() {
        var selected: UUID? = null
        render(DemoContextUiState(ready()), selectEvent = { selected = it })
        compose.onNodeWithText("Evento ficticio").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(event.id, selected) }
        compose.onNodeWithText("Evento ficticio").assertIsNotSelected()
    }
    @Test fun busyDisablesMutations() {
        render(DemoContextUiState(ready(), busy = true))
        compose.onNodeWithText("Captador ficticio · Seleccionado").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Evento ficticio").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Salir de la selección").performScrollTo().assertIsNotEnabled()
    }
    @Test fun supervisorHasNoCaptureEventAction() {
        render(DemoContextUiState(ready(supervisor = true)))
        compose.onNodeWithText("Evento ficticio").assertDoesNotExist()
        compose.onNodeWithText("Perfil supervisor:", substring = true).performScrollTo().assertIsDisplayed()
    }
    @Test fun clearSelectionIsAnExplicitCallback() {
        var calls = 0
        render(DemoContextUiState(ready(selected = true)), clear = { calls++ })
        compose.onNodeWithText("Salir de la selección").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, calls) }
    }
}
