package io.github.grupo6.capturaprospectos.domain.device

import io.github.grupo6.capturaprospectos.domain.capture.RutValidator

data class QrCandidate(val fullName: String, val rut: String) {
    override fun toString() = "QrCandidate(redacted)"
}
sealed interface QrResult {
    data class Candidate(val value: QrCandidate) : QrResult
    data object Unsupported : QrResult
    data object Invalid : QrResult
}
fun interface QrParser { fun parse(payload: String): QrResult }

/** Sólo el fixture sintético G6DEMO:1. No ejecuta URLs ni interpreta cédulas. */
class DemoQrParser : QrParser {
    override fun parse(payload: String): QrResult {
        if (payload.length > 512) return QrResult.Invalid
        if (!payload.startsWith("G6DEMO:1:")) return QrResult.Unsupported
        val fields = try { StringObjectReader(payload.substring(9)).read() }
        catch (_: InvalidJson) { return QrResult.Invalid }
        if (fields.keys != setOf("fullName", "rut")) return QrResult.Invalid
        val name = fields.getValue("fullName").trim()
        val rut = RutValidator.normalize(fields.getValue("rut"))
        if (name.length !in 2..120 || name.any { it.isISOControl() } || !RutValidator.isValid(rut)) return QrResult.Invalid
        return QrResult.Candidate(QrCandidate(name, rut))
    }
}

private class InvalidJson : Exception()
/** Subconjunto deliberadamente cerrado: objeto de cadenas JSON; no arrays, números o anidación. */
private class StringObjectReader(private val source: String) {
    private var index = 0
    private fun fail(): Nothing = throw InvalidJson()
    private fun whitespace() { while (index < source.length && source[index] in " \t\r\n") index++ }
    private fun take(c: Char) { whitespace(); if (index >= source.length || source[index++] != c) fail() }
    fun read(): Map<String, String> {
        take('{'); whitespace()
        val fields = linkedMapOf<String, String>()
        if (index < source.length && source[index] != '}') {
            while (true) {
                val key = string(); take(':'); val value = string()
                if (fields.put(key, value) != null) fail()
                whitespace()
                if (index >= source.length || source[index] != ',') break
                index++
            }
        }
        take('}'); whitespace(); if (index != source.length) fail()
        return fields
    }
    private fun string(): String {
        take('"'); val value = StringBuilder()
        while (index < source.length) {
            val c = source[index++]
            if (c == '"') {
                val result = value.toString()
                var p = 0
                while (p < result.length) {
                    val unit = result[p++]
                    if (unit.isHighSurrogate()) {
                        if (p >= result.length || !result[p++].isLowSurrogate()) fail()
                    } else if (unit.isLowSurrogate()) fail()
                }
                return result
            }
            if (c < ' ') fail()
            if (c != '\\') { value.append(c); continue }
            if (index >= source.length) fail()
            value.append(when (val escape = source[index++]) {
                '"', '\\', '/' -> escape
                'b' -> '\b'; 'f' -> '\u000C'; 'n' -> '\n'; 'r' -> '\r'; 't' -> '\t'
                'u' -> {
                    if (index + 4 > source.length) fail()
                    val digits = source.substring(index, index + 4)
                    // JSON limita los escapes a dígitos hexadecimales ASCII.
                    if (digits.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) fail()
                    index += 4; digits.toInt(16).toChar()
                }
                else -> fail()
            })
        }
        fail()
    }
}
