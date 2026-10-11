package io.github.grupo6.capturaprospectos.data.local

import android.content.Context
import androidx.room.Room
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/** Crear/abrir fuera del hilo principal; cerrar este dueño al finalizar su uso. */
internal class EncryptedDatabase private constructor(val database: CaptureDatabase, private val password: ByteArray) : AutoCloseable {
    override fun close() { try { database.close() } finally { password.fill(0) } }
    companion object {
        fun open(context: Context, name: String = "captures-demo.db"): EncryptedDatabase {
            val appContext = context.applicationContext
            val password = DatabaseKeyStore(appContext, name).getOrCreate()
            var database: CaptureDatabase? = null
            try {
                System.loadLibrary("sqlcipher")
                database = Room.databaseBuilder(appContext, CaptureDatabase::class.java, name)
                    .openHelperFactory(SupportOpenHelperFactory(password))
                    .addMigrations(CaptureMigration1To2, CaptureMigration2To3, CaptureMigration3To4)
                    .build()
                // Fuerza apertura ahora: no devolver éxito cuando aún no se comprobó la clave.
                database.openHelper.writableDatabase
                return EncryptedDatabase(database, password)
            } catch (error: Throwable) {
                try { database?.close() } finally { password.fill(0) }
                throw error
            }
        }
    }
}
