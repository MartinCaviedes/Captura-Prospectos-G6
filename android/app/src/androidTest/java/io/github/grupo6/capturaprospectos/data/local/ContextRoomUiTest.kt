package io.github.grupo6.capturaprospectos.data.local

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import io.github.grupo6.capturaprospectos.domain.context.*
import io.github.grupo6.capturaprospectos.ui.context.*
import io.github.grupo6.capturaprospectos.ui.theme.CapturaProspectosG6Theme
import java.io.File
import java.security.KeyStore
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class ContextRoomUiTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "test-route-${UUID.randomUUID()}.db"
    private val store = ViewModelStore()
    private var handle: EncryptedDatabase? = null
    @After fun cleanup() {
        compose.runOnIdle { store.clear() }
        handle?.close()
        check(name.startsWith("test-route-")); context.deleteDatabase(name)
        listOf("", ".bak", ".new").forEach { File(context.noBackupFilesDir, "$name.wrapped-key$it").delete() }
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry("g6.database.$name") }
    }
    @Test fun explicitSelectionReopenProfileChangeAndExitUseRealRoom() = runBlocking {
        handle = EncryptedDatabase.open(context, name)
        val repo = RoomDemoContextRepository(handle!!.database)
        lateinit var vm: DemoContextViewModel
        compose.runOnIdle { vm = DemoContextViewModel(repo); store.put("context", vm) }
        compose.setContent { CapturaProspectosG6Theme { DemoContextRoute(vm) } }
        compose.waitUntil(10000) { vm.uiState.value.context == DemoContextState.NotInitialized }
        compose.onNodeWithText("Preparar demo local").performScrollTo().performClick()
        compose.waitUntil(10000) { vm.uiState.value.context is DemoContextState.Ready && !vm.uiState.value.busy }
        fun state() = (vm.uiState.value.context as DemoContextState.Ready).snapshot
        assertNull(state().currentProfile); assertNull(state().selectedEvent)
        val first = state().profiles.first { it.actor.id.toString() == DemoSeed.id("captor-a") }
        val second = state().profiles.first { it.actor.id.toString() == DemoSeed.id("captor-b") }
        compose.onNodeWithText(first.displayName).performScrollTo().performClick()
        compose.waitUntil(10000) { state().currentProfile == first && !vm.uiState.value.busy }
        val event = state().availableEvents.first()
        compose.onNodeWithText(event.name).performScrollTo().performClick()
        compose.waitUntil(10000) { state().selectedEvent == event && !vm.uiState.value.busy }
        // Otra conexión cifrada lee el estado confirmado, sin reset ni seed adicional.
        EncryptedDatabase.open(context, name).use { reopened ->
            assertEquals(event, (RoomDemoContextRepository(reopened.database).snapshot() as DemoContextState.Ready).snapshot.selectedEvent)
            assertEquals(0, reopened.database.captures().count())
        }
        compose.onNodeWithText(second.displayName).performScrollTo().performClick()
        compose.waitUntil(10000) { state().currentProfile == second && !vm.uiState.value.busy }
        assertNull(state().selectedEvent)
        assertFalse(state().availableEvents.any { it.id == event.id })
        compose.onNodeWithText("Salir de la selección").performScrollTo().performClick()
        compose.waitUntil(10000) { state().currentProfile == null && !vm.uiState.value.busy }
        assertNull(state().selectedEvent)
        assertEquals(0, handle!!.database.captures().count())
    }
}
