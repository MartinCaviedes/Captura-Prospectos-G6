package io.github.grupo6.capturaprospectos.ui.context

import io.github.grupo6.capturaprospectos.ui.hub.*

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import io.github.grupo6.capturaprospectos.data.local.*
import io.github.grupo6.capturaprospectos.domain.admission.*
import io.github.grupo6.capturaprospectos.domain.capture.*
import io.github.grupo6.capturaprospectos.domain.context.*
import io.github.grupo6.capturaprospectos.domain.device.*
import io.github.grupo6.capturaprospectos.domain.records.*
import io.github.grupo6.capturaprospectos.ui.theme.CapturaProspectosG6Theme
import java.io.File
import java.security.KeyStore
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import org.junit.*
import org.junit.Assert.*

class EventAdminRoomUiTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private val name="test-event-ui-${UUID.randomUUID()}.db"
    private var handle:EncryptedDatabase?=null
    private val store=ViewModelStore()
    private lateinit var ctx:RoomDemoContextRepository
    private lateinit var repo:RoomRecordRepository
    private lateinit var vm:MainHubMetricsViewModel
    private lateinit var ctxVm:DemoContextViewModel
    private lateinit var saved:UUID
    private fun id(key:String)=UUID.fromString(DemoSeed.id(key))
    private fun start(supervisor:Boolean=false) {
        val snapshot=runBlocking(Dispatchers.IO) {
            handle=EncryptedDatabase.open(context,name);ctx=RoomDemoContextRepository(handle!!.database)
            ctx.initialize();ctx.selectProfile(id("captor-a"));ctx.selectEvent(id("event-a"))
            val env=ctx.environment(id("event-a"))!!
            val input=AdmissionInput(givenNames="Persona Ficticia",familyNames="Demo",rut="12345678-5",
                email="demo@example.invalid",phone="+56900000000",birthDate="2000-01-01",residenceCommune="Comuna ficticia",
                currentActivity=CurrentActivity.SECONDARY_GRADUATE,interests=listOf(RankedInterest(id("program-1"),1)))
            saved=(RoomAdmissionRepository(handle!!.database,ctx).create(NewAdmission(id("event-a"),input,SaveIntent.CONFIRM_REGISTRATION,
                ConsentEvidence(DemoSeed.CONSENT,Instant.now(),env.context.actor.id,AcceptanceMode.DIRECT),null)) as CreateAdmissionResult.Saved).id
            if(supervisor) { ctx.selectProfile(id("supervisor"));ctx.selectEvent(id("event-a")) }
            repo=RoomRecordRepository(handle!!.database,ctx)
            (ctx.snapshot() as DemoContextState.Ready).snapshot
        }
        compose.runOnIdle { vm=MainHubMetricsViewModel(repo,snapshot.selectedEvent!!);store.put("metrics",vm);ctxVm=DemoContextViewModel(ctx);store.put("context",ctxVm) }
        val offline=object:ConnectivityObserver { override fun observe()=flowOf(ConnectivityStatus.OFFLINE) }
        compose.setContent { CapturaProspectosG6Theme { MainHubRoute(snapshot.currentProfile!!,snapshot.selectedEvent!!,offline,{}, {},recordRepository=repo,suppliedMetrics=vm,contextViewModel=ctxVm) } }
        compose.waitUntil(10000) { vm.uiState.value.metrics != null && ctxVm.uiState.value.context is DemoContextState.Ready }
    }
    private fun scroll(text:String) { compose.onAllNodes(hasScrollAction()).onLast().performScrollToNode(hasText(text)) }
    @After fun cleanup() { compose.runOnIdle { store.clear() };handle?.close();check(name.startsWith("test-event-ui-"));context.deleteDatabase(name)
        listOf("",".bak",".new").forEach { File(context.noBackupFilesDir,"$name.wrapped-key$it").delete() }
        KeyStore.getInstance("AndroidKeyStore").apply { load(null);deleteEntry("g6.database.$name") }
    }
    private fun write(label:String,value:String) { scroll(label);compose.onNode(hasText(label) and hasSetTextAction()).performTextReplacement(value) }
    private fun stored()=runBlocking(Dispatchers.IO) { (RoomAdmissionRepository(handle!!.database,ctx).get(saved) as ReadAdmissionResult.Found).capture }
    @Test fun editingThroughHubPreservesSchoolCommuneAndCaptureWhileSchedulingMultipleDays() {
        start(supervisor=true);val original=stored()
        scroll("Editar evento");compose.onNodeWithText("Editar evento").performClick()
        compose.waitUntil(10000) { ctxVm.uiState.value.eventEditor!=null }
        scroll("Colegio o institución");compose.onNode(hasText("Colegio o institución") and hasSetTextAction()).assertTextContains("Colegio ficticio A")
        scroll("Comuna del colegio");compose.onNode(hasText("Comuna del colegio") and hasSetTextAction()).assertTextContains("Comuna ficticia A")
        write("Nombre del evento","Evento editado ficticio")
        write("Inicio con offset","2026-10-12T09:00-03:00");write("Término con offset","2026-10-15T17:00-03:00")
        write("Latitud declarada (opcional)","-34.0");write("Longitud declarada (opcional)","-70.0")
        scroll("Guardar evento");compose.onNodeWithText("Guardar evento").performClick()
        compose.waitUntil(10000) { ctxVm.uiState.value.eventEditor==null && !ctxVm.uiState.value.busy }
        val row=runBlocking(Dispatchers.IO) { handle!!.database.context().event(DemoSeed.id("event-a"))!! }
        assertEquals("Evento editado ficticio",row.name);assertEquals("Colegio ficticio A",row.establishment)
        assertEquals("Comuna ficticia A",row.establishmentCommune);assertEquals("2026-10-15T20:00:00Z",row.endsAt)
        assertEquals(-34.0,row.latitude!!,0.0);assertEquals(original,stored())
    }
    @Test fun invalidCreationPreservesInputAndConfirmationControlsDiscardThenSuccessfulSelection() {
        start(supervisor=true);scroll("Nuevo evento");compose.onNodeWithText("Nuevo evento").performClick()
        compose.waitUntil(10000) { ctxVm.uiState.value.eventEditor!=null };assertNull(ctxVm.uiState.value.eventEditor!!.siteId)
        write("Nombre del evento","Nuevo evento ficticio")
        scroll("Sede ficticia 1 — DEMO");compose.onNode(hasText("Sede ficticia 1 — DEMO") and hasClickAction()).performClick()
        write("Inicio con offset","2026-10-12T09:00-03:00");write("Término con offset","2026-10-11T09:00-03:00")
        scroll("Guardar evento");compose.onNodeWithText("Guardar evento").performClick()
        compose.waitUntil(10000) { ctxVm.uiState.value.message!=null }
        assertEquals(3,runBlocking(Dispatchers.IO) { handle!!.database.context().events().size })
        scroll("Cancelar edición de evento");compose.onNodeWithText("Cancelar edición de evento").performClick();compose.onNodeWithText("Conservar evento").performClick()
        assertEquals("Nuevo evento ficticio",ctxVm.uiState.value.eventEditor!!.name)
        write("Término con offset","2026-10-13T17:00-03:00")
        scroll("Captador A — DEMO");compose.onNodeWithText("Captador A — DEMO").performClick()
        scroll("Guardar evento");compose.onNodeWithText("Guardar evento").performClick()
        compose.waitUntil(10000) { ctxVm.uiState.value.eventEditor==null && !ctxVm.uiState.value.busy }
        val snapshot=(ctxVm.uiState.value.context as DemoContextState.Ready).snapshot
        assertEquals("Nuevo evento ficticio",snapshot.selectedEvent!!.name)
        assertEquals(setOf(id("captor-a")),snapshot.selectedEvent!!.assignedCaptorIds)
        assertEquals(4,runBlocking(Dispatchers.IO) { handle!!.database.context().events().size })
        scroll("Nuevo evento");compose.onNodeWithText("Nuevo evento").performClick();write("Nombre del evento","Cambio descartado ficticio")
        scroll("Cancelar edición de evento");compose.onNodeWithText("Cancelar edición de evento").performClick()
        compose.onNodeWithText("Descartar evento").performClick();assertNull(ctxVm.uiState.value.eventEditor)
        assertEquals(4,runBlocking(Dispatchers.IO) { handle!!.database.context().events().size })
    }
}
