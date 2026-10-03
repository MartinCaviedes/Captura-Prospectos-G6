package io.github.grupo6.capturaprospectos.domain.capture

import java.time.Instant
import java.time.ZoneId
import java.util.UUID

enum class LocalRole { CAPTOR, SUPERVISOR }
data class LocalActor(val id: UUID, val role: LocalRole, val siteIds: Set<UUID>)
data class CaptureEvent(
    val id: UUID, val siteId: UUID, val name: String,
    val startsAt: Instant, val endsAt: Instant, val timeZone: ZoneId,
    val assignedCaptorIds: Set<UUID>, val location: Coordinates?,
)
data class CaptureEnvironment(
    val actor: LocalActor, val event: CaptureEvent, val deviceId: UUID,
    val offerings: List<Offering>, val consentVersion: String, val consentText: String,
)

/** Fuente de contexto seleccionada por la aplicación, nunca por campos del formulario. */
interface CaptureContextSource {
    suspend fun currentActor(): LocalActor?
    suspend fun environment(eventId: UUID): CaptureEnvironment?
}

object LocalCaptureAccess {
    fun canCreate(actor: LocalActor, event: CaptureEvent): Boolean =
        actor.role == LocalRole.CAPTOR && event.siteId in actor.siteIds &&
            actor.id in event.assignedCaptorIds && event.startsAt < event.endsAt && event.name.isNotBlank()

    fun canRead(actor: LocalActor, ownerId: UUID, siteId: UUID, state: CaptureState): Boolean =
        siteId in actor.siteIds && (actor.id == ownerId ||
            (actor.role == LocalRole.SUPERVISOR && state != CaptureState.DRAFT))
}

data class NewCapture(
    val eventId: UUID, val input: CaptureInput, val intent: SaveIntent,
    val consent: ConsentEvidence?, val offeringIds: List<UUID>, val location: CaptureLocation?,
) { override fun toString() = "NewCapture(redacted)" }

data class StoredCapture(
    val id: UUID, val revision: Long, val ownerId: UUID, val siteId: UUID,
    val eventId: UUID, val deviceId: UUID, val capturedAt: Instant,
    val eventName: String, val eventTimeZone: ZoneId, val input: CaptureInput,
    val consent: ConsentEvidence, val interests: List<Offering>,
    val location: CaptureLocation?, val state: CaptureState,
    val transportState: TransportState = TransportState.NOT_QUEUED,
) { override fun toString() = "StoredCapture(id=$id, state=$state, personalData=redacted)" }

sealed interface CreateCaptureResult {
    data class Saved(val id: UUID, val revision: Long, val state: CaptureState) : CreateCaptureResult
    data object AccessDenied : CreateCaptureResult
    data object ConsentRequired : CreateCaptureResult
    data class Invalid(val issues: List<FieldIssue>) : CreateCaptureResult
    data class DuplicateOwn(val id: UUID) : CreateCaptureResult
    data object DuplicateRestricted : CreateCaptureResult
    data object StorageUnavailable : CreateCaptureResult
}
sealed interface ReadCaptureResult {
    data class Found(val capture: StoredCapture) : ReadCaptureResult
    data object Unavailable : ReadCaptureResult
    data object StorageUnavailable : ReadCaptureResult
}

/** Primer incremento: altas y detalle autorizado; edición/anulación se incorporarán después. */
interface CaptureRepository {
    suspend fun create(request: NewCapture): CreateCaptureResult
    suspend fun get(id: UUID): ReadCaptureResult
}
