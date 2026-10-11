package io.github.grupo6.capturaprospectos.ui.context

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.grupo6.capturaprospectos.domain.admission.AdmissionRepository
import io.github.grupo6.capturaprospectos.domain.records.RecordRepository
import io.github.grupo6.capturaprospectos.domain.admission.AdmissionEnvironment
import io.github.grupo6.capturaprospectos.domain.context.EstablishmentSuggestion
import io.github.grupo6.capturaprospectos.ui.admission.AdmissionFormViewModel
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
        val recordRepository: RecordRepository? = null,
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
    private val forms = ViewModelStore()
    private var formKey: String? = null
    private var formConsumer: AdmissionFormViewModel? = null
    init { start() }

    /** Retiene sólo memoria en recreaciones de Activity; nunca utiliza SavedState para PII. */
    @MainThread
    fun form(environment: AdmissionEnvironment, suggestion: EstablishmentSuggestion?): AdmissionFormViewModel {
        val key = "${environment.context.actor.id}/${environment.context.event.id}"
        if (key != formKey) {
            check(releaseForm()) { "No cambiar contexto durante el guardado" }
            formKey = key
        }
        return ViewModelProvider(forms, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass == AdmissionFormViewModel::class.java)
                val ready = state.value as ContextSessionState.Ready
                return AdmissionFormViewModel(environment, suggestion, repository = ready.admissionRepository) as T
            }
        })[AdmissionFormViewModel::class.java].also { formConsumer = it }
    }

    /** Una salida explícita espera el resultado del commit antes de permitir descartar. */
    @MainThread
    fun releaseForm(): Boolean {
        if (formConsumer?.uiState?.value?.isSaving == true) return false
        forms.clear()
        formConsumer = null
        formKey = null
        return true
    }

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
                state.value = ContextSessionState.Ready(consumer, resource?.admissionRepository, resource?.recordRepository)
                awaitCancellation()
            } catch (_: ContextOpenFailure) {
                state.value = ContextSessionState.Unavailable
            } finally {
                withContext(NonCancellable) {
                    val pendingForm = formConsumer
                    forms.clear()
                    pendingForm?.viewModelScope?.coroutineContext?.get(Job)?.join()
                    formConsumer = null
                    formKey = null
                    children.clear()
                    consumer?.viewModelScope?.coroutineContext?.get(Job)?.join()
                    withContext(io) { resource?.close() }
                }
            }
        }
    }
}
