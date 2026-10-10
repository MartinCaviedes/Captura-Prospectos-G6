package io.github.grupo6.capturaprospectos.domain.context

import io.github.grupo6.capturaprospectos.domain.capture.Coordinates
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test

class EventAdminTest {

    private val siteId = UUID.randomUUID()
    private val captor1 = UUID.randomUUID()
    private val captor2 = UUID.randomUUID()

    @Test
    fun validEventCreationCommandPreservesAllFields() {
        val now = Instant.now()
        val later = now.plus(8, ChronoUnit.HOURS)
        val cmd = SaveDemoEvent(
            eventId = null,
            siteId = siteId,
            name = "Feria Vocacional Regional 2026",
            startsAt = now,
            endsAt = later,
            timeZone = ZoneId.of("America/Santiago"),
            coordinates = Coordinates(-33.0245, -71.5518),
            establishment = "Liceo Bicentenario Viña",
            establishmentCommune = "Viña del Mar",
            assignedCaptorIds = setOf(captor1, captor2),
        )

        assertNull(cmd.eventId)
        assertEquals(siteId, cmd.siteId)
        assertEquals("Feria Vocacional Regional 2026", cmd.name)
        assertEquals(now, cmd.startsAt)
        assertEquals(later, cmd.endsAt)
        assertEquals("America/Santiago", cmd.timeZone.id)
        assertNotNull(cmd.coordinates)
        assertTrue(cmd.coordinates!!.isValid())
        assertEquals("Liceo Bicentenario Viña", cmd.establishment)
        assertEquals("Viña del Mar", cmd.establishmentCommune)
        assertEquals(2, cmd.assignedCaptorIds.size)
    }

    @Test
    fun editEventCommandPreservesExistingEventId() {
        val existingId = UUID.randomUUID()
        val now = Instant.now()
        val later = now.plus(48, ChronoUnit.HOURS) // Multi-day event
        val cmd = SaveDemoEvent(
            eventId = existingId,
            siteId = siteId,
            name = "Visita Multidía Colegio Alemán",
            startsAt = now,
            endsAt = later,
            timeZone = ZoneId.of("America/Santiago"),
            coordinates = null,
            establishment = "Colegio Alemán",
            establishmentCommune = "Valparaíso",
            assignedCaptorIds = setOf(captor1),
        )

        assertEquals(existingId, cmd.eventId)
        assertEquals(48, ChronoUnit.HOURS.between(cmd.startsAt, cmd.endsAt))
        assertEquals(1, cmd.assignedCaptorIds.size)
    }

    @Test
    fun saveEventResultVariantsFunctionCorrectly() {
        val successId = UUID.randomUUID()
        val success = SaveEventResult.Success(successId)
        assertEquals(successId, success.eventId)

        val unauthorized = SaveEventResult.Unauthorized
        assertNotNull(unauthorized)

        val invalid = SaveEventResult.Invalid(listOf("Nombre muy corto", "Fechas inconsistentes"))
        assertEquals(2, invalid.issues.size)

        val storageError = SaveEventResult.StorageUnavailable
        assertNotNull(storageError)
    }
}
