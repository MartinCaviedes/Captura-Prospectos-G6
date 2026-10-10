package io.github.grupo6.capturaprospectos.ui.context

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.grupo6.capturaprospectos.domain.admission.AdmissionRepository
import io.github.grupo6.capturaprospectos.domain.context.ContextSession
import io.github.grupo6.capturaprospectos.domain.context.ContextOpenFailure
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface ContextSessionState {
    data object Opening : ContextSessionState
    data object Unavailable : ContextSessionState
    data class Ready(
        val viewModel: DemoContextViewModel,
        val admissionRepository: AdmissionRepository? = null,
    ) : ContextSessionState
}

/** Dueño retenido por Activity: primero cancela consumidores y después cierra la conexión. */
class ContextSessionOwner(
    private val open: () -> ContextSession,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val state = MutableStateFlow<ContextSessionState>(ContextSessionState.Opening)
    val uiState = state.asStateFlow()
    private var opening: Job? = null
    init { start() }

    @MainThread
    fun retry() {
        if (state.value == ContextSessionState.Unavailable && opening?.isActive != true) start()
    }

    private fun start() {
        state.value = ContextSessionState.Opening
        opening = viewModelScope.launch {
            var resource: ContextSession? = null
            var consumer: DemoContextViewModel? = null
            val children = ViewModelStore()
            try {
                // Asignar dentro de IO permite cerrar incluso si hay cancelación al volver a Main.
                withContext(io) { resource = open() }
                consumer = ViewModelProvider(children, object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        require(modelClass == DemoContextViewModel::class.java)
                        return DemoContextViewModel(requireNotNull(resource).repository) as T
                    }
                })[DemoContextViewModel::class.java]
                state.value = ContextSessionState.Ready(consumer, resource?.admissionRepository)
                awaitCancellation()
            } catch (_: ContextOpenFailure) {
                state.value = ContextSessionState.Unavailable
            } finally {
                withContext(NonCancellable) {
                    children.clear()
                    consumer?.viewModelScope?.coroutineContext?.get(Job)?.join()
                    withContext(io) { resource?.close() }
                }
            }
        }
    }
}
