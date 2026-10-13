package io.github.grupo6.capturaprospectos.domain.context

import io.github.grupo6.capturaprospectos.domain.capture.*
import java.time.*
import java.util.UUID

/** Copia de trabajo en memoria. Fechas con offset explícito preservan el instante incluso en cambio horario. */
data class EventEditorInput(val eventId:UUID?=null,val siteId:UUID?=null,val name:String="",
    val startsAt:String="",val endsAt:String="",val timeZone:String="America/Santiago",
    val latitude:String="",val longitude:String="",val establishment:String="",val commune:String="",
    val captorIds:Set<UUID> = emptySet()) {
    companion object {
        fun from(event:CaptureEvent,suggestion:EstablishmentSuggestion?)=EventEditorInput(event.id,event.siteId,event.name,
            event.startsAt.atZone(event.timeZone).toOffsetDateTime().toString(),event.endsAt.atZone(event.timeZone).toOffsetDateTime().toString(),event.timeZone.id,
            event.location?.latitude?.toString().orEmpty(),event.location?.longitude?.toString().orEmpty(),
            suggestion?.name.orEmpty(),suggestion?.commune.orEmpty(),event.assignedCaptorIds)
    }
}
sealed interface EventPreparation {
    data class Ready(val command:SaveDemoEvent):EventPreparation
    data class Invalid(val issues:List<String>):EventPreparation
}
/** Reglas comunes de comandos UI/repositorio; la autorización de sedes y asignaciones es transaccional. */
fun eventIssues(command:SaveDemoEvent):List<String> = buildList {
    val name=command.name.trim()
    if(name.codePointCount(0,name.length) !in 3..100) add("El nombre del evento debe tener entre 3 y 100 caracteres.")
    if(command.startsAt >= command.endsAt) add("El inicio debe ser anterior al término.")
    if(command.coordinates?.isValid()==false) add("Las coordenadas declaradas no son válidas.")
    val school=command.establishment?.trim().orEmpty();val commune=command.establishmentCommune?.trim().orEmpty()
    if((school.isNotEmpty() && school.codePointCount(0,school.length) !in 2..160) || (commune.isNotEmpty() && commune.codePointCount(0,commune.length) !in 2..120)) add("Establecimiento: 2–160 caracteres; comuna: 2–120 si se informa (contrato F2027).")
    if(commune.isNotEmpty() && school.isEmpty()) add("La comuna del colegio requiere un establecimiento.")
}
fun prepareEvent(input:EventEditorInput):EventPreparation {
    val site=input.siteId ?: return EventPreparation.Invalid(listOf("Selecciona una sede autorizada."))
    val zone=try { ZoneId.of(input.timeZone.trim()) } catch (_:DateTimeException) { return EventPreparation.Invalid(listOf("Zona horaria no válida.")) }
    fun date(value:String):Instant? = try {
        val parsed=OffsetDateTime.parse(value.trim())
        if(parsed.offset !in zone.rules.getValidOffsets(parsed.toLocalDateTime())) null else parsed.toInstant()
    } catch (_:DateTimeException) { null }
    val start=date(input.startsAt);val end=date(input.endsAt)
    if(start==null || end==null) return EventPreparation.Invalid(listOf("Revisa inicio y término: usa fecha, hora y offset válidos para la zona, por ejemplo 2026-10-12T09:00-03:00."))
    val lat=input.latitude.trim();val lon=input.longitude.trim()
    val coordinates=if(lat.isEmpty() && lon.isEmpty()) null else {
        val a=lat.toDoubleOrNull();val b=lon.toDoubleOrNull()
        if(a==null || b==null || !Coordinates(a,b).isValid()) return EventPreparation.Invalid(listOf("Introduce ambas coordenadas válidas o deja ambas vacías."))
        Coordinates(a,b)
    }
    val command=SaveDemoEvent(input.eventId,site,input.name.trim(),start,end,zone,coordinates,
        input.establishment.trim().ifEmpty { null },input.commune.trim().ifEmpty { null },input.captorIds)
    val issues=eventIssues(command)
    return if(issues.isEmpty()) EventPreparation.Ready(command) else EventPreparation.Invalid(issues)
}
