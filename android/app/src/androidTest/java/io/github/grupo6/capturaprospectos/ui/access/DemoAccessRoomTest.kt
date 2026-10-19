package io.github.grupo6.capturaprospectos.ui.access

import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import io.github.grupo6.capturaprospectos.data.local.*
import io.github.grupo6.capturaprospectos.domain.capture.*
import io.github.grupo6.capturaprospectos.domain.admission.*
import io.github.grupo6.capturaprospectos.domain.context.*
import io.github.grupo6.capturaprospectos.domain.records.*
import io.github.grupo6.capturaprospectos.ui.context.DemoContextViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.io.File
import java.security.KeyStore
import java.util.UUID
import org.junit.*
import org.junit.Assert.*

class DemoAccessRoomTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val name = "test-demo-access-${UUID.randomUUID()}.db"
    private var handle: EncryptedDatabase? = null
    private val store = ViewModelStore()
    private fun id(key: String) = UUID.fromString(DemoSeed.id(key))
    private fun main(action: () -> Unit) = instrumentation.runOnMainSync(action)
    @After fun cleanup() {
        main { store.clear() }
        handle?.close()
        check(name.startsWith("test-demo-access-"));context.deleteDatabase(name)
        listOf("", ".bak", ".new").forEach { File(context.noBackupFilesDir,"$name.wrapped-key$it").delete() }
        KeyStore.getInstance("AndroidKeyStore").apply { load(null);deleteEntry("g6.database.$name") }
    }
    @Test fun credentialsSelectOnlyExistingFixtureAndLogoutPreservesRecordsWhileFreshAccessNeedsLogin() = runBlocking {
        handle = EncryptedDatabase.open(context, name)
        val repository = RoomDemoContextRepository(handle!!.database)
        repository.initialize()
        lateinit var ctx: DemoContextViewModel
        lateinit var access: DemoAccessViewModel
        main { ctx = DemoContextViewModel(repository);access = DemoAccessViewModel();store.put("ctx",ctx);store.put("access",access) }
        withTimeout(10000) { ctx.uiState.first { it.context is DemoContextState.Ready } }
        main { access.begin();access.submit("captador.a@g6.example", "incorrecta", ctx) }
        assertNull(access.uiState.value.actorId)
        assertNull((repository.snapshot() as DemoContextState.Ready).snapshot.currentProfile)
        main { access.submit("supervisor@g6.example", DemoAccessFixtures.PASSWORD, ctx) }
        withTimeout(10000) { access.uiState.first { !it.busy && it.actorId != null } }
        assertEquals(id("supervisor"),access.uiState.value.actorId)
        assertEquals(LocalRole.SUPERVISOR,(repository.snapshot() as DemoContextState.Ready).snapshot.currentProfile!!.actor.role)
        main { access.logout() }
        assertNull(access.uiState.value.actorId)
        assertEquals(3,handle!!.database.context().profiles().size)
        main { val fresh = DemoAccessViewModel();store.put("fresh",fresh);assertTrue(fresh.uiState.value.welcome);assertNull(fresh.uiState.value.actorId) }
        assertNull(DemoAccessFixtures.match("supervisor@g6.example", DemoAccessFixtures.PASSWORD, emptyList()))
    }
    @Test fun fixtureMixIsIdempotentPreservesManualEditsAndSupervisorScope() = runBlocking {
        handle = EncryptedDatabase.open(context, name)
        val ctx = RoomDemoContextRepository(handle!!.database)
        ctx.initialize();ctx.selectProfile(id("captor-a"));ctx.selectEvent(id("event-a"))
        val environment = ctx.environment(id("event-a"))!!
        lateinit var loader: DemoDataViewModel
        main { loader = DemoDataViewModel(RoomAdmissionRepository(handle!!.database,ctx), RoomRecordRepository(handle!!.database,ctx));store.put("data",loader);loader.load(environment);loader.load(environment) }
        withTimeout(15000) { loader.uiState.first { !it.busy && it.message != null } }
        assertTrue(loader.uiState.value.message!!.startsWith("60 prospectos"))
        val records = RoomRecordRepository(handle!!.database,ctx)
        val first = (records.getMetrics(MetricsQuery(id("event-a"))) as MetricsResult.Calculated).metrics
        assertEquals(42,first.distinctPeople);assertEquals(24,first.needsContextCount);assertEquals(18,first.readyCount)
        assertEquals(12,first.draftCount);assertEquals(6,first.cancelledCount)
        val rows = (records.list(RecordQuery(limit=100)) as RecordPageResult.Page).items
        assertEquals(60,rows.size)
        assertEquals(66,handle!!.database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM local_audit").use { it.moveToFirst(); it.getInt(0) })
        assertEquals(3,first.programMetrics.size);assertEquals(72,first.programMetrics.sumOf { it.registrations })
        val draft = rows.first { it.state == CaptureState.DRAFT }
        val admission = (records.get(draft.id) as ReadAdmissionResult.Found).capture
        assertTrue(records.edit(EditAdmission(draft.id,draft.revision,
            admission.input.copy(givenNames="Edición ficticia conservada"),SaveIntent.SAVE_DRAFT,null)) is RecordMutationResult.Saved)
        main { loader.load(environment) }
        withTimeout(15000) { loader.uiState.first { !it.busy && it.message != null } }
        assertTrue(loader.uiState.value.message!!.startsWith("0 prospectos ficticios creados; 60"))
        assertEquals(60,(records.list(RecordQuery(limit=100)) as RecordPageResult.Page).items.size)
        assertEquals("Edición ficticia conservada",(records.get(draft.id) as ReadAdmissionResult.Found).capture.input.givenNames)
        ctx.selectProfile(id("supervisor"));ctx.selectEvent(id("event-a"))
        val supervised = (records.getMetrics(MetricsQuery(id("event-a"))) as MetricsResult.Calculated).metrics
        assertEquals(42,supervised.distinctPeople);assertEquals(0,supervised.draftCount);assertEquals(6,supervised.cancelledCount)
        main { loader.load(environment) }
        withTimeout(15000) { loader.uiState.first { !it.busy && it.message != null } }
        assertTrue(loader.uiState.value.message!!.contains("La carga se detuvo"))
        assertEquals(48,(records.list(RecordQuery(limit=100)) as RecordPageResult.Page).items.size)
        ctx.selectProfile(id("captor-b"));ctx.selectEvent(id("event-b"))
        assertEquals(0,(records.list(RecordQuery()) as RecordPageResult.Page).items.size)
    }

    @Test fun interruptedLoadResumesWithoutDuplicatingLegacyDraftOrCancelledSamples() = runBlocking {
        handle = EncryptedDatabase.open(context,name)
        val ctx = RoomDemoContextRepository(handle!!.database)
        ctx.initialize();ctx.selectProfile(id("captor-a"));ctx.selectEvent(id("event-a"))
        val environment = ctx.environment(id("event-a"))!!
        val admissions = RoomAdmissionRepository(handle!!.database,ctx)
        val records = RoomRecordRepository(handle!!.database,ctx)
        suspend fun load(key:String, source:AdmissionRepository, target:RecordRepository):DemoDataViewModel {
            lateinit var vm:DemoDataViewModel
            main { vm=DemoDataViewModel(source,target);store.put(key,vm);vm.load(environment) }
            withTimeout(15000) { vm.uiState.first { !it.busy && it.message != null } }
            return vm
        }
        val legacyOnly=object:AdmissionRepository by admissions {
            override suspend fun create(request:NewAdmission):CreateAdmissionResult =
                if(request.input.givenNames=="Persona Ficticia 101") CreateAdmissionResult.StorageUnavailable
                else admissions.create(request)
        }
        load("legacy",legacyOnly,records)
        val old=(records.list(RecordQuery(limit=100)) as RecordPageResult.Page).items
        assertEquals(24,old.size);assertTrue(old.all { it.state==CaptureState.NEEDS_CONTEXT })
        val interrupted=object:RecordRepository by records {
            override suspend fun cancel(command:CancelRecord):RecordMutationResult = RecordMutationResult.StorageUnavailable
        }
        load("interrupted",admissions,interrupted)
        assertEquals(55,(records.list(RecordQuery(limit=100)) as RecordPageResult.Page).items.size)
        val resumed=load("resumed",admissions,records)
        assertTrue(resumed.uiState.value.message!!.startsWith("5 prospectos ficticios creados; 55"))
        val rows=(records.list(RecordQuery(limit=100)) as RecordPageResult.Page).items
        assertEquals(60,rows.size)
        assertTrue(rows.containsAll(old))
        val metrics=(records.getMetrics(MetricsQuery(id("event-a"))) as MetricsResult.Calculated).metrics
        assertEquals(18,metrics.readyCount);assertEquals(24,metrics.needsContextCount)
        assertEquals(12,metrics.draftCount);assertEquals(6,metrics.cancelledCount)
        assertEquals(66,handle!!.database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM local_audit").use { it.moveToFirst(); it.getInt(0) })
    }
}
