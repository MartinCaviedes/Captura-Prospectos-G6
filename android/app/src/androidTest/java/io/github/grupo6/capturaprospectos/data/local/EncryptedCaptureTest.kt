package io.github.grupo6.capturaprospectos.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.withTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptedCaptureTest {
    private lateinit var context: Context
    private lateinit var name: String
    private lateinit var handle: EncryptedDatabase
    private val now = Instant.parse("2026-10-19T15:00:00Z")
    private val owner = UUID.randomUUID()
    private val site = UUID.randomUUID()
    private val actor = LocalActor(owner, LocalRole.CAPTOR, setOf(site))
    private val event = CaptureEvent(UUID.randomUUID(), site, "Evento ficticio", now.minusSeconds(60), now.plusSeconds(3600), ZoneId.of("America/Santiago"), setOf(owner), null)
    private val offering = Offering(UUID.randomUUID(), UUID.randomUUID(), site, "demo-v1", "Programa ficticio", true)
    private val environment = CaptureEnvironment(actor, event, UUID.randomUUID(), listOf(offering), "demo-v1", "Texto ficticio para pruebas")
    private var current = actor
    private var supplied = environment
    private val source = object : CaptureContextSource {
        override suspend fun currentActor() = current
        override suspend fun environment(eventId: UUID) = supplied.takeIf { it.event.id == eventId }
    }
    private val request = NewCapture(event.id,
        CaptureInput("Persona Ficticia Alfa", "12345678-5", "alfa@example.invalid", "+56900000000", establishment = "Establecimiento ficticio"),
        SaveIntent.CONFIRM_REGISTRATION, ConsentEvidence("demo-v1", now, owner, AcceptanceMode.DIRECT), listOf(offering.id), null)
    private fun repository() = RoomCaptureRepository(handle.database, source, PrepareCapture(ContactValidator(setOf("SECONDARY"))), Clock.fixed(now, ZoneOffset.UTC))

    @Before fun openDatabase() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        name = "test-${UUID.randomUUID()}.db"
        handle = EncryptedDatabase.open(context, name)
    }
    @After fun cleanupTestDatabaseOnly() {
        if (!::name.isInitialized) return
        if (::handle.isInitialized) handle.close()
        check(name.startsWith("test-"))
        context.deleteDatabase(name)
        listOf("", ".bak", ".new").forEach { File(context.noBackupFilesDir, "$name.wrapped-key$it").delete() }
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry("g6.database.$name") }
    }

    @Test fun savedCaptureSurvivesCloseAndReopen() = runBlocking {
        val saved = repository().create(request) as CreateCaptureResult.Saved
        handle.close()
        handle = EncryptedDatabase.open(context, name)
        val restored = repository().get(saved.id) as ReadCaptureResult.Found
        assertEquals(request.input.fullName, restored.capture.input.fullName)
        assertEquals(now, restored.capture.capturedAt)
        assertEquals(CaptureState.NEEDS_CONTEXT, restored.capture.state)
        assertEquals(1, restored.capture.interests.size)
    }

    @Test fun ordinarySqliteCannotReadEncryptedDatabase() = runBlocking {
        repository().create(request)
        handle.close()
        var readable = false
        try {
            SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READONLY) { _ -> /* No borrar la base cifrada al rechazarla como SQLite sin clave. */ }.use { plain ->
                plain.rawQuery("SELECT COUNT(*) FROM captures", null).use { readable = it.moveToFirst() }
            }
        } catch (_: android.database.sqlite.SQLiteException) { /* Rechazo esperado sin clave. */ }
        assertFalse(readable)
        handle = EncryptedDatabase.open(context, name)
        assertEquals(1, handle.database.captures().count())
    }

    @Test fun missingKeystoreKeyNeverResetsDatabase() = runBlocking {
        repository().create(request)
        handle.close()
        val original = context.getDatabasePath(name).readBytes()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry("g6.database.$name") }
        try { EncryptedDatabase.open(context, name).close(); fail("Debió fallar por clave inaccesible") }
        catch (_: DatabaseKeyUnavailable) { /* No crear otra clave. */ }
        assertArrayEquals(original, context.getDatabasePath(name).readBytes())
    }

    @Test fun consentAndInvalidInputProduceNoRows() = runBlocking {
        assertEquals(CreateCaptureResult.ConsentRequired, repository().create(request.copy(consent = null)))
        assertTrue(repository().create(request.copy(input = request.input.copy(email = "incorrecto"))) is CreateCaptureResult.Invalid)
        assertEquals(0, handle.database.captures().count())
    }

    @Test fun duplicateOwnAndForeignAreDistinguishedWithoutLeakingForeignId() = runBlocking {
        val saved = repository().create(request) as CreateCaptureResult.Saved
        assertEquals(CreateCaptureResult.DuplicateOwn(saved.id), repository().create(request))
        current = actor.copy(id = UUID.randomUUID())
        supplied = environment.copy(actor = current, event = event.copy(assignedCaptorIds = setOf(current.id)))
        assertEquals(CreateCaptureResult.DuplicateRestricted, repository().create(request.copy(consent = request.consent!!.copy(recordedBy = current.id))))
        assertEquals(ReadCaptureResult.Unavailable, repository().get(saved.id))
        assertEquals(1, handle.database.captures().count())
    }

    @Test fun concurrentSavesProduceOnlyOneActiveRegistration() = runBlocking {
        val repo = repository()
        val results = listOf(async { repo.create(request) }, async { repo.create(request) }).awaitAll()
        assertEquals(1, results.count { it is CreateCaptureResult.Saved })
        assertEquals(1, results.count { it is CreateCaptureResult.DuplicateOwn })
        assertEquals(1, handle.database.captures().count())
    }

    @Test fun draftsDoNotReserveRegistrationKeyAndArePrivate() = runBlocking {
        val draft = repository().create(request.copy(intent = SaveIntent.SAVE_DRAFT)) as CreateCaptureResult.Saved
        assertTrue(repository().create(request) is CreateCaptureResult.Saved)
        current = actor.copy(id = UUID.randomUUID(), role = LocalRole.SUPERVISOR)
        assertEquals(ReadCaptureResult.Unavailable, repository().get(draft.id))
    }

    @Test fun constraintFailureRollsBackWholeTransaction() = runBlocking {
        val saved = repository().create(request) as CreateCaptureResult.Saved
        try {
            handle.database.withTransaction {
                handle.database.captures().insertConsent(ConsentTextRow("rollback-test", "Texto ficticio"))
                handle.database.captures().insertKey(ActiveKeyRow(event.id.toString(), request.input.rut, saved.id.toString()))
            }
            fail("La clave activa duplicada debe fallar")
        } catch (_: android.database.sqlite.SQLiteConstraintException) { /* ABORT + rollback. */ }
        assertNull(handle.database.captures().consent("rollback-test"))
        assertEquals(1, handle.database.captures().count())
    }
}
