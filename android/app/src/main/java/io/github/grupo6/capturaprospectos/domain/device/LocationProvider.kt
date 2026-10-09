package io.github.grupo6.capturaprospectos.domain.device

import io.github.grupo6.capturaprospectos.domain.capture.CaptureLocation
import java.time.Duration

sealed interface LocationResult {
    /** Candidato: el consumidor todavía debe aplicar LocationPolicy y pedir confirmación. */
    data class Candidate(val position: CaptureLocation.Device) : LocationResult {
        override fun toString() = "LocationCandidate(redacted)"
    }
    data object PermissionDenied : LocationResult
    data object ServicesDisabled : LocationResult
    data object TimedOut : LocationResult
    data object Unavailable : LocationResult
}
/** Implementación Android en ronda 2. Cancelar debe detener la solicitud, sin caché ni fallback implícito. */
fun interface LocationProvider {
    suspend fun request(timeout: Duration): LocationResult
}
object LocationRequestDefaults { val timeout: Duration = Duration.ofSeconds(10) }
