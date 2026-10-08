package io.github.grupo6.capturaprospectos.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.grupo6.capturaprospectos.domain.admission.*
import io.github.grupo6.capturaprospectos.domain.capture.*
import java.io.File
import java.security.KeyStore
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdmissionStorageTest {
    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var name: String
    private var handle: EncryptedDatabase? = null
    private val now = Instant.parse("2026-10-06T15:00:00Z")
    private val site = UUID.randomUUID()
    private val actor = LocalActor(UUID.randomUUID(), LocalRole.CAPTOR, setOf(site))
    private var current = actor
    private val event = CaptureEvent(UUID.randomUUID(), site, "Evento ficticio", now.minusSeconds(60), now.plusSeconds(3600),
        ZoneId.of("America/Santiago"), setOf(actor.id), null)
    private var environment = CaptureEnvironment(actor, event, UUID.randomUUID(), emptyList(), "demo-v1", "Consentimiento ficticio")
    private val mention = UUID.randomUUID()
    private val programs = (1..3).map { ProgramOption(UUID.randomUUID(), "Programa ficticio $it", "demo-v1",
        if (it == 2) "DEMO_ONLINE" else "DEMO_PHYSICAL", mentions = if (it == 3) mapOf(mention to "Mención ficticia") else emptyMap()) }
    private val sites = (1..4).map { SiteOption(UUID.randomUUID(), "Sede ficticia $it", "demo-v1") }
    private val source = object : AdmissionContextSource {
        override suspend fun currentActor() = current
        override suspend fun environment(eventId: UUID) = AdmissionEnvironment(environment, programs, sites).takeIf { it.context.event.id == eventId }
    }
    private val input = AdmissionInput(givenNames = "Persona Ficticia", familyNames = "Demostracion", rut = "12345678-5",
        email = "demo@example.invalid", phone = "+56900000000", birthDate = "2000-01-01", residenceCommune = "Comuna ficticia",
        currentActivity = CurrentActivity.OTHER, currentActivityOther = "Trabajando", establishment = "Colegio ficticio",
        establishmentCommune = "Comuna del colegio", formPersonalOption = PersonalOption.NOT_STATED,
        preferredSchedule = PreferredSchedule.EVENING, interestLevel = InterestLevel.HIGH, motivation = Motivation.VOCATION,
        modality = "DEMO_MODE", contactChannel = "DEMO_CHANNEL", notes = "Nota ficticia",
        interests = programs.mapIndexed { i, p -> RankedInterest(p.id, i + 1, if (i == 2) mention else null) },
        preferredSiteIds = sites.map { it.id })
    private val consent = ConsentEvidence("demo-v1", now, actor.id, AcceptanceMode.DIRECT)
    private val request = NewAdmission(event.id, input, SaveIntent.CONFIRM_REGISTRATION, consent, null)
    private fun open(): CaptureDatabase {
        if (handle == null) handle = EncryptedDatabase.open(context, name)
        return requireNotNull(handle).database
    }
    private fun repo() = RoomAdmissionRepository(open(), source,
        PrepareAdmission(AdmissionValidator(setOf("DEMO_CHANNEL"), setOf("DEMO_MODE"))), Clock.fixed(now, ZoneOffset.UTC))
    @Before fun setup() { name = "test-f2027-${UUID.randomUUID()}.db" }
    @After fun cleanupOnlyTestData() {
        handle?.close()
        if (!::name.isInitialized) return
        check(name.startsWith("test-f2027-"))
        context.deleteDatabase(name)
        listOf("", ".bak", ".new").forEach { File(context.noBackupFilesDir, "$name.wrapped-key$it").delete() }
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry("g6.database.$name") }
    }

    @Test fun allFieldsRankingMentionAndFourSitesSurviveReopen() = runBlocking {
        val saved = repo().create(request) as CreateAdmissionResult.Saved
        handle!!.close(); handle = null
        val restored = (repo().get(saved.id) as ReadAdmissionResult.Found).capture
        assertEquals(input.copy(preferredSiteIds = emptyList()), restored.input.copy(preferredSiteIds = emptyList()))
        assertEquals(input.preferredSiteIds.toSet(), restored.input.preferredSiteIds.toSet())
        assertEquals("Persona Ficticia Demostracion", restored.fullNameSnapshot)
        assertEquals(consent, restored.consent); assertEquals(now, restored.capturedAt)
        assertEquals(site, restored.siteId); assertFalse(site in restored.input.preferredSiteIds)
        assertEquals(CaptureState.NEEDS_CONTEXT, restored.state)
        assertEquals(TransportState.NOT_QUEUED, restored.transportState)
        assertEquals(listOf(1, 2, 3), restored.programs.map { it.priority })
        assertEquals("Mención ficticia", restored.programs.last().mentionName)
        assertEquals("F2027_1", open().captures().capture(saved.id.toString())!!.contractVersion)
    }

    @Test fun missingConsentInvalidFieldsAndUnauthorizedContextWriteNothing() = runBlocking {
        assertEquals(CreateAdmissionResult.ConsentRequired, repo().create(request.copy(consent = null)))
        assertTrue(repo().create(request.copy(input = input.copy(birthDate = "2027-01-01"))) is CreateAdmissionResult.Invalid)
        environment = environment.copy(event = event.copy(siteId = sites[0].id))
        assertEquals(CreateAdmissionResult.AccessDenied, repo().create(request))
        assertEquals(0, open().captures().count()); assertNull(open().captures().consent("demo-v1"))
    }

    @Test fun duplicateAndDraftAccessRulesApplyToF2027() = runBlocking {
        val draft = repo().create(request.copy(input = AdmissionInput(), intent = SaveIntent.SAVE_DRAFT)) as CreateAdmissionResult.Saved
        val saved = repo().create(request) as CreateAdmissionResult.Saved
        assertEquals(CreateAdmissionResult.DuplicateOwn(saved.id), repo().create(request))
        current = actor.copy(id = UUID.randomUUID())
        environment = environment.copy(actor = current, event = event.copy(assignedCaptorIds = setOf(current.id)))
        assertEquals(CreateAdmissionResult.DuplicateRestricted, repo().create(request.copy(consent = consent.copy(recordedBy = current.id))))
        assertEquals(ReadAdmissionResult.Unavailable, repo().get(saved.id))
        current = current.copy(role = LocalRole.SUPERVISOR)
        assertEquals(ReadAdmissionResult.Unavailable, repo().get(draft.id))
        assertTrue(repo().get(saved.id) is ReadAdmissionResult.Found)
    }

    @Test fun concurrentF2027RegistrationsReserveOneKey() = runBlocking {
        val repository = repo()
        val results = listOf(async { repository.create(request) }, async { repository.create(request) }).awaitAll()
        assertEquals(1, results.count { it is CreateAdmissionResult.Saved })
        assertEquals(1, results.count { it is CreateAdmissionResult.DuplicateOwn })
        assertEquals(1, open().captures().count())
    }

    @Test fun lateFailureRollsBackCapturePreferencesConsentKeyAndAudit() = runBlocking {
        val db = open().openHelper.writableDatabase
        db.execSQL("CREATE TEMP TRIGGER fail_test_audit BEFORE INSERT ON local_audit BEGIN SELECT RAISE(ABORT, 'test rollback'); END")
        assertEquals(CreateAdmissionResult.StorageUnavailable, repo().create(request))
        for (table in listOf("captures", "capture_interests", "capture_site_preferences", "consent_text", "active_registration_keys", "local_audit")) {
            db.query("SELECT COUNT(*) FROM $table").use { assertTrue(it.moveToFirst()); assertEquals(0, it.getInt(0)) }
        }
        db.execSQL("DROP TRIGGER fail_test_audit")
        assertTrue(repo().create(request) is CreateAdmissionResult.Saved)
    }

    @Test fun sqlRejectsDuplicatePriorityAndSiteWithoutChangingSavedCapture() = runBlocking {
        val saved = repo().create(request) as CreateAdmissionResult.Saved
        val dao = open().captures()
        try {
            dao.insertInterests(listOf(dao.interests(saved.id.toString()).first().copy(programId = UUID.randomUUID().toString())))
            fail("Debe rechazar prioridad duplicada")
        } catch (_: android.database.sqlite.SQLiteConstraintException) { }
        try { dao.insertSites(listOf(dao.sites(saved.id.toString()).first())); fail("Debe rechazar sede duplicada") }
        catch (_: android.database.sqlite.SQLiteConstraintException) { }
        assertEquals(3, dao.interests(saved.id.toString()).size); assertEquals(4, dao.sites(saved.id.toString()).size)
    }

    @Test fun encryptedV1MigrationPreservesEveryOldValueAndDoesNotInventFields() = runBlocking {
        val schema = JSONObject(InstrumentationRegistry.getInstrumentation().context.assets.open(
            "io.github.grupo6.capturaprospectos.data.local.CaptureDatabase/1.json").bufferedReader().use { it.readText() }).getJSONObject("database")
        System.loadLibrary("sqlcipher")
        val password = DatabaseKeyStore(context, name).getOrCreate()
        val helper = SupportOpenHelperFactory(password).create(SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onConfigure(db: SupportSQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
                override fun onCreate(db: SupportSQLiteDatabase) {
                    val entities = schema.getJSONArray("entities")
                    for (i in 0 until entities.length()) {
                        val e = entities.getJSONObject(i); val table = e.getString("tableName")
                        db.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", table))
                        val indices = e.optJSONArray("indices")
                        if (indices != null) for (j in 0 until indices.length()) db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                    }
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = error("Sólo fixture v1")
            }).build())
        val old = helper.writableDatabase
        old.execSQL("INSERT INTO consent_text VALUES (?, ?)", arrayOf("demo-v1", environment.consentText))
        val ids = listOf(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
        val states = listOf("DRAFT", "READY", "NEEDS_CONTEXT")
        ids.forEachIndexed { i, id ->
            val oldEvent = UUID.randomUUID().toString()
            val values = ContentValues().apply {
                put("id", id.toString()); put("revision", 7L); put("state", states[i]); put("ownerId", actor.id.toString())
                put("siteId", site.toString()); put("eventId", oldEvent); put("deviceId", environment.deviceId.toString())
                put("capturedAt", now.toString()); put("eventName", "Evento histórico ficticio"); put("eventTimeZone", event.timeZone.id)
                put("fullName", "Nombre antiguo sin separar"); put("rut", input.rut); put("email", input.email); put("phone", input.phone)
                put("educationLevel", "SECONDARY"); put("establishment", "Colegio antiguo"); put("notes", "Conservar literalmente")
                put("consentVersion", "demo-v1"); put("acceptedAt", now.toString()); put("recordedBy", actor.id.toString()); put("acceptanceMode", "DIRECT")
                if (i == 1) { put("locationSource", "DEVICE"); put("latitude", -33.0); put("longitude", -71.0); put("measuredAt", now.toString()); put("accuracyMeters", 20.0) }
            }
            old.insert("captures", SQLiteDatabase.CONFLICT_ABORT, values)
            repeat(4) { old.execSQL("INSERT INTO capture_interests VALUES (?, ?, ?, ?, ?, ?)", arrayOf(id.toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(), "demo-old", "Programa anterior", "Modalidad anterior")) }
            if (i != 0) old.execSQL("INSERT INTO active_registration_keys VALUES (?, ?, ?)", arrayOf(oldEvent, input.rut, id.toString()))
            old.execSQL("INSERT INTO local_audit VALUES (?, ?, ?, ?, ?, ?)", arrayOf<Any>(UUID.randomUUID().toString(), id.toString(), actor.id.toString(), now.toString(), "CREATED", 7))
        }
        val tables = listOf("consent_text", "captures", "capture_interests", "active_registration_keys", "local_audit")
        val columns = tables.associateWith { table -> old.query("SELECT * FROM $table LIMIT 0").use { it.columnNames.toList() } }
        fun snapshot(db: SupportSQLiteDatabase, table: String): List<List<String?>> = db.query(
            "SELECT ${columns.getValue(table).joinToString(",") { "`$it`" }} FROM `$table` ORDER BY 1, 2"
        ).use { c -> buildList { while (c.moveToNext()) add((0 until c.columnCount).map { if (c.isNull(it)) null else c.getString(it) }) } }
        val before = tables.associateWith { snapshot(old, it) }
        helper.close(); password.fill(0)
        val migrated = open().openHelper.writableDatabase // Room también valida el esquema v2.
        for (table in tables) assertEquals("Conservación $table", before[table], snapshot(migrated, table))
        for (id in ids) {
            val row = open().captures().capture(id.toString())!!
            assertEquals("LEGACY_1", row.contractVersion); assertNull(row.givenNames); assertNull(row.birthDate); assertNull(row.currentActivity)
            assertTrue(open().captures().sites(id.toString()).isEmpty())
            assertTrue(open().captures().interests(id.toString()).all { it.priority == null && it.offeringId != null })
            val restored = repo().get(id) as ReadAdmissionResult.Legacy
            assertEquals(4, restored.capture.interests.size); assertEquals(7L, restored.capture.revision)
        }
        migrated.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
        assertTrue(repo().create(request) is CreateAdmissionResult.Saved)
        handle!!.close(); handle = null
        assertEquals(4, open().captures().count())
    }

    private val invalidStoredValues: List<Pair<String, String?>> = listOf(
        "capturedAt" to "invalid-date", "eventTimeZone" to "Invalid/Zone",
        "acceptedAt" to "invalid-date", "state" to "UNKNOWN", "ownerId" to "invalid-uuid",
        "acceptanceMode" to "UNKNOWN", "locationSource" to "UNKNOWN",
        "measuredAt" to "invalid-date", "measuredAt" to null, "latitude" to null,
        "locationSource" to "EVENT_CONFIRMED", // Falta evidencia de confirmacion del evento.
    )

    @Test fun malformedRowsReturnStorageUnavailableAndPreserveBothRecords() = runBlocking {
        val repository = repo()
        val broken = repository.create(request.copy(intent = SaveIntent.SAVE_DRAFT,
            location = CaptureLocation.Device(Coordinates(-33.0, -71.0), now, 10.0))) as CreateAdmissionResult.Saved
        val healthy = repository.create(request.copy(intent = SaveIntent.SAVE_DRAFT,
            location = CaptureLocation.Device(Coordinates(-33.0, -71.0), now, 10.0))) as CreateAdmissionResult.Saved
        val dao = open().captures()
        val original = dao.capture(broken.id.toString())!!
        for ((column, value) in invalidStoredValues) {
            val sql = open().openHelper.writableDatabase
            val oldValue = sql.query("SELECT `$column` FROM captures WHERE id = ?", arrayOf(broken.id.toString())).use {
                check(it.moveToFirst()); if (it.isNull(0)) null else it.getString(0)
            }
            sql.execSQL("UPDATE captures SET `$column` = ? WHERE id = ?", arrayOf(value, broken.id.toString()))
            val damaged = dao.capture(broken.id.toString())!!
            assertEquals(column, ReadAdmissionResult.StorageUnavailable, repository.get(broken.id))
            assertEquals(damaged, dao.capture(broken.id.toString()))
            assertTrue(repository.get(healthy.id) is ReadAdmissionResult.Found)
            assertEquals(2, dao.count())
            sql.execSQL("UPDATE captures SET `$column` = ? WHERE id = ?", arrayOf(oldValue, broken.id.toString()))
        }
        assertEquals(original, dao.capture(broken.id.toString()))
        handle!!.close(); handle = null
        assertTrue(repo().get(broken.id) is ReadAdmissionResult.Found)
        assertTrue(repo().get(healthy.id) is ReadAdmissionResult.Found)
    }

    @Test fun readDoesNotHideCoroutineCancellation() = runBlocking {
        val cancelled = object : AdmissionContextSource {
            override suspend fun currentActor(): LocalActor? = throw kotlinx.coroutines.CancellationException("test cancellation")
            override suspend fun environment(eventId: UUID): AdmissionEnvironment? = null
        }
        try {
            RoomAdmissionRepository(open(), cancelled).get(UUID.randomUUID())
            fail("La cancelacion debe propagarse")
        } catch (_: kotlinx.coroutines.CancellationException) { }
    }
}
