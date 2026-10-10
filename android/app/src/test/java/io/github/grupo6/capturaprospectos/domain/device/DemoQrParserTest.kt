package io.github.grupo6.capturaprospectos.domain.device

import org.junit.Test
import org.junit.Assert.*

class DemoQrParserTest {
    private val parser = DemoQrParser()
    private val valid = "G6DEMO:1:{\"fullName\":\"Persona Ficticia Alfa\",\"rut\":\"12345678-5\"}"
    @Test fun returnsOnlyCandidateAndRedactsDiagnostics() {
        val result = parser.parse(valid) as QrResult.Candidate
        assertEquals("Persona Ficticia Alfa", result.value.fullName)
        assertEquals("12345678-5", result.value.rut)
        assertFalse(result.toString().contains("12345678"))
    }
    @Test fun rejectsVersionsUrlsAndInstitutionalPayloads() {
        listOf("https://example.test", valid.replace("DEMO:1", "DEMO:2"), "RUN=12345678-5").forEach {
            assertEquals(QrResult.Unsupported, parser.parse(it))
        }
    }
    @Test fun rejectsUnknownFieldsDuplicatesAndWrongTypes() {
        listOf(valid.replace("}", ",\"birthDate\":\"2000-01-01\"}"),
            valid.replace("}", ",\"rut\":\"12345678-5\"}"),
            valid.replace("\"12345678-5\"", "null"), valid.replace("\"12345678-5\"", "123"),
            valid.replace("\"12345678-5\"", "{}"), valid.replace("\"fullName\"", "\"other\""))
            .forEach { assertEquals(QrResult.Invalid, parser.parse(it)) }
    }
    @Test fun rejectsMalformedTrailingDataAndWrongCheckDigit() {
        listOf(valid.dropLast(1), "$valid trailing", valid.replace("}", ",}"),
            valid.replace("12345678-5", "12345678-0"), valid.replace("Persona Ficticia Alfa", "A"),
            valid.replace("Persona Ficticia Alfa", "\\uD800"), valid.replace("Persona Ficticia Alfa", "\\n"))
            .forEach { assertEquals(QrResult.Invalid, parser.parse(it)) }
    }
    @Test fun handlesWhitespaceEscapedUnicodeAndFieldOrder() {
        val value = parser.parse("G6DEMO:1: { \"rut\":\"12.345.678-5\", \"fullName\":\"Ficticio Jos\\u00e9\" } ") as QrResult.Candidate
        assertEquals("Ficticio José", value.value.fullName)
        assertEquals("12345678-5", value.value.rut)
    }
    @Test fun rejectsNonAsciiHexDigitsInUnicodeEscapes() {
        listOf("\\u００４１", "\\u٠٠٤١", "\\u00ＦＦ").forEach { escape ->
            assertEquals(QrResult.Invalid, parser.parse(valid.replace("Persona Ficticia Alfa", "Persona $escape")))
        }
    }
    @Test fun acceptsAsciiHexAndSurrogatePairs() {
        listOf("\\u0041", "\\u00EF", "\\uD83D\\uDE00").forEach { escape ->
            assertTrue(parser.parse(valid.replace("Persona Ficticia Alfa", "Persona $escape")) is QrResult.Candidate)
        }
    }
    @Test fun limitsPayloadBeforeParsing() {
        assertEquals(QrResult.Invalid, parser.parse(valid + " ".repeat(512)))
        assertEquals(QrResult.Invalid, parser.parse(valid.replace("Persona Ficticia Alfa", "A".repeat(121))))
    }
}
