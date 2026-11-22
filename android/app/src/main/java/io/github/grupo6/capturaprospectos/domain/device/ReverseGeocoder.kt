package io.github.grupo6.capturaprospectos.domain.device

import io.github.grupo6.capturaprospectos.domain.capture.CaptureLocation
import java.time.Duration
import java.time.Instant

enum class GeocodingFailure { NETWORK, LIMITED, UNAVAILABLE, INVALID_RESPONSE, STALE_POSITION }
sealed interface GeocodingResult {
    data class Found(val label:String):GeocodingResult
    data class Failed(val reason:GeocodingFailure):GeocodingResult
}
fun interface ReverseGeocoder {
    /** Solo coordenadas actuales del dispositivo, tras una acción informada del usuario. */
    suspend fun lookup(position:CaptureLocation.Device):GeocodingResult
}
object GeocodingPolicy {
    fun isFresh(position:CaptureLocation.Device,now:Instant):Boolean {
        val age=Duration.between(position.measuredAt,now)
        return position.coordinates.isValid() && position.accuracyMeters.isFinite() &&
            position.accuracyMeters in 0.0..100.0 && !age.isNegative && age<=Duration.ofSeconds(60)
    }
}
