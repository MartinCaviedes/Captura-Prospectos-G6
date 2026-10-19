package io.github.grupo6.capturaprospectos.data.local

import io.github.grupo6.capturaprospectos.domain.capture.LocalRole
import io.github.grupo6.capturaprospectos.domain.context.DemoProfile
import java.util.Locale
import java.util.UUID

data class DemoAccount(val actorId: UUID, val email: String, val role: LocalRole)

/** Valores públicos inventados para evaluación; nunca reutilizarlos como credenciales reales. */
object DemoAccessFixtures {
    const val PASSWORD = "DemoG6!2026"
    val accounts = listOf(
        DemoAccount(UUID.fromString(DemoSeed.id("captor-a")), "captador.a@g6.example", LocalRole.CAPTOR),
        DemoAccount(UUID.fromString(DemoSeed.id("captor-b")), "captador.b@g6.example", LocalRole.CAPTOR),
        DemoAccount(UUID.fromString(DemoSeed.id("supervisor")), "supervisor@g6.example", LocalRole.SUPERVISOR),
    )
    fun match(email: String, password: String, profiles: List<DemoProfile>): DemoAccount? {
        if (password != PASSWORD) return null
        val key = email.trim().lowercase(Locale.ROOT)
        return accounts.firstOrNull { account -> account.email == key &&
            profiles.any { it.actor.id == account.actorId && it.actor.role == account.role } }
    }
    fun canLoad(actorId: UUID, eventId: UUID) = when (actorId.toString()) {
        DemoSeed.id("captor-a") -> eventId.toString() in setOf(DemoSeed.id("event-a"), DemoSeed.id("event-a2"))
        DemoSeed.id("captor-b") -> eventId.toString() == DemoSeed.id("event-b")
        else -> false
    }
}
