package io.github.grupo6.capturaprospectos.ui.context

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.grupo6.capturaprospectos.domain.context.ContextChange
import io.github.grupo6.capturaprospectos.domain.context.DemoContextRepository
import io.github.grupo6.capturaprospectos.domain.context.DemoContextState
import java.util.UUID
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Recibe un repositorio ya abierto; no crea Room, fixtures ni selección por su cuenta. */
class DemoContextViewModel(private val repository: DemoContextRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(DemoContextUiState())
    val uiState = mutableState.asStateFlow()
    private var observation: Job? = null

    init { observe() }

    private fun observe() {
        observation = viewModelScope.launch {
            repository.observe().collect { context ->
                mutableState.update { it.copy(context = context) }
            }
        }
    }

    @MainThread
    fun initialize(): Job? = if (uiState.value.context == DemoContextState.NotInitialized) {
        perform { repository.initialize() }
    } else null

    @MainThread
    fun selectProfile(id: UUID): Job? = when (uiState.value.context) {
        is DemoContextState.Ready -> perform { repository.selectProfile(id) }
        else -> null
    }

    @MainThread
    fun selectEvent(id: UUID): Job? = when (uiState.value.context) {
        is DemoContextState.Ready -> perform { repository.selectEvent(id) }
        else -> null
    }

    @MainThread
    fun clearSelection(): Job? = when (uiState.value.context) {
        is DemoContextState.Ready -> perform { repository.clearSelection() }
        else -> null
    }

    @MainThread
    fun saveEvent(
        command: io.github.grupo6.capturaprospectos.domain.context.SaveDemoEvent,
        onResult: (io.github.grupo6.capturaprospectos.domain.context.SaveEventResult) -> Unit = {},
    ): Job? {
        if (uiState.value.busy || !viewModelScope.isActive) return null
        mutableState.update { it.copy(busy = true, message = null) }
        return viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                observation?.cancelAndJoin()
                val result = repository.saveEvent(command)
                val context = repository.snapshot()
                val msg = when (result) {
                    is io.github.grupo6.capturaprospectos.domain.context.SaveEventResult.Success ->
                        "Evento guardado correctamente en este dispositivo."
                    io.github.grupo6.capturaprospectos.domain.context.SaveEventResult.Unauthorized ->
                        "No tienes permisos de supervisor para administrar eventos en esta sede."
                    is io.github.grupo6.capturaprospectos.domain.context.SaveEventResult.Invalid ->
                        result.issues.joinToString(" ")
                    io.github.grupo6.capturaprospectos.domain.context.SaveEventResult.StorageUnavailable ->
                        "No se pudo guardar el evento localmente. Reintenta."
                }
                mutableState.update { it.copy(context = context, message = msg) }
                onResult(result)
            } finally {
                mutableState.update { it.copy(busy = false) }
                if (viewModelScope.isActive) observe()
            }
        }
    }

    @MainThread
    fun retry(): Job? = perform { null }

    /** Una acción en curso descarta nuevos clics: no encola eventos contra un perfil obsoleto. */
    private fun perform(operation: suspend () -> ContextChange?): Job? {
        if (uiState.value.busy || !viewModelScope.isActive) return null
        mutableState.update { it.copy(busy = true, message = null) }
        return viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                // Evita que una lectura previa a la mutación sobrescriba su resultado confirmado.
                observation?.cancelAndJoin()
                val result = operation()
                val context = repository.snapshot()
                mutableState.update { it.copy(context = context, message = result.message()) }
            } finally {
                // No capturar CancellationException ni convertir defectos de programación en éxito.
                mutableState.update { it.copy(busy = false) }
                if (viewModelScope.isActive) observe()
            }
        }
    }
}

private fun ContextChange?.message(): String? = when (this) {
    null, ContextChange.APPLIED -> null
    ContextChange.ALREADY_INITIALIZED -> "La demo ya estaba preparada; se conservó su contexto."
    ContextChange.INVALID_SELECTION -> "La selección no es válida para el contexto actual. Revisa el perfil y el evento."
    ContextChange.NOT_INITIALIZED -> "Primero prepara la demo local."
    ContextChange.INCOMPATIBLE_SEED -> "El contexto existente requiere revisión. No se borraron ni reemplazaron sus datos."
    ContextChange.STORAGE_UNAVAILABLE -> "No se pudo completar la operación local. Los datos se conservan; reintenta la lectura."
}
