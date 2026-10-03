package io.github.grupo6.capturaprospectos.domain.capture

import org.junit.Assert.*
import org.junit.Test

class ContactValidatorTest {
    private val validator = ContactValidator(setOf("SECONDARY"), setOf("EMAIL"))
    private val valid = CaptureInput(
        fullName = "Persona Ficticia Alfa", rut = "12.345.678-5", email = "alfa@example.invalid",
        phone = "+56 9 0000 0000", establishment = "Establecimiento ficticio",
    )
    private fun errors(input: CaptureInput) = validator.validate(input, SaveIntent.CONFIRM_REGISTRATION)

    @Test fun normalizesPresentationWithoutChangingNameOrEmailCase() {
        val result = validator.normalize(valid.copy(fullName = "  María José  ", email = " Alfa@example.invalid "))
        assertEquals("María José", result.fullName)
        assertEquals("Alfa@example.invalid", result.email)
        assertEquals("12345678-5", result.rut)
        assertEquals("+56900000000", result.phone)
        assertTrue(errors(valid).isEmpty())
    }

    @Test fun rutAcceptsFormattedCompactAndLowercaseCheckDigit() {
        assertTrue(RutValidator.isValid("12.345.678-5"))
        assertTrue(RutValidator.isValid("123456785"))
        assertTrue(RutValidator.isValid("6-k"))
    }

    @Test fun rutRejectsWrongCheckDigitZeroAndAmbiguousCharacters() {
        listOf("12345678-0", "0-0", "012345678-5", "12a345678-5", "12345678--5", "", "１２３-5").forEach {
            assertFalse(RutValidator.isValid(it))
        }
    }

    @Test fun nameAndEstablishmentBoundariesAreEnforced() {
        assertTrue(errors(valid.copy(fullName = "Á".repeat(120), establishment = "E".repeat(160))).isEmpty())
        assertTrue(errors(valid.copy(fullName = "Á".repeat(121))).any { it.field == CaptureField.NAME })
        assertTrue(errors(valid.copy(establishment = "E".repeat(161))).any { it.field == CaptureField.ESTABLISHMENT })
        assertTrue(errors(valid.copy(fullName = " A ")).any { it.code == ValidationCode.LENGTH })
    }

    @Test fun unicodeNamesAreNotRestrictedToAsciiOrTwoSurnames() {
        assertTrue(errors(valid.copy(fullName = "李明")).isEmpty())
        assertTrue(errors(valid.copy(fullName = "José")).isEmpty())
    }

    @Test fun optionalNotesHaveLengthLimitButAreNotRequired() {
        assertTrue(errors(valid.copy(notes = null)).isEmpty())
        assertTrue(errors(valid.copy(notes = " ")).isEmpty())
        assertTrue(errors(valid.copy(notes = "x".repeat(500))).isEmpty())
        assertTrue(errors(valid.copy(notes = "x".repeat(501))).any { it.field == CaptureField.NOTES })
    }

    @Test fun emailRejectsMalformedStructuresAndExcessLength() {
        listOf("a@@example.invalid", "a b@example.invalid", "a..b@example.invalid", "a@-example.invalid", "a@example", "a@.invalid", "x".repeat(255)+"@example.invalid").forEach {
            assertTrue(errors(valid.copy(email = it)).any { issue -> issue.field == CaptureField.EMAIL })
        }
        assertTrue(errors(valid.copy(email = "alfa+demo@example.invalid")).isEmpty())
    }

    @Test fun phoneUsesOnlyDeclaredChileanMobileRule() {
        listOf("+5690000000", "+569000000000", "+56200000000", "+54900000000", "+569abcdefgh").forEach {
            assertTrue(errors(valid.copy(phone = it)).any { issue -> issue.field == CaptureField.PHONE })
        }
    }

    @Test fun educationAndChannelComeFromSuppliedOptions() {
        assertTrue(errors(valid.copy(educationLevel = "UNKNOWN")).any { it.field == CaptureField.EDUCATION })
        assertTrue(errors(valid.copy(contactChannel = "UNKNOWN")).any { it.field == CaptureField.CHANNEL })
        assertTrue(errors(valid.copy(contactChannel = "EMAIL")).isEmpty())
    }

    @Test fun draftAllowsIncompleteFieldsButKeepsLengthLimits() {
        assertTrue(validator.validate(CaptureInput(rut = "12-", email = "a@"), SaveIntent.SAVE_DRAFT).isEmpty())
        assertTrue(validator.validate(valid.copy(notes = "x".repeat(501)), SaveIntent.SAVE_DRAFT).isNotEmpty())
    }

    @Test fun errorAndInputDescriptionsDoNotEchoPersonalValues() {
        assertFalse(valid.toString().contains(valid.fullName))
        assertFalse(errors(valid.copy(email = "private value")).toString().contains("private value"))
    }
}
