package io.github.grupo6.capturaprospectos.domain.admission

import io.github.grupo6.capturaprospectos.domain.capture.*
import java.time.LocalDate
import java.time.format.DateTimeParseException

class AdmissionValidator(contactChannels: Set<String> = emptySet(), modalities: Set<String> = emptySet()) {
    private val modalities = modalities.toSet()
    private val channels = contactChannels.toSet()
    private fun optional(value: String?) = value?.trim()?.takeIf { it.isNotEmpty() }
    fun normalize(v: AdmissionInput) = v.copy(
        givenNames = v.givenNames.trim(), familyNames = v.familyNames.trim(), rut = RutValidator.normalize(v.rut),
        email = v.email.trim(), phone = v.phone.filterNot { it.isWhitespace() }, birthDate = v.birthDate.trim(),
        residenceCommune = v.residenceCommune.trim(), currentActivityOther = optional(v.currentActivityOther),
        establishment = optional(v.establishment), establishmentCommune = optional(v.establishmentCommune),
        modality = optional(v.modality), contactChannel = optional(v.contactChannel), notes = optional(v.notes),
        interests = v.interests.toList(), preferredSiteIds = v.preferredSiteIds.toList(),
    )

    fun validate(input: AdmissionInput, intent: SaveIntent, today: LocalDate): List<AdmissionIssue> {
        val v = normalize(input)
        val complete = intent == SaveIntent.CONFIRM_REGISTRATION
        val issues = mutableListOf<AdmissionIssue>()
        fun issue(f: AdmissionField, c: ValidationCode) { issues += AdmissionIssue(f, c) }
        fun text(f: AdmissionField, raw: String?, min: Int, max: Int, required: Boolean) {
            val value = raw.orEmpty()
            val length = value.codePointCount(0, value.length)
            when {
                complete && required && value.isEmpty() -> issue(f, ValidationCode.REQUIRED)
                length > max || (complete && value.isNotEmpty() && length < min) -> issue(f, ValidationCode.LENGTH)
            }
        }
        text(AdmissionField.GIVEN_NAMES, v.givenNames, 2, 120, true)
        text(AdmissionField.FAMILY_NAMES, v.familyNames, 2, 120, true)
        text(AdmissionField.RESIDENCE_COMMUNE, v.residenceCommune, 2, 120, true)
        text(AdmissionField.EMAIL, v.email, 1, 254, true)
        text(AdmissionField.BIRTH_DATE, v.birthDate, 10, 10, true)
        text(AdmissionField.ACTIVITY_OTHER, v.currentActivityOther, 2, 160, v.currentActivity == CurrentActivity.OTHER)
        text(AdmissionField.ESTABLISHMENT, v.establishment, 2, 160, v.currentActivity == CurrentActivity.FOURTH_SECONDARY)
        text(AdmissionField.ESTABLISHMENT_COMMUNE, v.establishmentCommune, 2, 120, false)
        text(AdmissionField.NOTES, v.notes, 0, 500, false)
        if (complete) {
            if (!RutValidator.isValid(v.rut)) issue(AdmissionField.RUT, ValidationCode.FORMAT)
            if (v.email.isNotEmpty() && !ContactFormats.validEmail(v.email)) issue(AdmissionField.EMAIL, ValidationCode.FORMAT)
            if (!Regex("\\+569[0-9]{8}").matches(v.phone)) issue(AdmissionField.PHONE, ValidationCode.FORMAT)
            if (v.currentActivity == null) issue(AdmissionField.ACTIVITY, ValidationCode.REQUIRED)
            if (v.birthDate.isNotEmpty()) {
                val date = try { LocalDate.parse(v.birthDate) } catch (_: DateTimeParseException) { null }
                if (!Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(v.birthDate) || date == null || date.isAfter(today)) {
                    issue(AdmissionField.BIRTH_DATE, ValidationCode.FORMAT)
                }
            }
        }
        // No descartar silenciosamente texto al cambiar una selección del formulario.
        if (v.currentActivity != CurrentActivity.OTHER && v.currentActivityOther != null) issue(AdmissionField.ACTIVITY_OTHER, ValidationCode.INVALID_EVIDENCE)
        if (v.establishment == null && v.establishmentCommune != null) issue(AdmissionField.ESTABLISHMENT_COMMUNE, ValidationCode.INVALID_EVIDENCE)
        if (v.contactChannel != null && v.contactChannel !in channels) issue(AdmissionField.CHANNEL, ValidationCode.UNKNOWN_OPTION)
        if (v.modality != null && v.modality !in modalities) issue(AdmissionField.MODALITY, ValidationCode.UNKNOWN_OPTION)
        return issues.toList()
    }
}
