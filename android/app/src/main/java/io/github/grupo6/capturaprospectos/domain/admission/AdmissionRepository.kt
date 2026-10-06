package io.github.grupo6.capturaprospectos.domain.admission

import io.github.grupo6.capturaprospectos.domain.capture.*
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

data class AdmissionEnvironment(val context: CaptureEnvironment, val programs: List<ProgramOption>, val sites: List<SiteOption>)
/** Contexto confiable de aplicación; preferencias nunca conceden permisos de sede. */
interface AdmissionContextSource {
    suspend fun currentActor(): LocalActor?
    suspend fun environment(eventId: UUID): AdmissionEnvironment?
}
data class NewAdmission(val eventId: UUID, val input: AdmissionInput, val intent: SaveIntent,
    val consent: ConsentEvidence?, val location: CaptureLocation?) {
    override fun toString() = "NewAdmission(redacted)"
}
data class StoredAdmission(
    val id: UUID, val revision: Long, val ownerId: UUID, val siteId: UUID, val eventId: UUID,
    val deviceId: UUID, val capturedAt: Instant, val eventName: String, val eventTimeZone: ZoneId,
    val input: AdmissionInput, val fullNameSnapshot: String, val consent: ConsentEvidence,
    val programs: List<SelectedProgram>, val sites: List<SiteOption>, val location: CaptureLocation?,
    val state: CaptureState, val transportState: TransportState = TransportState.NOT_QUEUED,
) { override fun toString() = "StoredAdmission(id=$id, state=$state, personalData=redacted)" }
sealed interface CreateAdmissionResult {
    data class Saved(val id: UUID, val revision: Long, val state: CaptureState) : CreateAdmissionResult
    data object AccessDenied : CreateAdmissionResult
    data object ConsentRequired : CreateAdmissionResult
    data class Invalid(val issues: List<AdmissionIssue>) : CreateAdmissionResult
    data class DuplicateOwn(val id: UUID) : CreateAdmissionResult
    data object DuplicateRestricted : CreateAdmissionResult
    data object StorageUnavailable : CreateAdmissionResult
}
sealed interface ReadAdmissionResult {
    data class Found(val capture: StoredAdmission) : ReadAdmissionResult
    data class Legacy(val capture: StoredCapture) : ReadAdmissionResult
    data object Unavailable : ReadAdmissionResult
    data object StorageUnavailable : ReadAdmissionResult
}
/** Altas F2027 y lectura versionada; no edición/conversión implícita de registros antiguos. */
interface AdmissionRepository {
    suspend fun create(request: NewAdmission): CreateAdmissionResult
    suspend fun get(id: UUID): ReadAdmissionResult
}
