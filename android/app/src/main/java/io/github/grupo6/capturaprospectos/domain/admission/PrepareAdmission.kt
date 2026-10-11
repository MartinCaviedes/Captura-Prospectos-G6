package io.github.grupo6.capturaprospectos.domain.admission

import io.github.grupo6.capturaprospectos.domain.capture.*
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

/** Valida escrituras F2027, sin DAO ni inferencias de establecimiento.
 * En edición, consentActorId identifica al registrador de la evidencia persistida e inmutable;
 * actorId sigue identificando a quien confirma una ubicación nueva. No revalida lecturas históricas. */
class PrepareAdmission(
    private val validator: AdmissionValidator = AdmissionValidator(),
    private val locations: LocationPolicy = LocationPolicy(),
) {
    fun prepare(
        input: AdmissionInput, intent: SaveIntent, consent: ConsentEvidence?,
        expectedConsentVersion: String, actorId: UUID,
        availablePrograms: List<ProgramOption>, availableSites: List<SiteOption>,
        location: CaptureLocation?, declaredEventLocation: Coordinates?,
        now: Instant, eventZone: ZoneId, consentActorId: UUID = actorId,
    ): AdmissionPreparation {
        if (consent == null) return AdmissionPreparation.ConsentRequired
        val v = validator.normalize(input)
        val issues = validator.validate(v, intent, now.atZone(eventZone).toLocalDate()).toMutableList()
        fun issue(f: AdmissionField, c: ValidationCode) { issues += AdmissionIssue(f, c) }
        if (expectedConsentVersion.isBlank() || consent.version != expectedConsentVersion ||
            consent.recordedBy != consentActorId || consent.acceptedAt.isAfter(now)) issue(AdmissionField.CONSENT, ValidationCode.INVALID_EVIDENCE)
        if (intent == SaveIntent.CONFIRM_REGISTRATION && v.interests.size !in 1..3) issue(AdmissionField.INTERESTS, ValidationCode.LENGTH)
        if (v.interests.map { it.programId }.distinct().size != v.interests.size) issue(AdmissionField.INTERESTS, ValidationCode.DUPLICATE)
        if (v.interests.map { it.priority }.sorted() != (1..v.interests.size).toList()) issue(AdmissionField.INTERESTS, ValidationCode.FORMAT)
        val programs = v.interests.mapNotNull { selected ->
            val program = availablePrograms.singleOrNull { it.id == selected.programId }
            val mention = selected.mentionId?.let { program?.mentions?.get(it) }
            if (program == null || !program.enabled || program.name.isBlank() || program.catalogVersion.isBlank() ||
                program.section.isBlank() || (selected.mentionId != null && mention.isNullOrBlank())) {
                issue(AdmissionField.INTERESTS, ValidationCode.UNAVAILABLE)
                null
            } else SelectedProgram(program.id, program.name, program.catalogVersion, program.section, selected.priority, selected.mentionId, mention)
        }.sortedBy { it.priority }
        if (v.preferredSiteIds.distinct().size != v.preferredSiteIds.size) issue(AdmissionField.SITES, ValidationCode.DUPLICATE)
        val sites = v.preferredSiteIds.mapNotNull { id ->
            val site = availableSites.singleOrNull { it.id == id }
            if (site == null || !site.enabled || site.name.isBlank() || site.catalogVersion.isBlank()) {
                issue(AdmissionField.SITES, ValidationCode.UNAVAILABLE)
                null
            } else site
        }
        if (location != null && !locations.isValid(location, now, actorId, declaredEventLocation)) issue(AdmissionField.LOCATION, ValidationCode.INVALID_EVIDENCE)
        if (issues.isNotEmpty()) return AdmissionPreparation.Invalid(issues.toList())
        val state = when {
            intent == SaveIntent.SAVE_DRAFT -> CaptureState.DRAFT
            location == null -> CaptureState.NEEDS_CONTEXT
            else -> CaptureState.READY
        }
        return AdmissionPreparation.Prepared(v.copy(interests = v.interests.sortedBy { it.priority }), consent, programs, sites, location, state)
    }
}
