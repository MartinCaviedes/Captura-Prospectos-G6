package io.github.grupo6.capturaprospectos.domain.capture

import java.time.Duration
import java.time.Instant
import java.util.UUID

data class Coordinates(val latitude: Double, val longitude: Double) {
    fun isValid() = latitude.isFinite() && longitude.isFinite() &&
        latitude in -90.0..90.0 && longitude in -180.0..180.0
    override fun toString() = "Coordinates(redacted)"
}

sealed interface CaptureLocation {
    data class Device(val coordinates: Coordinates, val measuredAt: Instant, val accuracyMeters: Double) : CaptureLocation
    data class EventConfirmed(val coordinates: Coordinates, val confirmedAt: Instant, val confirmedBy: UUID) : CaptureLocation
}

/** PT04: umbrales de trabajo ajustables tras medición, no aprobación del cliente. */
class LocationPolicy(
    private val maxAge: Duration = Duration.ofMinutes(5),
    private val maxAccuracyMeters: Double = 100.0,
) {
    init {
        require(!maxAge.isNegative)
        require(maxAccuracyMeters.isFinite() && maxAccuracyMeters >= 0)
    }

    // Evaluar al preparar una captura nueva, no invalidar snapshots históricos al leerlos.
    fun isValid(location: CaptureLocation, now: Instant, actorId: UUID, declaredEventLocation: Coordinates?): Boolean = when (location) {
        is CaptureLocation.Device -> location.coordinates.isValid() &&
            location.accuracyMeters.isFinite() && location.accuracyMeters in 0.0..maxAccuracyMeters &&
            !location.measuredAt.isAfter(now) && Duration.between(location.measuredAt, now) <= maxAge
        is CaptureLocation.EventConfirmed -> location.coordinates.isValid() &&
            location.coordinates == declaredEventLocation && location.confirmedBy == actorId &&
            !location.confirmedAt.isAfter(now)
    }
}
