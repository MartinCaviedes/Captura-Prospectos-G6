package io.github.grupo6.capturaprospectos.domain.records

import io.github.grupo6.capturaprospectos.domain.admission.*
import io.github.grupo6.capturaprospectos.domain.capture.*
import java.time.Instant
import java.util.UUID

/** Propuesta R1, sin implementación Room. Requiere revisión antes de crear consumidores. */
data class RecordFilter(
    val query: String = "", val eventId: UUID? = null,
    val states: Set<CaptureState> = emptySet(),
    val fromInclusive: Instant? = null, val untilExclusive: Instant? = null,
) {
    fun isValid() = query.length <= 120 &&
        (fromInclusive == null || untilExclusive == null || fromInclusive < untilExclusive)
    override fun toString() = "RecordFilter(redacted)"
}
/** Orden capturado DESC, UUID canónico DESC. Reiniciar cursor al cambiar filtro o perfil. */
data class RecordCursor(val capturedAt: Instant, val id: UUID)
data class RecordQuery(val filter: RecordFilter = RecordFilter(), val after: RecordCursor? = null, val limit: Int = 30) {
    fun isValid() = limit in 1..100 && filter.isValid()
    override fun toString() = "RecordQuery(redacted)"
}
enum class RecordContract { LEGACY, F2027 }
data class RecordSummary(
    val id: UUID, val revision: Long, val capturedAt: Instant, val eventId: UUID,
    val displayName: String, val rut: String, val state: CaptureState, val contract: RecordContract,
) { override fun toString() = "RecordSummary(id=$id, personalData=redacted)" }
sealed interface RecordPageResult {
    data class Page(val items: List<RecordSummary>, val next: RecordCursor?) : RecordPageResult
    data object InvalidQuery : RecordPageResult
    data object AccessDenied : RecordPageResult
    data object StorageUnavailable : RecordPageResult
}
data class EditAdmission(
    val id: UUID, val expectedRevision: Long, val input: AdmissionInput,
    val intent: SaveIntent, val location: CaptureLocation?, val reason: String? = null,
) { override fun toString() = "EditAdmission(redacted)" }
data class CancelRecord(val id: UUID, val expectedRevision: Long, val reason: String) {
    override fun toString() = "CancelRecord(redacted)"
}
sealed interface RecordMutationResult {
    data class Saved(val id: UUID, val revision: Long, val state: CaptureState) : RecordMutationResult
    data class Invalid(val issues: List<AdmissionIssue>) : RecordMutationResult
    data object InvalidRevision : RecordMutationResult
    data object ReasonRequired : RecordMutationResult
    data object InvalidReason : RecordMutationResult
    /** No revela si existe un ID fuera del alcance del actor. */
    data object Unavailable : RecordMutationResult
    data object RevisionConflict : RecordMutationResult
    data object InvalidTransition : RecordMutationResult
    data object UnsupportedContract : RecordMutationResult
    data class DuplicateOwn(val id: UUID) : RecordMutationResult
    data object DuplicateRestricted : RecordMutationResult
    data object StorageUnavailable : RecordMutationResult
}
/** Contexto confiable para editar: catálogo actual y evidencia del registro, nunca alta implícita. */
sealed interface RecordEditContextResult {
    data class Available(val environment: AdmissionEnvironment,
        val suggestion: io.github.grupo6.capturaprospectos.domain.context.EstablishmentSuggestion? = null) : RecordEditContextResult
    data object Unavailable : RecordEditContextResult
    data object StorageUnavailable : RecordEditContextResult
}
interface RecordRepository {
    suspend fun editContext(id: UUID): RecordEditContextResult = RecordEditContextResult.Unavailable
    suspend fun list(query: RecordQuery): RecordPageResult
    suspend fun get(id: UUID): ReadAdmissionResult
    suspend fun edit(command: EditAdmission): RecordMutationResult
    suspend fun cancel(command: CancelRecord): RecordMutationResult
}
