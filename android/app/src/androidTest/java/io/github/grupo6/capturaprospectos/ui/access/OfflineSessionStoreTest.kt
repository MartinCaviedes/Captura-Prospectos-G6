package io.github.grupo6.capturaprospectos.ui.access

import androidx.test.platform.app.InstrumentationRegistry
import io.github.grupo6.capturaprospectos.data.remote.UserSummaryDto
import io.github.grupo6.capturaprospectos.data.session.*
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class OfflineSessionStoreTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private val suffix="test-${UUID.randomUUID()}"
    private val store=AndroidOfflineSessionStore(context,suffix)
    private val file=File(context.noBackupFilesDir,"offline-session-$suffix.bin")
    private fun session()=OfflineSession(UserSummaryDto("11111111-1111-4111-8111-111111111111","ficticio",
        "prueba@example.invalid","Persona ficticia","ACTIVE","CAPTADOR",listOf("site-1"),1,"2026-11-22","2026-11-22"),
        AndroidOfflineClock(context).elapsedMillis(),AndroidOfflineClock(context).bootId())
    @After fun cleanup() { store.clear() }
    @Test fun encryptedFileSurvivesStoreRecreationWithoutPlainIdentity()=runBlocking {
        val value=session();assertTrue(store.write(value,store.revision()))
        val bytes=file.readBytes().toString(Charsets.ISO_8859_1)
        assertFalse(bytes.contains("prueba@example.invalid"));assertFalse(bytes.contains("CAPTADOR"))
        assertEquals(value,AndroidOfflineSessionStore(context,suffix).read())
        assertTrue(value.remaining(AndroidOfflineClock(context))>0)
    }
    @Test fun tamperedCiphertextFailsClosed()=runBlocking {
        assertTrue(store.write(session(),store.revision()))
        val bytes=file.readBytes();bytes[bytes.lastIndex]=(bytes.last().toInt() xor 1).toByte();file.writeBytes(bytes)
        assertNull(store.read());assertFalse(file.exists())
    }
    @Test fun clearRejectsStaleWriteAndOldCiphertextReplay()=runBlocking {
        val value=session();val revision=store.revision();assertTrue(store.write(value,revision))
        val oldBytes=file.readBytes();store.clear()
        assertFalse(store.write(value,revision));assertNull(store.read())
        file.writeBytes(oldBytes);assertNull(AndroidOfflineSessionStore(context,suffix).read())
    }
}
