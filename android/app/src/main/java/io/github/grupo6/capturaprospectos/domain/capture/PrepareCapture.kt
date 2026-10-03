package io.github.grupo6.capturaprospectos.domain.capture

import java.time.Instant
import java.util.UUID

/** Preparación pura de altas E1. No escribe datos ni sustituye autorización/commit en Room. */
class PrepareCapture(
    private val contacts: ContactValidator,
    private val locations: LocationPolicy = LocationPolicy(),
) {
    fun prepare(
        input: CaptureInput,
        intent: SaveIntent,
        consent: ConsentEvidence?,
        expectedConsentVersion: String,
        actorId: UUID,
        siteId: UUID,
        offeringIds: List<UUID>,
        availableOfferings: List<Offering>,
        location: CaptureLocation?,
        declaredEventLocation: Coordinates?,
        now: Instant,
    ): PreparationResult {
        // También un borrador necesita evidencia explícita antes de poder prepararse.
        if (consent == null) return PreparationResult.ConsentRequired
        val issues = contacts.validate(input, intent).toMutableList()
        if (expectedConsentVersion.isBlank() || consent.version != expectedConsentVersion ||
            consent.recordedBy != actorId || consent.acceptedAt.isAfter(now)) {
            issues += FieldIssue(CaptureField.CONSENT, ValidationCode.INVALID_EVIDENCE)
        }
        if (intent == SaveIntent.CONFIRM_REGISTRATION && offeringIds.isEmpty()) {
            issues += FieldIssue(CaptureField.INTERESTS, ValidationCode.REQUIRED)
        }
        val selected = offeringIds.mapNotNull { id -> availableOfferings.singleOrNull { it.id == id } }
        if (selected.size != offeringIds.size || selected.any {
                !it.enabled || it.siteId != siteId || it.catalogVersion.isBlank() || it.programName.isBlank()
            }) {
            issues += FieldIssue(CaptureField.INTERESTS, ValidationCode.UNAVAILABLE)
        }
        if (offeringIds.distinct().size != offeringIds.size || selected.map { it.programId }.distinct().size != selected.size) {
            issues += FieldIssue(CaptureField.INTERESTS, ValidationCode.DUPLICATE)
        }
        if (location != null && !locations.isValid(location, now, actorId, declaredEventLocation)) {
            issues += FieldIssue(CaptureField.LOCATION, ValidationCode.INVALID_EVIDENCE)
        }
        if (issues.isNotEmpty()) return PreparationResult.Invalid(issues.toList())
        val state = when {
            intent == SaveIntent.SAVE_DRAFT -> CaptureState.DRAFT
            location == null -> CaptureState.NEEDS_CONTEXT
            else -> CaptureState.READY
        }
        return PreparationResult.Prepared(contacts.normalize(input), consent, selected, location, state)
    }
}
