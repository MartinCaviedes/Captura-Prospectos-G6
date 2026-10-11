package io.github.grupo6.capturaprospectos.ui.access

import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import io.github.grupo6.capturaprospectos.data.local.*
import io.github.grupo6.capturaprospectos.domain.capture.*
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
    @Test fun fixtureLoadUsesRealEncryptedRepositoryIsIdempotentAndSupervisorSeesCountsWithoutInventedGps() = runBlocking {
        handle = EncryptedDatabase.open(context, name)
        val ctx = RoomDemoContextRepository(handle!!.database)
        ctx.initialize();ctx.selectProfile(id("captor-a"));ctx.selectEvent(id("event-a"))
        val environment = ctx.environment(id("event-a"))!!
        lateinit var loader: DemoDataViewModel
        main { loader = DemoDataViewModel(RoomAdmissionRepository(handle!!.database,ctx));store.put("data",loader);loader.load(environment);loader.load(environment) }
        withTimeout(15000) { loader.uiState.first { !it.busy && it.message != null } }
        assertTrue(loader.uiState.value.message!!.startsWith("24 prospectos"))
        val records = RoomRecordRepository(handle!!.database,ctx)
        val first = (records.getMetrics(MetricsQuery(id("event-a"))) as MetricsResult.Calculated).metrics
        assertEquals(24,first.distinctPeople);assertEquals(24,first.needsContextCount);assertEquals(0,first.readyCount)
        assertEquals(listOf(12,24),(first.programMetrics.map { it.registrations }).sorted())
        main { loader.load(environment) }
        withTimeout(15000) { loader.uiState.first { !it.busy && it.message != null } }
        assertTrue(loader.uiState.value.message!!.startsWith("0 prospectos ficticios creados; 24"))
        assertEquals(24,(records.list(RecordQuery()) as RecordPageResult.Page).items.size)
        ctx.selectProfile(id("supervisor"));ctx.selectEvent(id("event-a"))
        assertEquals(24,(records.getMetrics(MetricsQuery(id("event-a"))) as MetricsResult.Calculated).metrics.distinctPeople)
        main { loader.load(environment) }
        withTimeout(15000) { loader.uiState.first { !it.busy && it.message != null } }
        assertTrue(loader.uiState.value.message!!.contains("La carga se detuvo"))
        assertEquals(24,(records.list(RecordQuery()) as RecordPageResult.Page).items.size)
        ctx.selectProfile(id("captor-b"));ctx.selectEvent(id("event-b"))
        assertEquals(0,(records.list(RecordQuery()) as RecordPageResult.Page).items.size)
    }
}
