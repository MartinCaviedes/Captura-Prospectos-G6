package io.github.grupo6.capturaprospectos.data.session

import io.github.grupo6.capturaprospectos.data.remote.TokenResponseDto

const val MAX_ONLINE_SESSION_SECONDS = 4 * 60 * 60L

/** Autorización online cifrada: sin contraseña; no prolonga el vencimiento al restaurar. */
data class RememberedSession(val token: TokenResponseDto, val issuedElapsed: Long,
    val bootId: Int, val backendUrl: String, val schema: Int = 1) {
    fun remaining(clock: OfflineClock, url: String): Long {
        if (schema != 1 || bootId < 0 || clock.bootId() != bootId || issuedElapsed < 0 || backendUrl != url ||
            token.expiresIn !in 1..MAX_ONLINE_SESSION_SECONDS || token.accessToken.isBlank()) return 0
        val age = clock.elapsedMillis() - issuedElapsed
        val lifetime = token.expiresIn * 1000
        return if (age < 0 || age >= lifetime) 0 else lifetime - age
    }
    override fun toString() = "RememberedSession(credentials=redacted)"
}
interface RememberedSessionStore {
    suspend fun read(): RememberedSession?
    suspend fun write(session: RememberedSession, expectedRevision: Long): Boolean
    fun revision(): Long
    fun clear()
}
object NoRememberedSession : RememberedSessionStore {
    override suspend fun read(): RememberedSession? = null
    override suspend fun write(session: RememberedSession, expectedRevision: Long) = false
    override fun revision() = 0L
    override fun clear() {}
}
