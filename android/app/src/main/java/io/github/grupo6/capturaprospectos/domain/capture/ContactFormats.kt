package io.github.grupo6.capturaprospectos.domain.capture

/** Formato compartido; no verifica existencia ni titularidad del correo. */
internal object ContactFormats {
    fun validEmail(email: String): Boolean {
        if (email.any { it.isWhitespace() } || email.count { it == '@' } != 1) return false
        val (local, domain) = email.split('@')
        if (local.isEmpty() || local.startsWith('.') || local.endsWith('.') || ".." in local) return false
        if (!Regex("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+").matches(local)) return false
        val labels = domain.split('.')
        return labels.size >= 2 && labels.all {
            it.isNotEmpty() && !it.startsWith('-') && !it.endsWith('-') && Regex("[A-Za-z0-9-]+").matches(it)
        }
    }
}
