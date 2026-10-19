package io.github.grupo6.capturaprospectos.ui.records

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelStore
import io.github.grupo6.capturaprospectos.domain.capture.CaptureState
import io.github.grupo6.capturaprospectos.domain.records.*
import io.github.grupo6.capturaprospectos.ui.theme.CapturaProspectosG6Theme
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import org.junit.*
import org.junit.Assert.*

/** Doble identificado para navegación; la paginación SQL se prueba en RecordStorageTest. */
class RecordTrayUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val store = ViewModelStore()
    private val mutation = CompletableDeferred<RecordMutationResult>()
    private var exits = 0
    private var pages = 0
    private lateinit var vm: RecordTrayViewModel
    private fun row(n: Long) = RecordSummary(UUID(0, n), 1, Instant.EPOCH, UUID(0, 9),
        "Persona ficticia $n", "12345678-5", CaptureState.NEEDS_CONTEXT, RecordContract.F2027)
    private fun start() {
        compose.runOnIdle {
            vm = RecordTrayViewModel(object : RecordRepository {
                override suspend fun get(id: java.util.UUID): io.github.grupo6.capturaprospectos.domain.admission.ReadAdmissionResult = io.github.grupo6.capturaprospectos.domain.admission.ReadAdmissionResult.Unavailable
            override suspend fun list(query: RecordQuery): RecordPageResult {
                    pages++
                    return if (query.after == null) RecordPageResult.Page(listOf(row(1)), RecordCursor(Instant.EPOCH, UUID(0, 1)))
                    else RecordPageResult.Page(listOf(row(2)), null)
                }
                override suspend fun edit(command: EditAdmission) = RecordMutationResult.Unavailable
                override suspend fun cancel(command: CancelRecord) = mutation.await()
            })
            store.put("tray", vm)
        }
        compose.setContent { CapturaProspectosG6Theme { RecordTrayRoute(vm, { exits++ }) } }
        compose.waitUntil(5000) { !vm.uiState.value.isLoading }
    }
    @After fun cleanup() { compose.runOnIdle { store.clear() } }

    @Test fun loadMoreDisplaysNextPageAndRemovesExhaustedCursor() {
        start()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Cargar más registros"))
        compose.onNodeWithText("Cargar más registros").performClick()
        compose.waitUntil(5000) { vm.uiState.value.items.size == 2 }
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Persona ficticia 2"))
        compose.onNodeWithText("Persona ficticia 2").assertIsDisplayed()
        compose.onNodeWithText("Cargar más registros").assertDoesNotExist()
        compose.runOnIdle { assertEquals(2, pages); assertNull(vm.uiState.value.nextCursor) }
    }

    @Test fun pendingCancellationBlocksBackAndKeepsReasonOnStorageFailure() {
        start()
        compose.runOnIdle { vm.openCancelDialog(row(1)) }
        val reason = "Motivo ficticio conservado"
        compose.onNodeWithText("Motivo de la anulación (5 a 300 caracteres)").performTextInput(reason)
        compose.onNodeWithText("Confirmar anulación").performClick()
        compose.onNodeWithText("Confirmar anulación").assertIsNotEnabled()
        compose.onNodeWithText("Cancelar").assertIsNotEnabled()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed(); assertEquals(0, exits) }
        compose.runOnIdle { mutation.complete(RecordMutationResult.StorageUnavailable) }
        compose.waitUntil(5000) { !vm.uiState.value.isMutating }
        compose.onNodeWithText(reason).assertIsDisplayed()
        compose.onNodeWithText("Confirmar anulación").assertIsEnabled()
        compose.runOnIdle { assertNotNull(vm.uiState.value.targetForCancellation); assertEquals(0, exits) }
    }
}
