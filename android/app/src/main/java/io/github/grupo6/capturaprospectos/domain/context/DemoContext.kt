package io.github.grupo6.capturaprospectos.domain.context

import io.github.grupo6.capturaprospectos.domain.admission.*
import io.github.grupo6.capturaprospectos.domain.capture.*
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.Flow

data class DemoProfile(val actor: LocalActor, val displayName: String)
data class DemoCatalog(
    val version: String, val sourceKind: String, val sourceReference: String, val academicYear: Int,
    val programs: List<ProgramOption>, val sites: List<SiteOption>,
)
/** Establecimiento configurado explícitamente; la UI debe pedir confirmación y permitir editar. */
data class EstablishmentSuggestion(val name: String, val commune: String?)
data class DemoContextSnapshot(
    val deviceId: UUID, val profiles: List<DemoProfile>, val currentProfile: DemoProfile?,
    val availableEvents: List<CaptureEvent>, val selectedEvent: CaptureEvent?,
    val establishmentSuggestion: EstablishmentSuggestion?, val catalog: DemoCatalog,
    val consentVersion: String, val consentText: String,
)
sealed interface DemoContextState {
    data object NotInitialized : DemoContextState
    data class Ready(val snapshot: DemoContextSnapshot) : DemoContextState
    data object StorageUnavailable : DemoContextState
}
enum class ContextChange { APPLIED, ALREADY_INITIALIZED, INVALID_SELECTION, NOT_INITIALIZED, INCOMPATIBLE_SEED, STORAGE_UNAVAILABLE }


data class SaveDemoEvent(
    val eventId: UUID? = null,
    val siteId: UUID,
    val name: String,
    val startsAt: Instant,
    val endsAt: Instant,
    val timeZone: ZoneId = ZoneId.of("America/Santiago"),
    val coordinates: Coordinates? = null,
    val establishment: String? = null,
    val establishmentCommune: String? = null,
    val assignedCaptorIds: Set<UUID> = emptySet(),
)

sealed interface SaveEventResult {
    data class Success(val eventId: UUID) : SaveEventResult
    data object Unauthorized : SaveEventResult
    data class Invalid(val issues: List<String>) : SaveEventResult
    data object StorageUnavailable : SaveEventResult
}

/** Perfiles de demostración seleccionables: no autentica identidades ni emite tokens. */
interface DemoContextRepository : AdmissionContextSource {
    suspend fun initialize(): ContextChange
    suspend fun snapshot(): DemoContextState
    fun observe(): Flow<DemoContextState>
    suspend fun selectProfile(id: UUID): ContextChange
    suspend fun selectEvent(id: UUID): ContextChange
    suspend fun clearSelection(): ContextChange
    suspend fun saveEvent(command: SaveDemoEvent): SaveEventResult
}
