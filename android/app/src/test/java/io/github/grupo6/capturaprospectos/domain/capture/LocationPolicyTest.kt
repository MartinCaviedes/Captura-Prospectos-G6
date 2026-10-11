package io.github.grupo6.capturaprospectos.domain.capture

import java.time.Duration
import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationPolicyTest {

    private val policy = LocationPolicy()
    private val now = Instant.parse("2026-10-10T16:00:00Z")
    private val actorId = UUID(0, 1)
    private val eventCoords = Coordinates(-33.0245, -71.5518)

    @Test
    fun acceptsValidDeviceLocationWithinThresholds() {
        val device = CaptureLocation.Device(
            coordinates = eventCoords,
            measuredAt = now.minusSeconds(60),
            accuracyMeters = 15.0,
        )
        assertTrue(policy.isValid(device, now, actorId, eventCoords))
    }

    @Test
    fun rejectsInaccurateDeviceLocation() {
        val inaccurate = CaptureLocation.Device(
            coordinates = eventCoords,
            measuredAt = now.minusSeconds(10),
            accuracyMeters = 105.0,
        )
        assertFalse(policy.isValid(inaccurate, now, actorId, eventCoords))
    }

    @Test
    fun rejectsStaleDeviceLocation() {
        val stale = CaptureLocation.Device(
            coordinates = eventCoords,
            measuredAt = now.minus(Duration.ofMinutes(6)),
            accuracyMeters = 20.0,
        )
        assertFalse(policy.isValid(stale, now, actorId, eventCoords))
    }

    @Test
    fun rejectsFutureDeviceLocation() {
        val future = CaptureLocation.Device(
            coordinates = eventCoords,
            measuredAt = now.plusSeconds(30),
            accuracyMeters = 10.0,
        )
        assertFalse(policy.isValid(future, now, actorId, eventCoords))
    }

    @Test
    fun rejectsInvalidCoordinates() {
        val invalidLat = CaptureLocation.Device(
            coordinates = Coordinates(95.0, -71.0),
            measuredAt = now,
            accuracyMeters = 10.0,
        )
        assertFalse(policy.isValid(invalidLat, now, actorId, eventCoords))

        val nanCoords = CaptureLocation.Device(
            coordinates = Coordinates(Double.NaN, -71.0),
            measuredAt = now,
            accuracyMeters = 10.0,
        )
        assertFalse(policy.isValid(nanCoords, now, actorId, eventCoords))
    }

    @Test
    fun acceptsValidEventConfirmedLocation() {
        val confirmed = CaptureLocation.EventConfirmed(
            coordinates = eventCoords,
            confirmedAt = now.minusSeconds(10),
            confirmedBy = actorId,
        )
        assertTrue(policy.isValid(confirmed, now, actorId, eventCoords))
    }

    @Test
    fun rejectsEventConfirmedWithMismatchedCoordinates() {
        val confirmed = CaptureLocation.EventConfirmed(
            coordinates = Coordinates(-33.4372, -70.6506),
            confirmedAt = now,
            confirmedBy = actorId,
        )
        assertFalse(policy.isValid(confirmed, now, actorId, eventCoords))
    }

    @Test
    fun rejectsEventConfirmedWithMismatchedActor() {
        val otherActor = UUID(0, 99)
        val confirmed = CaptureLocation.EventConfirmed(
            coordinates = eventCoords,
            confirmedAt = now,
            confirmedBy = otherActor,
        )
        assertFalse(policy.isValid(confirmed, now, actorId, eventCoords))
    }

    @Test
    fun rejectsEventConfirmedInFuture() {
        val future = CaptureLocation.EventConfirmed(
            coordinates = eventCoords,
            confirmedAt = now.plusSeconds(10),
            confirmedBy = actorId,
        )
        assertFalse(policy.isValid(future, now, actorId, eventCoords))
    }
}
