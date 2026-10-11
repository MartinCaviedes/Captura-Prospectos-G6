package io.github.grupo6.capturaprospectos.ui.hub

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

class HubMetricsRoomUiTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private val name="test-hub-metrics-${UUID.randomUUID()}.db"
    private var handle:EncryptedDatabase?=null
    private val store=ViewModelStore()
    private lateinit var ctx:RoomDemoContextRepository
    private lateinit var repo:RoomRecordRepository
    private lateinit var vm:MainHubMetricsViewModel
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
        compose.runOnIdle { vm=MainHubMetricsViewModel(repo,snapshot.selectedEvent!!);store.put("metrics",vm) }
        val offline=object:ConnectivityObserver { override fun observe()=flowOf(ConnectivityStatus.OFFLINE) }
        compose.setContent { CapturaProspectosG6Theme { MainHubRoute(snapshot.currentProfile!!,snapshot.selectedEvent!!,offline,{}, {},recordRepository=repo,suppliedMetrics=vm) } }
        compose.waitUntil(10000) { vm.uiState.value.metrics != null }
    }
    private fun scroll(text:String) { compose.onNode(hasScrollAction()).performScrollToNode(hasText(text)) }
    @After fun cleanup() { compose.runOnIdle { store.clear() };handle?.close();check(name.startsWith("test-hub-metrics-"));context.deleteDatabase(name)
        listOf("",".bak",".new").forEach { File(context.noBackupFilesDir,"$name.wrapped-key$it").delete() }
        KeyStore.getInstance("AndroidKeyStore").apply { load(null);deleteEntry("g6.database.$name") }
    }
    @Test fun supervisorSeesAuthorizedEventAndLocalCountsWithoutCapturePermission() {
        start(supervisor=true)
        scroll("Nueva Captura de Prospecto");compose.onNodeWithText("Nueva Captura de Prospecto").assertIsNotEnabled()
        scroll("Personas distintas: 1");compose.onNodeWithText("Personas distintas: 1").assertExists()
        assertEquals(DemoSeed.sites.first().name,vm.uiState.value.metrics!!.siteMetrics.single().siteName)
        assertNull(runBlocking(Dispatchers.IO) { ctx.environment(id("event-a")) })
    }
    @Test fun refreshingAfterCancellationUsesRoomAndInvalidRangePreservesLastCalculation() {
        start();scroll("Personas distintas: 1");compose.onNodeWithText("Personas distintas: 1").assertExists()
        runBlocking(Dispatchers.IO) { assertTrue(repo.cancel(CancelRecord(saved,1,"Motivo ficticio")) is RecordMutationResult.Saved) }
        scroll("Actualizar");compose.onNodeWithText("Actualizar").performClick()
        compose.waitUntil(10000) { vm.uiState.value.metrics?.totalCaptures==0 }
        assertEquals(1,vm.uiState.value.metrics!!.cancelledCount)
        scroll("Desde (aaaa-mm-dd)");compose.onNode(hasText("Desde (aaaa-mm-dd)") and hasSetTextAction()).performTextReplacement("2026-02-30")
        scroll("Hasta inclusive (aaaa-mm-dd)");compose.onNode(hasText("Hasta inclusive (aaaa-mm-dd)") and hasSetTextAction()).performTextReplacement("2026-03-01")
        scroll("Aplicar rango");compose.onNodeWithText("Aplicar rango").performClick()
        compose.waitUntil(10000) { vm.uiState.value.error != null }
        assertEquals(1,vm.uiState.value.metrics!!.cancelledCount)
        compose.onNodeWithText("Se conserva el último cálculo; revisa su período antes de usarlo.").assertExists()
    }
}
