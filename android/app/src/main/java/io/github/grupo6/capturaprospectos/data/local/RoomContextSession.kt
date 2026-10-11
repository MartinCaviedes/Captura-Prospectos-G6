package io.github.grupo6.capturaprospectos.data.local

import android.content.Context
import android.database.sqlite.SQLiteException
import io.github.grupo6.capturaprospectos.domain.context.ContextOpenFailure
import io.github.grupo6.capturaprospectos.domain.context.ContextSession
import kotlinx.coroutines.CancellationException
import java.io.IOException

/** Invocar fuera de Main. Fallar no autoriza borrar, resembrar o reemplazar claves. */
internal fun openContextSession(context: Context): ContextSession {
    val handle = try { EncryptedDatabase.open(context.applicationContext) }
    catch (cancelled: CancellationException) { throw cancelled }
    catch (_: DatabaseKeyUnavailable) { throw ContextOpenFailure() }
    catch (_: SQLiteException) { throw ContextOpenFailure() }
    catch (_: IOException) { throw ContextOpenFailure() }
    catch (_: SecurityException) { throw ContextOpenFailure() }
    catch (_: IllegalStateException) { throw ContextOpenFailure() } // Apertura/esquema incompatible, sin reset.
    val demoRepo = RoomDemoContextRepository(handle.database)
    val admissionRepo = RoomAdmissionRepository(handle.database, demoRepo)
    val recordRepo = RoomRecordRepository(handle.database, demoRepo)
    val locationProvider = io.github.grupo6.capturaprospectos.data.device.AndroidLocationProvider(context.applicationContext)
    return object : ContextSession {
        override val repository = demoRepo
        override val admissionRepository = admissionRepo
        override val recordRepository = recordRepo
        override val locationProvider = locationProvider
        override fun close() = handle.close()
    }
}
