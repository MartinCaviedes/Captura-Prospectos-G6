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

/** Sólo bases test-events-* y datos ficticios; no abre ni borra captures-demo.db. */
class EventStorageTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "test-events-${UUID.randomUUID()}.db"
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
        handle?.close(); check(name.startsWith("test-events-")); context.deleteDatabase(name)
        listOf("", ".bak", ".new").forEach { File(context.noBackupFilesDir, "$name.wrapped-key$it").delete() }
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry("g6.database.$name") }
    }

    private suspend fun supervisor() { ctx().selectProfile(id("supervisor")) }
    private fun command(key:String="event-a"):SaveDemoEvent {
        val row=db().context().event(DemoSeed.id(key))!!
        return SaveDemoEvent(UUID.fromString(row.id),UUID.fromString(row.siteId),row.name,Instant.parse(row.startsAt),Instant.parse(row.endsAt),ZoneId.of(row.timeZone),
            if(row.latitude==null) null else Coordinates(row.latitude,row.longitude!!),row.establishment,row.establishmentCommune,
            db().context().assignments(row.id).map { UUID.fromString(it.profileId) }.toSet())
    }
    @Test fun originAndDestinationScopeAndAssignedCaptorRoleAreValidatedBeforeWriting()=runBlocking {
        supervisor();db().openHelper.writableDatabase.execSQL("DELETE FROM demo_profile_sites WHERE profileId=? AND siteId=?",arrayOf(DemoSeed.id("supervisor"),DemoSeed.id("site-2")))
        val foreign=db().context().event(DemoSeed.id("event-b"))!!
        assertEquals(SaveEventResult.Unauthorized,ctx().saveEvent(command("event-b").copy(siteId=id("site-1"))))
        assertEquals(foreign,db().context().event(foreign.id))
        val before=db().context().event(DemoSeed.id("event-a"));val assignments=db().context().assignments(DemoSeed.id("event-a"))
        for(captor in listOf(id("captor-b"),id("supervisor"),UUID.randomUUID())) {
            assertTrue(ctx().saveEvent(command().copy(assignedCaptorIds=setOf(captor))) is SaveEventResult.Invalid)
            assertEquals(before,db().context().event(DemoSeed.id("event-a")));assertEquals(assignments,db().context().assignments(DemoSeed.id("event-a")))
        }
        assertEquals(SaveEventResult.Unauthorized,ctx().saveEvent(command().copy(eventId=UUID.randomUUID())))
        assertEquals(SaveEventResult.Unauthorized,ctx().saveEvent(command().copy(eventId=null,siteId=id("site-3"))))
    }
    @Test fun editingPreservesSchoolAndCaptureSnapshotsWhileChangingExplicitDates()=runBlocking {
        val capture=save();val before=command();supervisor();ctx().selectEvent(id("event-a"))
        val altered=before.copy(name="Evento ficticio corregido",startsAt=Instant.parse("2026-10-12T12:00:00Z"),endsAt=Instant.parse("2026-10-15T20:00:00Z"),coordinates=Coordinates(-34.0,-70.0))
        assertEquals(SaveEventResult.Success(id("event-a")),ctx().saveEvent(altered))
        val after=command();assertEquals(before.establishment,after.establishment);assertEquals(before.establishmentCommune,after.establishmentCommune)
        assertEquals(altered.startsAt,after.startsAt);assertEquals(altered.endsAt,after.endsAt)
        assertEquals(capture,(admissions().get(capture.id) as ReadAdmissionResult.Found).capture)
        val snapshot=(ctx().snapshot() as DemoContextState.Ready).snapshot
        assertEquals(altered.name,snapshot.selectedEvent!!.name);assertEquals(before.establishment,snapshot.establishmentSuggestion!!.name)
    }
    @Test fun failedAssignmentInsertRollsBackEventAssignmentsAndActiveContext()=runBlocking {
        supervisor();ctx().selectEvent(id("event-a"));val before=command();val contextBefore=db().context().state()
        db().openHelper.writableDatabase.execSQL("CREATE TEMP TRIGGER fail_event_assignment BEFORE INSERT ON demo_assignments BEGIN SELECT RAISE(ABORT,'fixture rollback'); END")
        assertEquals(SaveEventResult.StorageUnavailable,ctx().saveEvent(before.copy(name="Nombre ficticio corregido")))
        assertEquals(before,command());assertEquals(contextBefore,db().context().state())
    }
    @Test fun revokedAssignmentPreventsNewCaptureWithoutDeletingExistingRegistration()=runBlocking {
        val capture=save();supervisor();assertTrue(ctx().saveEvent(command().copy(assignedCaptorIds=emptySet())) is SaveEventResult.Success)
        ctx().selectProfile(id("captor-a"))
        val snapshot=(ctx().snapshot() as DemoContextState.Ready).snapshot
        assertFalse(snapshot.availableEvents.any { it.id==id("event-a") });assertEquals(ContextChange.INVALID_SELECTION,ctx().selectEvent(id("event-a")))
        assertNull(ctx().environment(id("event-a")));assertEquals(capture,(admissions().get(capture.id) as ReadAdmissionResult.Found).capture)
        assertTrue(db().captures().capture(capture.id.toString())!=null)
    }
}
