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

/** Sólo bases test-records-* y datos ficticios; no abre ni borra captures-demo.db. */
class RecordStorageTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "test-records-${UUID.randomUUID()}.db"
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
        handle?.close(); check(name.startsWith("test-records-")); context.deleteDatabase(name)
        listOf("", ".bak", ".new").forEach { File(context.noBackupFilesDir, "$name.wrapped-key$it").delete() }
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry("g6.database.$name") }
    }

    @Test fun invalidEditCannotCorruptReadyAndOptionalValuesCanBeCleared() = runBlocking {
        val old = save()
        for (bad in listOf(old.input.copy(email = "x"), old.input.copy(phone = "123"), old.input.copy(residenceCommune = "x"))) {
            assertTrue(records().edit(edit(old, bad)) is RecordMutationResult.Invalid)
            assertEquals(old, (admissions().get(old.id) as ReadAdmissionResult.Found).capture)
        }
        val changed = old.input.copy(email = " corrected@example.invalid ", establishment = null, notes = null, preferredSiteIds = emptyList())
        assertEquals(RecordMutationResult.Saved(old.id, 2, CaptureState.READY), records().edit(edit(old, changed)))
        handle!!.close(); handle = null
        val restored = (admissions().get(old.id) as ReadAdmissionResult.Found).capture
        assertEquals("corrected@example.invalid", restored.input.email); assertNull(restored.input.establishment); assertNull(restored.input.notes)
        assertTrue(restored.sites.isEmpty()); assertEquals(old.consent, restored.consent); assertEquals(old.capturedAt, restored.capturedAt)
        assertEquals(old.location, restored.location); assertEquals(old.programs, restored.programs)
        val audit = db().captures().audit(old.id.toString()).last()
        assertEquals(1L, audit.previousRevision); assertEquals(2L, audit.resultingRevision)
        assertTrue(audit.changedFields!!.contains("email")); assertFalse(audit.changedFields.contains("corrected@example.invalid"))
    }
    @Test fun draftCanConfirmAndNeedsContextCompletesWithoutChangingIdentity() = runBlocking {
        val draft = save(AdmissionInput(), draft = true, location = false)
        assertEquals(RecordMutationResult.Saved(draft.id, 2, CaptureState.NEEDS_CONTEXT), records().edit(edit(draft, input())))
        val pending = (admissions().get(draft.id) as ReadAdmissionResult.Found).capture
        assertEquals(RecordMutationResult.InvalidTransition, records().edit(edit(pending, pending.input.copy(rut = "87654321-K"))))
        assertEquals(RecordMutationResult.InvalidTransition, records().edit(edit(pending, intent = SaveIntent.SAVE_DRAFT)))
        val gps = CaptureLocation.Device(Coordinates(-33.0, -71.0), now, 30.0)
        assertEquals(RecordMutationResult.Saved(draft.id, 3, CaptureState.READY), records().edit(edit(pending,
            pending.input.copy(rut = "12.345.678-5"), location = gps)))
        val ready = (admissions().get(draft.id) as ReadAdmissionResult.Found).capture
        assertEquals(draft.capturedAt, ready.capturedAt); assertEquals(draft.consent, ready.consent)
        assertNotNull(db().captures().duplicate(ready.eventId.toString(), ready.input.rut))
    }
    @Test fun duplicateCompletionDoesNotChangeDraftAndRestrictedDuplicateDoesNotRevealId() = runBlocking {
        val ready = save(); val draft = save(AdmissionInput(), draft = true, location = false)
        assertEquals(RecordMutationResult.DuplicateOwn(ready.id), records().edit(edit(draft, input())))
        assertEquals(draft, (admissions().get(draft.id) as ReadAdmissionResult.Found).capture)
        assertEquals(RecordMutationResult.Saved(ready.id, 2, CaptureState.CANCELLED), records().cancel(CancelRecord(ready.id, 1, "Anulación ficticia")))
        // Fixture de otro captador legítimamente asignado al mismo evento/sede.
        db().context().insertScopes(listOf(ProfileSiteRow(DemoSeed.id("captor-b"), DemoSeed.id("site-1"))))
        db().context().insertAssignments(listOf(DemoAssignmentRow(DemoSeed.id("event-a"), DemoSeed.id("captor-b"))))
        select("captor-b", "event-a"); val other = save(); select()
        val result = records().edit(edit(draft, input()))
        assertEquals(RecordMutationResult.DuplicateRestricted, result)
        assertFalse(result.toString().contains(other.id.toString()))
        assertEquals(draft, (admissions().get(draft.id) as ReadAdmissionResult.Found).capture)
    }
    @Test fun scopeLossDeniesListReadEditAndCancellationWithoutLeakingRevision() = runBlocking {
        val old = save(); actor = actor.copy(siteIds = emptySet())
        assertTrue((records().list(RecordQuery()) as RecordPageResult.Page).items.isEmpty())
        assertEquals(ReadAdmissionResult.Unavailable, admissions().get(old.id))
        assertEquals(RecordMutationResult.Unavailable, records().edit(edit(old).copy(expectedRevision = 99)))
        assertEquals(RecordMutationResult.Unavailable, records().cancel(CancelRecord(old.id, 99, "Motivo ficticio")))
        assertEquals(1L, db().captures().capture(old.id.toString())!!.revision)
    }
    @Test fun supervisorNeedsReasonAndPreservesConsentOwnerAndHistoricalLocation() = runBlocking {
        val ready = save(); val draft = save(AdmissionInput(), draft = true, location = false)
        actor = LocalActor(id("supervisor"), LocalRole.SUPERVISOR, setOf(id("site-1")))
        assertEquals(RecordMutationResult.Unavailable, records().edit(edit(draft, intent = SaveIntent.SAVE_DRAFT)))
        assertEquals(RecordMutationResult.ReasonRequired, records().edit(edit(ready, ready.input.copy(email = "new@example.invalid"))))
        val changed = records().edit(edit(ready, ready.input.copy(email = "new@example.invalid"), reason = "Corrección ficticia"))
        assertEquals(RecordMutationResult.Saved(ready.id, 2, CaptureState.READY), changed)
        val restored = (admissions().get(ready.id) as ReadAdmissionResult.Found).capture
        assertEquals(ready.ownerId, restored.ownerId); assertEquals(ready.consent, restored.consent); assertEquals(ready.location, restored.location)
        assertEquals("Corrección ficticia", db().captures().audit(ready.id.toString()).last().reasonText)
    }
    @Test fun auditFailureRollsBackEditSelectionsKeyRevisionAndCancellation() = runBlocking {
        val ready = save(); val sql = db().openHelper.writableDatabase
        sql.execSQL("CREATE TEMP TRIGGER fail_record_audit BEFORE INSERT ON local_audit BEGIN SELECT RAISE(ABORT, 'fixture rollback'); END")
        assertEquals(RecordMutationResult.StorageUnavailable, records().edit(edit(ready, ready.input.copy(email = "new@example.invalid",
            interests = listOf(RankedInterest(id("program-2"), 1)), preferredSiteIds = emptyList()))))
        assertEquals(ready, (admissions().get(ready.id) as ReadAdmissionResult.Found).capture)
        assertEquals(RecordMutationResult.StorageUnavailable, records().cancel(CancelRecord(ready.id, 1, "Motivo ficticio")))
        assertEquals(ready, (admissions().get(ready.id) as ReadAdmissionResult.Found).capture)
        assertNotNull(db().captures().duplicate(ready.eventId.toString(), ready.input.rut)); assertEquals(1, db().captures().audit(ready.id.toString()).size)
    }
    @Test fun cancellationPersistsReasonReleasesKeyAndRejectsStaleRevision() = runBlocking {
        val old = save()
        val first = records().cancel(CancelRecord(old.id, 1, "  Motivo ficticio  "))
        assertEquals(RecordMutationResult.Saved(old.id, 2, CaptureState.CANCELLED), first)
        assertEquals(RecordMutationResult.RevisionConflict, records().cancel(CancelRecord(old.id, 1, "Motivo ficticio")))
        assertEquals(RecordMutationResult.InvalidTransition, records().edit(edit(old).copy(expectedRevision = 2)))
        assertNull(db().captures().duplicate(old.eventId.toString(), old.input.rut))
        handle!!.close(); handle = null
        val audit = db().captures().audit(old.id.toString()).last()
        assertEquals("Motivo ficticio", audit.reasonText); assertEquals(1L, audit.previousRevision); assertEquals("[\"state\"]", audit.changedFields)
        assertEquals(old.input, (admissions().get(old.id) as ReadAdmissionResult.Found).capture.input)
    }
    @Test fun cursorPagesTiedTimestampsWithoutOmissionsAndSupervisorCannotSeeOtherDrafts() = runBlocking {
        repeat(120) { save(AdmissionInput(givenNames = "Persona ficticia $it"), draft = true, location = false) }
        val ids = mutableListOf<UUID>(); var cursor: RecordCursor? = null
        do { val page = records().list(RecordQuery(after = cursor, limit = 30)) as RecordPageResult.Page
            ids += page.items.map { it.id }; cursor = page.next
        } while (cursor != null)
        assertEquals(120, ids.size); assertEquals(120, ids.toSet().size)
        assertEquals(ids.sortedByDescending(UUID::toString), ids)
        val matched = records().list(RecordQuery(RecordFilter(query = "persona FICTÍCIA"), limit = 100)) as RecordPageResult.Page
        assertEquals(100, matched.items.size); assertNotNull(matched.next)
        assertTrue((records().list(RecordQuery(RecordFilter(query = "."))) as RecordPageResult.Page).items.isEmpty())
        actor = LocalActor(id("supervisor"), LocalRole.SUPERVISOR, setOf(id("site-1")))
        assertTrue((records().list(RecordQuery()) as RecordPageResult.Page).items.isEmpty())
        actor = actor.copy(id = id("captor-a"))
        assertEquals(30, (records().list(RecordQuery()) as RecordPageResult.Page).items.size)
    }
    @Test fun concurrentEditsCommitOneRevisionAndCancellationPropagates() = runBlocking {
        val old = save()
        val results = (1..2).map { n -> async(Dispatchers.Default) { records().edit(edit(old, old.input.copy(email = "change$n@example.invalid"))) } }.awaitAll()
        assertEquals(1, results.count { it is RecordMutationResult.Saved }); assertEquals(1, results.count { it == RecordMutationResult.RevisionConflict })
        val cancelled = RoomRecordRepository(db(), object : AdmissionContextSource {
            override suspend fun currentActor(): LocalActor = throw CancellationException("Fixture")
            override suspend fun environment(eventId: UUID): AdmissionEnvironment? = null
        }, clock)
        try { cancelled.list(RecordQuery()); fail("Debe propagar cancelación") } catch (_: CancellationException) { }
    }
}
