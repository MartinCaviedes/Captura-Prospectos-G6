package io.github.grupo6.capturaprospectos.data.session

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import com.google.gson.Gson
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidOfflineClock(private val context: Context) : OfflineClock {
    override fun elapsedMillis() = SystemClock.elapsedRealtime()
    override fun bootId() = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
}

/** Archivo sin backup, autenticado con AES-GCM. No contiene contraseñas, hashes ni JWT. */
class AndroidOfflineSessionStore(context: Context, suffix: String = "default") : OfflineSessionStore {
    private val file = AtomicFile(File(context.noBackupFilesDir, "offline-session-$suffix.bin"))
    private val alias = "g6.offline.session.$suffix"
    private val gate = Any()
    private var generation = 0L
    override fun revision() = synchronized(gate) { generation }
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    override suspend fun read(): OfflineSession? = withContext(Dispatchers.IO) {
        synchronized(gate) {
            if (!file.baseFile.exists()) return@synchronized null
            try {
                require(file.baseFile.length() in 29..16384)
                val bytes = file.readFully()
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
                cipher.updateAAD(alias.toByteArray(Charsets.UTF_8))
                Gson().fromJson(String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8), OfflineSession::class.java)
            } catch (_: Exception) { generation++; file.delete(); null }
        }
    }
    override suspend fun write(session: OfflineSession, expectedRevision: Long): Boolean = withContext(Dispatchers.IO) {
        synchronized(gate) {
            if (generation != expectedRevision) return@synchronized false
            try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, key()); cipher.updateAAD(alias.toByteArray(Charsets.UTF_8))
                require(cipher.iv.size == 12)
                val bytes = cipher.iv + cipher.doFinal(Gson().toJson(session).toByteArray(Charsets.UTF_8))
                require(bytes.size <= 16384)
                val stream = file.startWrite()
                try { stream.write(bytes); file.finishWrite(stream) }
                catch (error: Exception) { file.failWrite(stream); throw error }
                true
            } catch (_: Exception) { false }
        }
    }
    override fun clear() = synchronized(gate) {
        generation++; file.delete()
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        store.deleteEntry(alias) // Un archivo antiguo tampoco debe poder descifrarse tras logout.
    }
}
