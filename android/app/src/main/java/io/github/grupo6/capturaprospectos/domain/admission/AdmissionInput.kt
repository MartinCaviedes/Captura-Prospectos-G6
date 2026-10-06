package io.github.grupo6.capturaprospectos.domain.admission

import io.github.grupo6.capturaprospectos.domain.capture.*
import java.util.UUID

enum class CurrentActivity { FOURTH_SECONDARY, SECONDARY_GRADUATE, OTHER }
enum class PersonalOption { FEMALE, MALE, NOT_STATED }
enum class PreferredSchedule { DAY, EVENING }
enum class InterestLevel { HIGH, MEDIUM, LOW }
enum class Motivation { INCOME_PROGRESS, VOCATION, RESUME_OR_NEW_PATH, FINANCING_CONCERN }

/** Datos volátiles. No persistir ni registrar antes del consentimiento. Fecha en ISO yyyy-MM-dd. */
data class AdmissionInput(
    val givenNames: String = "", val familyNames: String = "",
    val rut: String = "", val email: String = "", val phone: String = "",
    val birthDate: String = "", val residenceCommune: String = "",
    val currentActivity: CurrentActivity? = null, val currentActivityOther: String? = null,
    val establishment: String? = null, val establishmentCommune: String? = null,
    val formPersonalOption: PersonalOption? = null, val preferredSchedule: PreferredSchedule? = null,
    val interestLevel: InterestLevel? = null, val motivation: Motivation? = null,
    val modality: String? = null, val contactChannel: String? = null, val notes: String? = null,
    val interests: List<RankedInterest> = emptyList(), val preferredSiteIds: List<UUID> = emptyList(),
) { override fun toString() = "AdmissionInput(redacted)" }

data class RankedInterest(val programId: UUID, val priority: Int, val mentionId: UUID? = null)
/** Identidad de programa independiente de sede, jornada o modalidad de la oferta. */
data class ProgramOption(
    val id: UUID, val name: String, val catalogVersion: String, val section: String,
    val enabled: Boolean = true, val mentions: Map<UUID, String> = emptyMap(),
)
data class SiteOption(val id: UUID, val name: String, val catalogVersion: String, val enabled: Boolean = true)
data class SelectedProgram(
    val id: UUID, val name: String, val catalogVersion: String, val section: String,
    val priority: Int, val mentionId: UUID?, val mentionName: String?,
)

enum class AdmissionField {
    GIVEN_NAMES, FAMILY_NAMES, RUT, EMAIL, PHONE, BIRTH_DATE, RESIDENCE_COMMUNE,
    ACTIVITY, ACTIVITY_OTHER, ESTABLISHMENT, ESTABLISHMENT_COMMUNE,
    MODALITY, CHANNEL, NOTES, INTERESTS, SITES, CONSENT, LOCATION,
}
data class AdmissionIssue(val field: AdmissionField, val code: ValidationCode)

sealed interface AdmissionPreparation {
    data object ConsentRequired : AdmissionPreparation
    data class Invalid(val issues: List<AdmissionIssue>) : AdmissionPreparation
    /** No acredita permisos ni guardado. El repositorio v1 no acepta este contrato. */
    class Prepared internal constructor(
        input: AdmissionInput, val consent: ConsentEvidence,
        programs: List<SelectedProgram>, sites: List<SiteOption>,
        val location: CaptureLocation?, val state: CaptureState,
    ) : AdmissionPreparation {
        private val snapshot = input.copy(interests = input.interests.toList(), preferredSiteIds = input.preferredSiteIds.toList())
        private val programSnapshot = programs.toList()
        private val siteSnapshot = sites.toList()
        val input: AdmissionInput get() = snapshot.copy(interests = snapshot.interests.toList(), preferredSiteIds = snapshot.preferredSiteIds.toList())
        val programs: List<SelectedProgram> get() = programSnapshot.toList()
        val sites: List<SiteOption> get() = siteSnapshot.toList()
        val transportState = TransportState.NOT_QUEUED
        override fun toString() = "AdmissionPrepared(state=$state, personalData=redacted)"
    }
}
