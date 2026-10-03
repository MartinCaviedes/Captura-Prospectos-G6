package io.github.grupo6.capturaprospectos.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class DatabaseKeyUnavailable : Exception("No se puede acceder a la clave local; se conserva el almacenamiento.")

/** Un archivo cifrado por base; no se genera una clave nueva si existe estado anterior. */
internal class DatabaseKeyStore(private val context: Context, private val databaseName: String) {
    init { require(Regex("[a-zA-Z0-9_-]+\\.db").matches(databaseName)) }
    private val alias = "g6.database.$databaseName"
    private val file = AtomicFile(File(context.noBackupFilesDir, "$databaseName.wrapped-key"))

    fun getOrCreate(): ByteArray = synchronized(lock) {
        try {
            val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val exists = file.baseFile.exists() || File(file.baseFile.path + ".bak").exists()
            if (exists) {
                val key = store.getKey(alias, null) as? SecretKey ?: throw DatabaseKeyUnavailable()
                val bytes = file.readFully()
                if (bytes.size != 61 || bytes[0] != 1.toByte()) throw DatabaseKeyUnavailable()
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, bytes.copyOfRange(1, 13)))
                cipher.updateAAD(databaseName.toByteArray(Charsets.UTF_8))
                return@synchronized cipher.doFinal(bytes.copyOfRange(13, bytes.size))
            }
            if (context.getDatabasePath(databaseName).exists() || store.containsAlias(alias)) throw DatabaseKeyUnavailable()
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            generator.init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build())
            val key = generator.generateKey()
            val password = ByteArray(32).also { SecureRandom().nextBytes(it) }
            try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, key)
                cipher.updateAAD(databaseName.toByteArray(Charsets.UTF_8))
                check(cipher.iv.size == 12)
                val blob = byteArrayOf(1) + cipher.iv + cipher.doFinal(password)
                val output = file.startWrite()
                try { output.write(blob); file.finishWrite(output) }
                catch (error: Exception) { file.failWrite(output); throw error }
                password
            } catch (error: Exception) { password.fill(0); throw error }
        } catch (error: Exception) {
            // No reset, borrado, logging de material criptográfico ni excepción con secretos.
            throw DatabaseKeyUnavailable()
        }
    }
    private companion object { val lock = Any() }
}
