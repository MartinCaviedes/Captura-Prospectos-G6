package io.github.grupo6.capturaprospectos.ui.records

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import io.github.grupo6.capturaprospectos.data.local.*
import io.github.grupo6.capturaprospectos.domain.admission.*
import io.github.grupo6.capturaprospectos.domain.capture.*
import io.github.grupo6.capturaprospectos.ui.theme.CapturaProspectosG6Theme
import java.io.File
import java.security.KeyStore
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*

/** Recorrido UI con repositorios reales y base cifrada test-detail-*; nunca usa la base de la app. */
class RecordDetailRoomUiTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private val name="test-detail-${UUID.randomUUID()}.db"
    private var handle:EncryptedDatabase?=null
    private val store=ViewModelStore()
    private lateinit var vm:RecordTrayViewModel
    private lateinit var admissions:RoomAdmissionRepository
    private lateinit var original:StoredAdmission
    private fun id(key:String)=UUID.fromString(DemoSeed.id(key))
    private fun start(draft:Boolean=false) {
        runBlocking(Dispatchers.IO) {
            handle=EncryptedDatabase.open(context,name)
            val db=handle!!.database; val ctx=RoomDemoContextRepository(db)
            ctx.initialize(); ctx.selectProfile(id("captor-a")); ctx.selectEvent(id("event-a"))
            db.openHelper.writableDatabase.execSQL("UPDATE demo_events SET latitude=-33.0, longitude=-71.0 WHERE id=?",arrayOf(DemoSeed.id("event-a")))
            val env=ctx.environment(id("event-a"))!!
            admissions=RoomAdmissionRepository(db,ctx)
            val input=if (draft) AdmissionInput() else AdmissionInput(givenNames="Persona Ficticia",familyNames="Demo",
                rut="12345678-5",email="demo@example.invalid",phone="+56900000000",birthDate="2000-01-01",
                residenceCommune="Comuna ficticia",currentActivity=CurrentActivity.SECONDARY_GRADUATE,notes="Nota ficticia",
                interests=listOf(RankedInterest(id("program-1"),1)))
            val saved=admissions.create(NewAdmission(env.context.event.id,input,
                if (draft) SaveIntent.SAVE_DRAFT else SaveIntent.CONFIRM_REGISTRATION,
                ConsentEvidence(DemoSeed.CONSENT,Instant.now(),env.context.actor.id,AcceptanceMode.DIRECT),null)) as CreateAdmissionResult.Saved
            original=(admissions.get(saved.id) as ReadAdmissionResult.Found).capture
            compose.runOnIdle { vm=RecordTrayViewModel(RoomRecordRepository(db,ctx)); store.put("tray",vm) }
        }
        compose.setContent { CapturaProspectosG6Theme { RecordTrayRoute(vm,{}) } }
        compose.waitUntil(10000) { vm.uiState.value.items.isNotEmpty() }
        compose.onNodeWithText("Ver ficha completa").performClick()
        compose.waitUntil(10000) { vm.uiState.value.selectedRecord != null }
        compose.onNodeWithText("Editar ficha").performClick()
        compose.waitUntil(10000) { vm.uiState.value.editForm != null }
    }
    private fun scroll(text:String) {
        compose.onAllNodes(hasScrollAction()).onLast().performScrollToNode(hasText(text))
    }
    private fun write(label:String,value:String) { scroll(label); compose.onNode(hasText(label) and hasSetTextAction()).performTextReplacement(value) }
    private fun stored()=runBlocking(Dispatchers.IO) { (admissions.get(original.id) as ReadAdmissionResult.Found).capture }
    private fun save() { scroll("Guardar cambios y confirmar inscripción"); compose.onNodeWithText("Guardar cambios y confirmar inscripción").performClick() }
    @After fun cleanup() {
        compose.runOnIdle { store.clear() }
        handle?.close(); check(name.startsWith("test-detail-")); context.deleteDatabase(name)
        listOf("",".bak",".new").forEach { File(context.noBackupFilesDir,"$name.wrapped-key$it").delete() }
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry("g6.database.$name") }
    }
    @Test fun draftCompletesAndNeedsContextCanConfirmEventInSameEncryptedRecord() {
        start(draft=true)
        write("Nombres","Persona Ficticia"); write("Apellidos","Demo"); write("RUT","12345678-5")
        write("Correo electrónico","demo@example.invalid"); write("Teléfono","900000000"); write("Nacimiento (aaaa-mm-dd)","2000-01-01")
        write("Comuna de residencia","Comuna ficticia")
        scroll("Egresado Enseñanza Media"); compose.onNodeWithText("Egresado Enseñanza Media").performClick()
        scroll("Programa ficticio 1 — DEMO"); compose.onNodeWithText("Programa ficticio 1 — DEMO").performClick()
        save(); compose.waitUntil(10000) { !vm.uiState.value.isEditing && !vm.uiState.value.isReadingDetail }
        val pending=stored(); assertEquals(CaptureState.NEEDS_CONTEXT,pending.state); assertEquals(original.id,pending.id)
        assertEquals(original.consent,pending.consent); assertEquals(original.capturedAt,pending.capturedAt)
        compose.onNodeWithText("Editar ficha").performClick(); compose.waitUntil(10000) { vm.uiState.value.editForm != null }
        scroll("RUT"); compose.onNode(hasText("RUT") and hasSetTextAction()).assertIsNotEnabled()
        scroll("Confirmar evento"); compose.onNodeWithText("Confirmar evento").performClick()
        save(); compose.waitUntil(10000) { !vm.uiState.value.isEditing && !vm.uiState.value.isReadingDetail }
        val ready=stored(); assertEquals(CaptureState.READY,ready.state); assertEquals(3L,ready.revision)
        assertTrue(ready.location is CaptureLocation.EventConfirmed)
        assertEquals(original.consent,ready.consent)
    }
    @Test fun invalidEmailShowsFeedbackAndRetryPersistsClearedOptionalNotes() {
        start(); write("Correo electrónico","bad"); save()
        compose.waitUntil(10000) { !vm.uiState.value.isMutating && vm.uiState.value.editForm!!.uiState.value.issues.isNotEmpty() }
        assertEquals(1L,stored().revision)
        compose.onNode(hasText("Correo electrónico") and hasSetTextAction()).assertIsFocused()
        compose.onNodeWithText("Revisa los campos indicados; los cambios se conservan.").assertExists()
        write("Correo electrónico","corrected@example.invalid")
        write("Notas (opcional, máximo 500; no amplían la motivación)","")
        save(); compose.waitUntil(10000) { !vm.uiState.value.isEditing && !vm.uiState.value.isReadingDetail }
        val changed=stored(); assertEquals("corrected@example.invalid",changed.input.email); assertNull(changed.input.notes)
        assertEquals(original.consent,changed.consent); assertEquals(2L,changed.revision)
    }
    @Test fun conflictKeepsInputAndRequiresExplicitDiscardBeforeReload() {
        start(); write("Correo electrónico","changed@example.invalid")
        runBlocking(Dispatchers.IO) { handle!!.database.openHelper.writableDatabase.execSQL("UPDATE captures SET revision=revision+1 WHERE id=?",arrayOf(original.id.toString())) }
        save(); compose.waitUntil(10000) { vm.uiState.value.editConflict }
        assertEquals("changed@example.invalid",vm.uiState.value.editForm!!.uiState.value.input.email)
        compose.onNodeWithText("Recargar ficha y descartar cambios…").performClick()
        compose.onNodeWithText("Conservar cambios").performClick()
        assertTrue(vm.uiState.value.editConflict)
        compose.onNodeWithText("Recargar ficha y descartar cambios…").performClick()
        compose.onNodeWithText("Descartar cambios").performClick()
        compose.waitUntil(10000) { !vm.uiState.value.isEditing && !vm.uiState.value.isReadingDetail }
        assertEquals(2L,vm.uiState.value.selectedRecord!!.revision)
        assertEquals("demo@example.invalid",stored().input.email)
    }
}
