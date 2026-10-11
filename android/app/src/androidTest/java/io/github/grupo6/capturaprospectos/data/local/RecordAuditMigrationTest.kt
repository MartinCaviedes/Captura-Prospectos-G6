package io.github.grupo6.capturaprospectos.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.platform.app.InstrumentationRegistry
import io.github.grupo6.capturaprospectos.domain.admission.*
import io.github.grupo6.capturaprospectos.domain.capture.*
import java.io.File
import java.security.KeyStore
import java.time.*
import java.util.UUID
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*

class RecordAuditMigrationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "test-records-migration-${UUID.randomUUID()}.db"
    private var handle: EncryptedDatabase? = null
    @After fun cleanup() {
        handle?.close(); check(name.startsWith("test-records-migration-")); context.deleteDatabase(name)
        listOf("", ".bak", ".new").forEach { File(context.noBackupFilesDir, "$name.wrapped-key$it").delete() }
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry("g6.database.$name") }
    }
    private fun rows(sql: SupportSQLiteDatabase, table: String, columns: List<String>? = null): List<Map<String, String?>> =
        sql.query("SELECT ${columns?.joinToString(",") ?: "*"} FROM $table ORDER BY 1").use { cursor ->
            buildList { while (cursor.moveToNext()) add((0 until cursor.columnCount).associate {
                cursor.getColumnName(it) to if (cursor.isNull(it)) null else cursor.getString(it)
            }) }
        }

    @Test fun encryptedV3UpgradePreservesEveryOldValueAndAddsUnknownAuditAttributesAsNull() {
        val schema = JSONObject(InstrumentationRegistry.getInstrumentation().context.assets.open(
            "io.github.grupo6.capturaprospectos.data.local.CaptureDatabase/3.json").bufferedReader().use { it.readText() }).getJSONObject("database")
        System.loadLibrary("sqlcipher")
        val password = DatabaseKeyStore(context, name).getOrCreate()
        val helper = SupportOpenHelperFactory(password).create(SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                override fun onConfigure(db: SupportSQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
                override fun onCreate(db: SupportSQLiteDatabase) {
                    val entities = schema.getJSONArray("entities")
                    for (i in 0 until entities.length()) {
                        val e = entities.getJSONObject(i); val table = e.getString("tableName")
                        db.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", table))
                        val indices = e.optJSONArray("indices")
                        if (indices != null) for (j in 0 until indices.length())
                            db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                    }
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = error("Fixture v3")
            }).build())
        val old = helper.writableDatabase
        val captureFields = (0 until schema.getJSONArray("entities").length()).map { schema.getJSONArray("entities").getJSONObject(it) }
            .single { it.getString("tableName") == "captures" }.getJSONArray("fields")
        val captureColumns = (0 until captureFields.length()).map { captureFields.getJSONObject(it).getString("columnName") }.toSet()
        val actor = LocalActor(UUID(0, 1), LocalRole.CAPTOR, setOf(UUID(0, 2)))
        val now = Instant.parse("2026-10-10T15:00:00Z")
        val program = ProgramOption(UUID(0, 5), "Programa ficticio", "v1", "DEMO")
        old.execSQL("INSERT INTO consent_text VALUES ('v1', 'Consentimiento ficticio')")
        fun insertCapture(row: CaptureRow) {
            val values = ContentValues()
            // Fixture del esquema conservado: sólo columnas físicas de CaptureRow, sin inferir campos.
            for (field in CaptureRow::class.java.declaredFields.filter { it.name in captureColumns }) {
                field.isAccessible = true
                when (val value = field.get(row)) {
                    null -> values.putNull(field.name)
                    is String -> values.put(field.name, value)
                    is Long -> values.put(field.name, value)
                    is Double -> values.put(field.name, value)
                    else -> error("Tipo inesperado del fixture")
                }
            }
            old.insert("captures", SQLiteDatabase.CONFLICT_ABORT, values)
        }
        val input = AdmissionInput(givenNames = "Persona Ficticia", familyNames = "Demostracion", rut = "12345678-5",
            email = "demo@example.invalid", phone = "+56900000000", birthDate = "2000-01-01", residenceCommune = "Comuna ficticia",
            currentActivity = CurrentActivity.SECONDARY_GRADUATE, interests = listOf(RankedInterest(program.id, 1)))
        for ((i, state) in listOf(CaptureState.READY, CaptureState.NEEDS_CONTEXT, CaptureState.DRAFT, CaptureState.CANCELLED).withIndex()) {
            val event = CaptureEvent(UUID(0, 10L + i), UUID(0, 2), "Evento ficticio $i", now, now.plusSeconds(3600), ZoneOffset.UTC, setOf(actor.id), null)
            val env = CaptureEnvironment(actor, event, UUID(0, 99), emptyList(), "v1", "Consentimiento ficticio")
            val prepared = PrepareAdmission().prepare(input,
                if (state == CaptureState.DRAFT) SaveIntent.SAVE_DRAFT else SaveIntent.CONFIRM_REGISTRATION,
                ConsentEvidence("v1", now, actor.id, AcceptanceMode.DIRECT), "v1", actor.id, listOf(program), emptyList(),
                if (state == CaptureState.READY) CaptureLocation.Device(Coordinates(-33.0, -71.0), now, 20.0) else null,
                null, now, ZoneOffset.UTC) as AdmissionPreparation.Prepared
            val row = prepared.toAdmissionRow(UUID(0, 100L + i), env, now).copy(state = state.name,
                revision = if (state == CaptureState.CANCELLED) 2 else 1)
            insertCapture(row)
            old.execSQL("INSERT INTO capture_interests (captureId, programId, catalogVersion, programName, priority, section) VALUES (?, ?, ?, ?, ?, ?)",
                arrayOf<Any>(row.id, program.id.toString(), program.catalogVersion, program.name, 1, program.section))
            if (state == CaptureState.READY || state == CaptureState.NEEDS_CONTEXT)
                old.execSQL("INSERT INTO active_registration_keys VALUES (?, ?, ?)", arrayOf(row.eventId, row.rut, row.id))
            old.execSQL("INSERT INTO local_audit VALUES (?, ?, ?, ?, ?, ?)", arrayOf<Any>(UUID.randomUUID().toString(), row.id,
                actor.id.toString(), now.toString(), if (state == CaptureState.CANCELLED) "CANCELLED" else "CREATED", row.revision))
        }
        val tables = listOf("captures", "capture_interests", "capture_site_preferences", "consent_text", "active_registration_keys", "local_audit")
        val before = tables.associateWith { rows(old, it) }
        val indices = rows(old, "sqlite_master", listOf("type", "name", "tbl_name", "sql")).filter { it["type"] == "index" && !it["name"]!!.startsWith("sqlite_autoindex") }
        helper.close(); password.fill(0)
        handle = EncryptedDatabase.open(context, name)
        val migrated = handle!!.database.openHelper.writableDatabase
        for (table in tables) {
            val columns = before.getValue(table).firstOrNull()?.keys?.toList()
            assertEquals(table, before.getValue(table), rows(migrated, table, columns))
        }
        assertTrue(rows(migrated, "local_audit").all { it["previousRevision"] == null && it["reasonText"] == null && it["changedFields"] == null })
        assertEquals(indices, rows(migrated, "sqlite_master", listOf("type", "name", "tbl_name", "sql")).filter { it["type"] == "index" && !it["name"]!!.startsWith("sqlite_autoindex") })
        migrated.query("PRAGMA user_version").use { assertTrue(it.moveToFirst()); assertEquals(4, it.getInt(0)) }
        migrated.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
        assertFalse(context.getDatabasePath(name).readBytes().take(16).toByteArray().toString(Charsets.US_ASCII).startsWith("SQLite format 3"))
    }
}
