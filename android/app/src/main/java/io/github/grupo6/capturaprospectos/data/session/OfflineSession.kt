package io.github.grupo6.capturaprospectos.data.session

import io.github.grupo6.capturaprospectos.data.remote.UserSummaryDto

const val OFFLINE_WINDOW_MS = 12 * 60 * 60 * 1000L
interface OfflineClock { fun elapsedMillis(): Long; fun bootId(): Int }
object ProcessClock : OfflineClock {
    override fun elapsedMillis() = System.nanoTime() / 1_000_000
    override fun bootId() = -1 // Sin identidad de arranque no se autoriza restauración offline.
}
data class OfflineSession(val account: UserSummaryDto, val issuedElapsed: Long, val bootId: Int, val schema: Int = 1) {
    fun remaining(clock: OfflineClock): Long {
        if (schema != 1 || bootId < 0 || clock.bootId() != bootId || issuedElapsed < 0) return 0
        val age = clock.elapsedMillis() - issuedElapsed
        if (age < 0 || age >= OFFLINE_WINDOW_MS || account.status != "ACTIVE" ||
            account.role !in listOf("CAPTADOR", "SUPERVISOR") || account.siteIds.isEmpty()) return 0
        return OFFLINE_WINDOW_MS - age
    }
    override fun toString() = "OfflineSession(identity=redacted)"
}
interface OfflineSessionStore {
    suspend fun read(): OfflineSession?
    suspend fun write(session: OfflineSession, expectedRevision: Long): Boolean
    fun revision(): Long
    /** Invalida en forma durable antes de permitir otra sesión; nunca borra Room. */
    fun clear()
}
object NoOfflineSession : OfflineSessionStore {
    override suspend fun read(): OfflineSession? = null
    override suspend fun write(session: OfflineSession, expectedRevision: Long) = false
    override fun revision() = 0L
    override fun clear() {}
}
