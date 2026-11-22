package io.github.grupo6.capturaprospectos.data.session

import android.content.Context
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

/** Archivo privado sin backup. AES-GCM con clave no exportable en Android Keystore. */
class AndroidRememberedSessionStore(context: Context, suffix: String = "default") : RememberedSessionStore {
    private val file = AtomicFile(File(context.noBackupFilesDir, "remembered-session-$suffix.bin"))
    private val alias = "g6.remembered.session.$suffix"
    private class Gate(var generation: Long = 0)
    private val gate = synchronized(gates) { gates.getOrPut(file.baseFile.absolutePath) { Gate() } }
    private var generation: Long
        get() = gate.generation
        set(value) { gate.generation = value }
    override fun revision() = synchronized(gate) { generation }
    private fun key(): SecretKey {
        val keys = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keys.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    override suspend fun read(): RememberedSession? = withContext(Dispatchers.IO) {
        synchronized(gate) {
            if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) return@synchronized null
            try {
                require(file.baseFile.length() <= 32768)
                val bytes = file.readFully(); require(bytes.size in 29..32768)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
                cipher.updateAAD(alias.toByteArray(Charsets.UTF_8))
                Gson().fromJson(String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8), RememberedSession::class.java)
            } catch (_: Exception) { generation++; file.delete(); null }
        }
    }
    override suspend fun write(session: RememberedSession, expectedRevision: Long): Boolean = withContext(Dispatchers.IO) {
        synchronized(gate) {
            if (generation != expectedRevision) return@synchronized false
            try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, key()); cipher.updateAAD(alias.toByteArray(Charsets.UTF_8))
                require(cipher.iv.size == 12)
                val bytes = cipher.iv + cipher.doFinal(Gson().toJson(session).toByteArray(Charsets.UTF_8))
                require(bytes.size <= 32768)
                val stream = file.startWrite()
                try { stream.write(bytes); file.finishWrite(stream) }
                catch (error: Exception) { file.failWrite(stream); throw error }
                true
            } catch (_: Exception) { false }
        }
    }
    override fun clear() = synchronized(gate) {
        generation++; file.delete()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(alias) }
        Unit
    }
    companion object { private val gates = mutableMapOf<String, Gate>() }
}
