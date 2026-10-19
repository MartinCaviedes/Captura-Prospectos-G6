package io.github.grupo6.capturaprospectos.domain.context

import io.github.grupo6.capturaprospectos.domain.capture.*
import java.time.*
import java.util.UUID
import org.junit.Test
import org.junit.Assert.*

class EventPreparationTest {
    private val event=CaptureEvent(UUID(0,1),UUID(0,2),"Evento ficticio",Instant.parse("2026-10-01T12:00:00.123Z"),
        Instant.parse("2026-11-01T23:00:00Z"),ZoneId.of("America/Santiago"),setOf(UUID(0,3)),Coordinates(-33.0,-71.0))
    private val source=EventEditorInput.from(event,EstablishmentSuggestion("Colegio ficticio","Comuna ficticia"))
    @Test fun unchangedEditingPreservesDatesCoordinatesSchoolCommuneAndAssignments() {
        val command=(prepareEvent(source.copy(name="Nombre corregido")) as EventPreparation.Ready).command
        assertEquals(event.id,command.eventId);assertEquals(event.startsAt,command.startsAt);assertEquals(event.endsAt,command.endsAt)
        assertEquals(event.location,command.coordinates);assertEquals(event.assignedCaptorIds,command.assignedCaptorIds)
        assertEquals("Colegio ficticio",command.establishment);assertEquals("Comuna ficticia",command.establishmentCommune)
    }
    @Test fun explicitMultipleDaysAndClearedOptionalLocationArePrepared() {
        val command=(prepareEvent(source.copy(startsAt="2026-10-12T09:00-03:00",endsAt="2026-10-15T17:00-03:00",latitude="",longitude="",establishment="",commune="")) as EventPreparation.Ready).command
        assertEquals(80,Duration.between(command.startsAt,command.endsAt).toHours().toInt());assertNull(command.coordinates)
        assertNull(command.establishment);assertNull(command.establishmentCommune)
    }
    @Test fun invalidDatesOffsetsZonesAndCoordinatePairsCannotProduceCommand() {
        val bad=listOf(source.copy(startsAt="2026-02-30T09:00-03:00"),source.copy(endsAt=source.startsAt),
            source.copy(startsAt="2026-10-12T09:00+01:00"),source.copy(timeZone="invalid"),
            source.copy(latitude="NaN"),source.copy(latitude="91"),source.copy(longitude=""),source.copy(siteId=null))
        bad.forEach { assertTrue(prepareEvent(it) is EventPreparation.Invalid) }
    }
    @Test fun communeWithoutSchoolAndBlankOrTooLongNameAreInvalid() {
        for(input in listOf(source.copy(establishment=""),source.copy(name="x"),source.copy(name="x".repeat(101))))
            assertTrue(prepareEvent(input) is EventPreparation.Invalid)
    }
}
