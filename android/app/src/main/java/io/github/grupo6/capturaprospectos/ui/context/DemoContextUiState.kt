package io.github.grupo6.capturaprospectos.ui.context

import io.github.grupo6.capturaprospectos.domain.context.DemoContextState

/** Estado recibido: null significa lectura inicial. La pantalla no abre ni inicializa Room. */
data class DemoContextUiState(
    val context: DemoContextState? = null,
    val busy: Boolean = false,
    val message: String? = null,
)

