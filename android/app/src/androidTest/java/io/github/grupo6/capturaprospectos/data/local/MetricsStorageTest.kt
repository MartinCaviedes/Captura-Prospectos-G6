package io.github.grupo6.capturaprospectos.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.platform.app.InstrumentationRegistry
import io.github.grupo6.capturaprospectos.domain.admission.*
import io.github.grupo6.capturaprospectos.domain.capture.*
import io.github.grupo6.capturaprospectos.domain.context.*
import io.github.grupo6.capturaprospectos.domain.records.*
import java.io.File
import java.security.KeyStore
import java.time.*
import java.util.UUID
import kotlinx.coroutines.*
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*

/** Sólo bases test-metrics-* y datos ficticios; no abre ni borra captures-demo.db. */
class MetricsStorageTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "test-metrics-${UUID.randomUUID()}.db"
    private var handle: EncryptedDatabase? = null
    private val now = Instant.parse("2026-10-10T15:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private lateinit var actor: LocalActor
    private lateinit var environment: AdmissionEnvironment
    private fun id(key: String) = UUID.fromString(DemoSeed.id(key))
    private fun db(): CaptureDatabase {
        if (handle == null) handle = EncryptedDatabase.open(context, name)
        return handle!!.database
    }
    private fun ctx() = RoomDemoContextRepository(db(), clock)
    private val source = object : AdmissionContextSource {
        override suspend fun currentActor() = actor
        override suspend fun environment(eventId: UUID) = environment.takeIf { it.context.event.id == eventId }
    }
    private fun admissions() = RoomAdmissionRepository(db(), source, clock = clock)
    private fun records() = RoomRecordRepository(db(), source, clock)
    private fun input() = AdmissionInput(givenNames = "Persona Ficticia", familyNames = "Demostracion",
        rut = "12345678-5", email = "demo@example.invalid", phone = "+56900000000", birthDate = "2000-01-01",
        residenceCommune = "Comuna ficticia", currentActivity = CurrentActivity.SECONDARY_GRADUATE,
        establishment = "Colegio ficticio", notes = "Nota ficticia",
        interests = listOf(RankedInterest(id("program-1"), 1)), preferredSiteIds = listOf(id("site-3")))
    private suspend fun select(captor: String = "captor-a", event: String = "event-a") {
        ctx().selectProfile(id(captor)); ctx().selectEvent(id(event))
        actor = requireNotNull(ctx().currentActor())
        environment = requireNotNull(ctx().environment(id(event)))
    }
    private suspend fun save(v: AdmissionInput = input(), draft: Boolean = false, location: Boolean = true): StoredAdmission {
        val result = admissions().create(NewAdmission(environment.context.event.id, v,
            if (draft) SaveIntent.SAVE_DRAFT else SaveIntent.CONFIRM_REGISTRATION,
            ConsentEvidence(DemoSeed.CONSENT, now, actor.id, AcceptanceMode.DIRECT),
            if (location) CaptureLocation.Device(Coordinates(-33.0, -71.0), now, 20.0) else null)) as CreateAdmissionResult.Saved
        return (admissions().get(result.id) as ReadAdmissionResult.Found).capture
    }
    private fun edit(old: StoredAdmission, v: AdmissionInput = old.input, intent: SaveIntent = SaveIntent.CONFIRM_REGISTRATION,
        location: CaptureLocation? = null, reason: String? = null) = EditAdmission(old.id, old.revision, v, intent, location, reason)
    @Before fun setup() = runBlocking { ctx().initialize(); select() }
    @After fun cleanup() {
        handle?.close(); check(name.startsWith("test-metrics-")); context.deleteDatabase(name)
        listOf("", ".bak", ".new").forEach { File(context.noBackupFilesDir, "$name.wrapped-key$it").delete() }
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry("g6.database.$name") }
    }

    @Test fun activePeopleProgramsAndStatesAreSeparatedWithinDayAndEvent() = runBlocking {
        val ready=save(input().copy(interests=listOf(RankedInterest(id("program-1"),1),RankedInterest(id("program-2"),2))))
        val pending=save(input().copy(rut="11111111-1",interests=listOf(RankedInterest(id("program-2"),1))),location=false)
        save(AdmissionInput(),draft=true,location=false)
        val cancelled=save(input().copy(rut="22222222-2"));records().cancel(CancelRecord(cancelled.id,1,"Motivo ficticio"))
        select(event="event-a2");save();select()
        val all=(records().getMetrics(MetricsQuery(id("event-a"))) as MetricsResult.Calculated).metrics
        assertEquals(2,all.totalCaptures);assertEquals(2,all.distinctPeople);assertEquals(1,all.readyCount)
        assertEquals(1,all.needsContextCount);assertEquals(1,all.draftCount);assertEquals(1,all.cancelledCount)
        assertEquals(mapOf(id("program-1") to 1,id("program-2") to 2),all.programMetrics.associate { it.programId to it.registrations })
        assertEquals(1,(records().getMetrics(MetricsQuery(id("event-a2"))) as MetricsResult.Calculated).metrics.distinctPeople)
        val from=Instant.parse("2026-10-10T03:00:00Z");val until=Instant.parse("2026-10-11T03:00:00Z")
        db().openHelper.writableDatabase.execSQL("UPDATE captures SET capturedAt=? WHERE id=?",arrayOf(until.toString(),pending.id.toString()))
        db().openHelper.writableDatabase.execSQL("UPDATE captures SET capturedAt=? WHERE id=?",arrayOf(from.toString(),ready.id.toString()))
        val day=(records().getMetrics(MetricsQuery(id("event-a"),from,until)) as MetricsResult.Calculated).metrics
        assertEquals(1,day.totalCaptures);assertEquals(from,day.fromInclusive);assertEquals(until,day.untilExclusive)
        assertEquals(2,day.programMetrics.sumOf { it.registrations })
    }
    @Test fun supervisorAndCaptorScopeExcludeForeignDraftsAndUnknownEvents() = runBlocking {
        save();save(AdmissionInput(),draft=true,location=false)
        db().context().insertScopes(listOf(ProfileSiteRow(DemoSeed.id("captor-b"),DemoSeed.id("site-1"))))
        db().context().insertAssignments(listOf(DemoAssignmentRow(DemoSeed.id("event-a"),DemoSeed.id("captor-b"))))
        select("captor-b","event-a");save(input().copy(rut="11111111-1"));save(AdmissionInput(),draft=true,location=false)
        assertEquals(1,(records().getMetrics(MetricsQuery(id("event-a"))) as MetricsResult.Calculated).metrics.totalCaptures)
        actor=LocalActor(id("supervisor"),LocalRole.SUPERVISOR,setOf(id("site-1")))
        val metrics=(records().getMetrics(MetricsQuery(id("event-a"))) as MetricsResult.Calculated).metrics
        assertEquals(2,metrics.totalCaptures);assertEquals(0,metrics.draftCount)
        assertEquals(DemoSeed.sites.first().name,metrics.siteMetrics.single().siteName)
        actor=actor.copy(siteIds=emptySet())
        assertEquals(MetricsResult.Unavailable,records().getMetrics(MetricsQuery(id("event-a"))))
        assertEquals(MetricsResult.Unavailable,records().getMetrics(MetricsQuery(UUID.randomUUID())))
    }
    @Test fun invalidRangeAndCorruptTimestampReturnErrorsRatherThanZeros() = runBlocking {
        val row=save()
        assertEquals(MetricsResult.InvalidQuery,records().getMetrics(MetricsQuery(id("event-a"),now,now)))
        assertEquals(MetricsResult.InvalidQuery,records().getMetrics(MetricsQuery(id("event-a"),now,null)))
        db().openHelper.writableDatabase.execSQL("UPDATE captures SET capturedAt='invalid' WHERE id=?",arrayOf(row.id.toString()))
        assertEquals(MetricsResult.StorageUnavailable,records().getMetrics(MetricsQuery(id("event-a"))))
        assertEquals("invalid",db().captures().capture(row.id.toString())!!.capturedAt)
    }
    @Test fun storageFailureAndCoroutineCancellationAreNotEmptyMetrics() = runBlocking {
        save();val started=CompletableDeferred<Unit>()
        val blocking=object:AdmissionContextSource {
            override suspend fun currentActor():LocalActor? { started.complete(Unit);awaitCancellation() }
            override suspend fun environment(eventId:UUID):AdmissionEnvironment?=null
        }
        val job=async { RoomRecordRepository(db(),blocking,clock).getMetrics(MetricsQuery(id("event-a"))) }
        started.await();job.cancelAndJoin();assertTrue(job.isCancelled)
        handle!!.close();assertEquals(MetricsResult.StorageUnavailable,records().getMetrics(MetricsQuery(id("event-a"))))
    }
}
