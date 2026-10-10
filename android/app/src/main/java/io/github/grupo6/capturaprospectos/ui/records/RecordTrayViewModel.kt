package io.github.grupo6.capturaprospectos.ui.records

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.grupo6.capturaprospectos.domain.capture.CaptureState
import io.github.grupo6.capturaprospectos.domain.records.*
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecordTrayUiState(
    val items: List<RecordSummary> = emptyList(),
    val filter: RecordFilter = RecordFilter(),
    val isLoading: Boolean = false,
    val message: String? = null,
    val nextCursor: RecordCursor? = null,
    val targetForCancellation: RecordSummary? = null,
) {
    override fun toString() = "RecordTrayUiState(count=${items.size}, redacted)"
}

class RecordTrayViewModel(
    private val repository: RecordRepository?,
) : ViewModel() {
    private val mutableState = MutableStateFlow(RecordTrayUiState())
    val uiState = mutableState.asStateFlow()

    init {
        load()
    }

    @MainThread
    fun refresh(): Job = load()

    @MainThread
    fun updateQuery(text: String): Job {
        mutableState.update { it.copy(filter = it.filter.copy(query = text), message = null) }
        return load()
    }

    @MainThread
    fun filterState(state: CaptureState?): Job {
        val newStates = if (state == null) emptySet() else setOf(state)
        mutableState.update { it.copy(filter = it.filter.copy(states = newStates), message = null) }
        return load()
    }

    @MainThread
    fun openCancelDialog(record: RecordSummary) {
        mutableState.update { it.copy(targetForCancellation = record, message = null) }
    }

    @MainThread
    fun closeCancelDialog() {
        mutableState.update { it.copy(targetForCancellation = null) }
    }

    @MainThread
    fun confirmCancellation(reason: String): Job? {
        val target = uiState.value.targetForCancellation ?: return null
        val repo = repository ?: run {
            mutableState.update { it.copy(message = "Repositorio local no disponible.", targetForCancellation = null) }
            return null
        }
        val trimmedReason = reason.trim()
        if (trimmedReason.length !in 5..300) {
            mutableState.update { it.copy(message = "El motivo debe tener entre 5 y 300 caracteres.") }
            return null
        }

        mutableState.update { it.copy(isLoading = true, message = null) }
        return viewModelScope.launch {
            val result = repo.cancel(CancelRecord(target.id, target.revision, trimmedReason))
            when (result) {
                is RecordMutationResult.Saved -> {
                    mutableState.update {
                        it.copy(
                            targetForCancellation = null,
                            message = "Inscripción anulada correctamente. Se liberó la reserva del RUT.",
                        )
                    }
                    load()
                }
                RecordMutationResult.ReasonRequired, RecordMutationResult.InvalidReason -> {
                    mutableState.update { it.copy(isLoading = false, message = "Motivo de anulación inválido (5 a 300 caracteres).") }
                }
                RecordMutationResult.RevisionConflict -> {
                    mutableState.update {
                        it.copy(
                            isLoading = false,
                            targetForCancellation = null,
                            message = "El registro fue modificado externamente. La lista se actualizará.",
                        )
                    }
                    load()
                }
                RecordMutationResult.InvalidTransition -> {
                    mutableState.update {
                        it.copy(
                            isLoading = false,
                            targetForCancellation = null,
                            message = "No es posible anular este registro en su estado actual.",
                        )
                    }
                }
                RecordMutationResult.Unavailable -> {
                    mutableState.update {
                        it.copy(
                            isLoading = false,
                            targetForCancellation = null,
                            message = "Registro no disponible o no autorizado.",
                        )
                    }
                }
                RecordMutationResult.StorageUnavailable -> {
                    mutableState.update {
                        it.copy(
                            isLoading = false,
                            message = "Almacenamiento local no disponible. Intenta nuevamente.",
                        )
                    }
                }
                else -> {
                    mutableState.update { it.copy(isLoading = false, message = "No se pudo anular el registro.") }
                }
            }
        }
    }

    private fun load(): Job = viewModelScope.launch {
        val repo = repository ?: run {
            mutableState.update { it.copy(isLoading = false, items = emptyList()) }
            return@launch
        }
        mutableState.update { it.copy(isLoading = true) }
        val query = RecordQuery(filter = mutableState.value.filter, limit = 50)
        when (val result = repo.list(query)) {
            is RecordPageResult.Page -> {
                mutableState.update { it.copy(items = result.items, nextCursor = result.next, isLoading = false) }
            }
            RecordPageResult.AccessDenied -> {
                mutableState.update { it.copy(items = emptyList(), isLoading = false, message = "Acceso no autorizado para este perfil.") }
            }
            RecordPageResult.InvalidQuery -> {
                mutableState.update { it.copy(items = emptyList(), isLoading = false, message = "Filtro de búsqueda inválido.") }
            }
            RecordPageResult.StorageUnavailable -> {
                mutableState.update { it.copy(isLoading = false, message = "Almacenamiento local no disponible al leer la bandeja.") }
            }
        }
    }
}
