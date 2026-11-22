package io.github.grupo6.capturaprospectos.ui.access

import androidx.test.platform.app.InstrumentationRegistry
import io.github.grupo6.capturaprospectos.data.remote.TokenResponseDto
import io.github.grupo6.capturaprospectos.data.session.*
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class RememberedSessionStoreTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private val suffix="test-${UUID.randomUUID()}"
    private val store=AndroidRememberedSessionStore(context,suffix)
    private val file=File(context.noBackupFilesDir,"remembered-session-$suffix.bin")
    private fun session()=RememberedSession(TokenResponseDto("TOKEN-FICTICIO-NO-VALIDO",expiresIn=14400,
        userId="11111111-1111-4111-8111-111111111111",username="ficticio",email="prueba@example.invalid",
        displayName="Persona ficticia",rol="CAPTADOR",roles=listOf("CAPTADOR"),siteIds=listOf("site-1")),
        AndroidOfflineClock(context).elapsedMillis(),AndroidOfflineClock(context).bootId(),"http://127.0.0.1:8080/")
    @After fun cleanup(){store.clear()}
    @Test fun encryptedTokenSurvivesStoreRecreationWithoutPlainCredentials()=runBlocking {
        val value=session();assertTrue(store.write(value,store.revision()))
        val bytes=file.readBytes().toString(Charsets.ISO_8859_1)
        assertFalse(bytes.contains(value.token.accessToken));assertFalse(bytes.contains(value.token.email))
        assertEquals(value,AndroidRememberedSessionStore(context,suffix).read())
        assertFalse(value.toString().contains(value.token.accessToken))
    }
    @Test fun tamperingCannotRestoreToken()=runBlocking {
        assertTrue(store.write(session(),store.revision()))
        val bytes=file.readBytes();bytes[bytes.lastIndex]=(bytes.last().toInt() xor 1).toByte();file.writeBytes(bytes)
        assertNull(store.read());assertFalse(file.exists())
    }
    @Test fun logoutAcrossStoreInstancesRejectsLateWriteAndOldCiphertext()=runBlocking {
        val value=session();val revision=store.revision();assertTrue(store.write(value,revision))
        val bytes=file.readBytes();AndroidRememberedSessionStore(context,suffix).clear()
        assertFalse(store.write(value,revision));assertNull(store.read())
        file.writeBytes(bytes);assertNull(AndroidRememberedSessionStore(context,suffix).read())
    }
}
