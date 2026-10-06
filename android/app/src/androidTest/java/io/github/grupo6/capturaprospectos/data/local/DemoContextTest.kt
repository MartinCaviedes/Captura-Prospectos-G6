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
import io.github.grupo6.capturaprospectos.domain.context.*
import java.io.File
import java.security.KeyStore
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DemoContextTest {
    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var name: String
    private var handle: EncryptedDatabase? = null
    private val now = Instant.parse("2026-10-06T15:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private fun id(key: String) = UUID.fromString(DemoSeed.id(key))
    private fun db(): CaptureDatabase {
        if (handle == null) handle = EncryptedDatabase.open(context, name)
        return handle!!.database
    }
    private fun repository() = RoomDemoContextRepository(db(), clock)
    private suspend fun ready() = (repository().snapshot() as DemoContextState.Ready).snapshot
    private suspend fun selectCaptor() {
        assertEquals(ContextChange.APPLIED, repository().selectProfile(id("captor-a")))
        assertEquals(ContextChange.APPLIED, repository().selectEvent(id("event-a")))
    }
    @Before fun setup() { name = "test-context-${UUID.randomUUID()}.db" }
    @After fun cleanupOnlyTestData() {
        handle?.close()
        if (!::name.isInitialized) return
        check(name.startsWith("test-context-")); context.deleteDatabase(name)
        listOf("", ".bak", ".new").forEach { File(context.noBackupFilesDir, "$name.wrapped-key$it").delete() }
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry("g6.database.$name") }
    }

    @Test fun initializesOnceWithoutSelectingProfileEventOrCreatingCaptures() = runBlocking {
        assertEquals(DemoContextState.NotInitialized, repository().snapshot())
        assertEquals(ContextChange.NOT_INITIALIZED, repository().selectProfile(id("captor-a")))
        val repo = repository()
        val results = listOf(async { repo.initialize() }, async { repo.initialize() }).awaitAll()
        assertEquals(setOf(ContextChange.APPLIED, ContextChange.ALREADY_INITIALIZED), results.toSet())
        val initial = ready()
        assertEquals(3, initial.profiles.size); assertNull(initial.currentProfile); assertNull(initial.selectedEvent)
        assertTrue(initial.availableEvents.isEmpty()); assertEquals("SYNTHETIC", initial.catalog.sourceKind)
        assertEquals(4, initial.catalog.programs.size); assertEquals(4, initial.catalog.sites.size)
        assertTrue(initial.consentText.contains("ficticio")); assertEquals(0, db().captures().count())
        assertEquals(ContextChange.ALREADY_INITIALIZED, repository().initialize())
        assertEquals(initial.deviceId, ready().deviceId)
    }

    @Test fun selectionAndInstallationIdSurviveReopen() = runBlocking {
        repository().initialize(); selectCaptor()
        val previous = ready()
        assertEquals(setOf(id("event-a"), id("event-a2")), previous.availableEvents.map { it.id }.toSet())
        assertEquals("Colegio ficticio A", previous.establishmentSuggestion!!.name)
        assertNull(previous.selectedEvent!!.location)
        handle!!.close(); handle = null
        assertEquals(previous, ready())
        assertEquals(ContextChange.ALREADY_INITIALIZED, repository().initialize())
        assertEquals(previous, ready())
    }

    @Test fun rejectsForeignEventAndProfileSwitchClearsSelection() = runBlocking {
        repository().initialize(); selectCaptor()
        assertEquals(ContextChange.INVALID_SELECTION, repository().selectEvent(id("event-b")))
        assertEquals(id("event-a"), ready().selectedEvent!!.id)
        assertNull(repository().environment(id("event-a2"))) // Asignado pero no seleccionado.
        assertEquals(ContextChange.INVALID_SELECTION, repository().selectProfile(UUID.randomUUID()))
        repository().selectProfile(id("captor-b"))
        assertNull(ready().selectedEvent); assertNull(ready().establishmentSuggestion)
        assertNull(repository().environment(id("event-a")))
        assertEquals(ContextChange.INVALID_SELECTION, repository().selectEvent(id("event-a")))
        repository().selectProfile(id("supervisor"))
        assertEquals(LocalRole.SUPERVISOR, repository().currentActor()!!.role)
        assertTrue(ready().availableEvents.isEmpty()) // El supervisor no crea capturas atribuidas a otro.
    }

    @Test fun realContextSourceSavesAdmissionAndLogoutPreservesIt() = runBlocking {
        repository().initialize(); selectCaptor()
        val env = repository().environment(id("event-a"))!!
        val input = AdmissionInput(givenNames = "Persona Ficticia", familyNames = "Demostracion", rut = "12345678-5",
            email = "demo@example.invalid", phone = "+56900000000", birthDate = "2000-01-01", residenceCommune = "Comuna ficticia",
            currentActivity = CurrentActivity.SECONDARY_GRADUATE,
            interests = listOf(RankedInterest(id("program-2"), 1)), preferredSiteIds = env.sites.map { it.id })
        val request = NewAdmission(id("event-a"), input, SaveIntent.CONFIRM_REGISTRATION,
            ConsentEvidence(env.context.consentVersion, now, id("captor-a"), AcceptanceMode.DIRECT), null)
        val repo = RoomAdmissionRepository(db(), repository(), clock = clock)
        val saved = repo.create(request) as CreateAdmissionResult.Saved
        assertEquals(CaptureState.NEEDS_CONTEXT, saved.state)
        assertNull((repo.get(saved.id) as ReadAdmissionResult.Found).capture.input.establishment) // Sugerencia no aplicada.
        repository().clearSelection()
        assertEquals(ReadAdmissionResult.Unavailable, repo.get(saved.id)); assertEquals(1, db().captures().count())
        assertEquals(CreateAdmissionResult.AccessDenied, repo.create(request))
        handle!!.close(); handle = null
        assertNull(ready().currentProfile)
        selectCaptor()
        val restored = (RoomAdmissionRepository(db(), repository(), clock = clock).get(saved.id) as ReadAdmissionResult.Found).capture
        assertEquals(input.preferredSiteIds.toSet(), restored.input.preferredSiteIds.toSet())
        assertEquals(env.context.deviceId, restored.deviceId)
    }

    @Test fun conflictingConsentNeverOverwritesOrPartiallySeeds() = runBlocking {
        db().captures().insertConsent(ConsentTextRow(DemoSeed.CONSENT, "Otro texto ficticio"))
        assertEquals(ContextChange.INCOMPATIBLE_SEED, repository().initialize())
        assertEquals(0, db().context().existingRows()); assertNull(db().context().state())
        assertEquals("Otro texto ficticio", db().captures().consent(DemoSeed.CONSENT)!!.text)
    }

    @Test fun initializationFailureRollsBackAndCanRetry() = runBlocking {
        val sql = db().openHelper.writableDatabase
        sql.execSQL("CREATE TEMP TRIGGER fail_context_test BEFORE INSERT ON demo_context BEGIN SELECT RAISE(ABORT, 'test rollback'); END")
        assertEquals(ContextChange.STORAGE_UNAVAILABLE, repository().initialize())
        assertEquals(0, db().context().existingRows()); assertNull(db().captures().consent(DemoSeed.CONSENT))
        sql.execSQL("DROP TRIGGER fail_context_test")
        assertEquals(ContextChange.APPLIED, repository().initialize())
    }

    @Test fun observationEmitsPersistedSelection() = runBlocking {
        val repo = repository()
        assertEquals(DemoContextState.NotInitialized, withTimeout(5000) { repo.observe().first() })
        repo.initialize(); repo.selectProfile(id("captor-a"))
        val selected = withTimeout(5000) { repo.observe().first() } as DemoContextState.Ready
        assertEquals(id("captor-a"), selected.snapshot.currentProfile!!.actor.id)
    }

    @Test fun v2UpgradePreservesCaptureAndRequiresExplicitSeed() = runBlocking {
        val schema = JSONObject(InstrumentationRegistry.getInstrumentation().context.assets.open(
            "io.github.grupo6.capturaprospectos.data.local.CaptureDatabase/2.json").bufferedReader().use { it.readText() }).getJSONObject("database")
        System.loadLibrary("sqlcipher")
        val password = DatabaseKeyStore(context, name).getOrCreate()
        val helper = SupportOpenHelperFactory(password).create(SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(2) {
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
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = error("Sólo fixture v2")
            }).build())
        val old = helper.writableDatabase
        old.execSQL("INSERT INTO consent_text VALUES ('old-demo', 'Texto anterior ficticio')")
        val fields = (0 until schema.getJSONArray("entities").length()).map { schema.getJSONArray("entities").getJSONObject(it) }
            .single { it.getString("tableName") == "captures" }.getJSONArray("fields")
        val values = ContentValues()
        for (i in 0 until fields.length()) {
            val f = fields.getJSONObject(i)
            if (f.optBoolean("notNull")) {
                if (f.getString("affinity") == "INTEGER") values.put(f.getString("columnName"), 1L)
                else values.put(f.getString("columnName"), "dato ficticio anterior")
            }
        }
        values.put("id", UUID.randomUUID().toString()); values.put("consentVersion", "old-demo"); values.put("contractVersion", "LEGACY_1")
        old.insert("captures", SQLiteDatabase.CONFLICT_ABORT, values)
        fun originalRow(db: SupportSQLiteDatabase) = db.query("SELECT * FROM captures").use { c ->
            assertTrue(c.moveToFirst()); (0 until c.columnCount).associate { c.getColumnName(it) to if (c.isNull(it)) null else c.getString(it) }
        }
        val before = originalRow(old)
        helper.close(); password.fill(0)
        assertEquals(before, originalRow(db().openHelper.writableDatabase))
        assertEquals(DemoContextState.NotInitialized, repository().snapshot())
        assertEquals(ContextChange.APPLIED, repository().initialize())
        assertEquals(before, originalRow(db().openHelper.writableDatabase))
        db().openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
    }
}
