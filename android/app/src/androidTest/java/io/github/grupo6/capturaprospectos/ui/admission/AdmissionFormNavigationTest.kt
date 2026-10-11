package io.github.grupo6.capturaprospectos.ui.admission

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelStore
import io.github.grupo6.capturaprospectos.domain.admission.*
import io.github.grupo6.capturaprospectos.domain.capture.*
import io.github.grupo6.capturaprospectos.ui.theme.CapturaProspectosG6Theme
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import org.junit.*
import org.junit.Assert.*

/** Datos ficticios y doble demorado: no abre ni modifica la base instalada del usuario. */
class AdmissionFormNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val store = ViewModelStore()
    private var exits = 0
    private lateinit var vm: AdmissionFormViewModel
    @After fun cleanup() { compose.runOnIdle { store.clear() } }

    private fun start(repo: AdmissionRepository? = null) {
        val actor = LocalActor(UUID(0, 1), LocalRole.CAPTOR, setOf(UUID(0, 2)))
        val event = CaptureEvent(UUID(0, 3), UUID(0, 2), "Evento ficticio", Instant.EPOCH,
            Instant.EPOCH.plusSeconds(3600), ZoneOffset.UTC, setOf(actor.id), null)
        val env = AdmissionEnvironment(CaptureEnvironment(actor, event, UUID(0, 4),
            emptyList(), "v1", "Consentimiento ficticio"), emptyList(), emptyList())
        compose.runOnIdle { vm = AdmissionFormViewModel(env, repository = repo); store.put("form", vm) }
        compose.setContent { CapturaProspectosG6Theme { AdmissionFormRoute(vm, onNavigateBack = { exits++ }) } }
    }

    @Test fun systemBackAsksBeforeClearingVolatileData() {
        start()
        compose.onNodeWithText("Nombres").performTextInput("Persona ficticia")
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("¿Descartar formulario?").assertIsDisplayed()
        compose.onNodeWithText("Seguir editando").performClick()
        compose.runOnIdle { assertEquals(0, exits); assertEquals("Persona ficticia", vm.uiState.value.input.givenNames) }
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("Descartar datos").performClick()
        compose.runOnIdle { assertEquals(1, exits); assertEquals(AdmissionFormState(), vm.uiState.value) }
    }

    @Test fun systemBackAndDiscardWaitForStorageResult() {
        val result = CompletableDeferred<CreateAdmissionResult>()
        start(object : AdmissionRepository {
            override suspend fun create(request: NewAdmission) = result.await()
            override suspend fun get(id: UUID) = ReadAdmissionResult.Unavailable
        })
        compose.runOnIdle {
            vm.mode(AcceptanceMode.DIRECT); vm.consent(true); vm.save(SaveIntent.SAVE_DRAFT)
        }
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Descartar formulario"))
        compose.onNodeWithText("Descartar formulario").assertIsNotEnabled()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("¿Descartar formulario?").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, exits); result.complete(CreateAdmissionResult.StorageUnavailable) }
        compose.waitUntil(5000) { !vm.uiState.value.isSaving }
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("¿Descartar formulario?").assertIsDisplayed()
    }
}
