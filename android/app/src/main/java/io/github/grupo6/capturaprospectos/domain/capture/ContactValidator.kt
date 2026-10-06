package io.github.grupo6.capturaprospectos.domain.capture

import java.util.Locale

object CaptureLimits {
    const val NAME_MIN = 2
    const val NAME_MAX = 120
    const val EMAIL_MAX = 254
    const val ESTABLISHMENT_MIN = 2
    const val ESTABLISHMENT_MAX = 160
    const val NOTES_MAX = 500
}

object RutValidator {
    fun normalize(value: String): String {
        val compact = value.trim().replace(".", "").uppercase(Locale.ROOT)
        if ('-' in compact || compact.length < 2) return compact
        return compact.dropLast(1) + "-" + compact.last()
    }

    fun isValid(value: String): Boolean {
        val normalized = normalize(value)
        if (!Regex("[0-9]+-[0-9K]").matches(normalized)) return false
        val body = normalized.substringBefore('-')
        if (body.startsWith('0')) return false
        // Módulo 11 verifica estructura, nunca identidad o titularidad.
        val sum = body.reversed().mapIndexed { index, digit ->
            (digit - '0') * (2 + index % 6)
        }.fold(0) { remainder, term -> (remainder + term) % 11 }
        val check = when (val remainder = 11 - sum % 11) {
            11 -> '0'
            10 -> 'K'
            else -> ('0'.code + remainder).toChar()
        }
        return normalized.last() == check
    }
}

class ContactValidator(
    educationLevels: Set<String>,
    contactChannels: Set<String> = emptySet(),
) {
    private val educationLevels = educationLevels.toSet()
    private val contactChannels = contactChannels.toSet()

    fun normalize(input: CaptureInput): CaptureInput = input.copy(
        fullName = input.fullName.trim(),
        rut = RutValidator.normalize(input.rut),
        email = input.email.trim(),
        phone = input.phone.filterNot { it.isWhitespace() },
        educationLevel = input.educationLevel.trim(),
        establishment = input.establishment.trim(),
        contactChannel = input.contactChannel?.trim()?.takeIf { it.isNotEmpty() },
        notes = input.notes?.trim()?.takeIf { it.isNotEmpty() },
    )

    fun validate(input: CaptureInput, intent: SaveIntent): List<FieldIssue> {
        val value = normalize(input)
        val issues = mutableListOf<FieldIssue>()
        val complete = intent == SaveIntent.CONFIRM_REGISTRATION
        fun text(field: CaptureField, text: String, min: Int, max: Int) {
            val length = text.codePointCount(0, text.length)
            when {
                complete && min > 0 && text.isEmpty() -> issues += FieldIssue(field, ValidationCode.REQUIRED)
                length > max || (complete && length < min) -> issues += FieldIssue(field, ValidationCode.LENGTH)
            }
        }
        text(CaptureField.NAME, value.fullName, CaptureLimits.NAME_MIN, CaptureLimits.NAME_MAX)
        text(CaptureField.ESTABLISHMENT, value.establishment, CaptureLimits.ESTABLISHMENT_MIN, CaptureLimits.ESTABLISHMENT_MAX)
        text(CaptureField.EMAIL, value.email, 1, CaptureLimits.EMAIL_MAX)
        text(CaptureField.NOTES, value.notes.orEmpty(), 0, CaptureLimits.NOTES_MAX)
        // Los borradores pueden retener entradas incompletas para corregirlas después.
        if (complete) {
            if (!RutValidator.isValid(value.rut)) issues += FieldIssue(CaptureField.RUT, ValidationCode.FORMAT)
            if (value.email.isNotEmpty() && !ContactFormats.validEmail(value.email)) issues += FieldIssue(CaptureField.EMAIL, ValidationCode.FORMAT)
            if (!Regex("\\+569[0-9]{8}").matches(value.phone)) issues += FieldIssue(CaptureField.PHONE, ValidationCode.FORMAT)
        }
        if (value.educationLevel.isEmpty()) {
            if (complete) issues += FieldIssue(CaptureField.EDUCATION, ValidationCode.REQUIRED)
        } else if (value.educationLevel !in educationLevels) {
            issues += FieldIssue(CaptureField.EDUCATION, ValidationCode.UNKNOWN_OPTION)
        }
        if (value.contactChannel != null && value.contactChannel !in contactChannels) {
            issues += FieldIssue(CaptureField.CHANNEL, ValidationCode.UNKNOWN_OPTION)
        }
        return issues.toList()
    }
}
