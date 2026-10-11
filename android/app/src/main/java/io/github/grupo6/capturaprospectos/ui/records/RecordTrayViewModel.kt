package io.github.grupo6.capturaprospectos.ui.records

import java.util.UUID
import io.github.grupo6.capturaprospectos.domain.admission.*
import io.github.grupo6.capturaprospectos.domain.capture.SaveIntent
import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.grupo6.capturaprospectos.domain.capture.CaptureState
import io.github.grupo6.capturaprospectos.domain.records.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class RecordTrayUiState(
    val selectedRecord: StoredAdmission? = null, val isDetailOpen: Boolean = false,
    val isEditing: Boolean = false, val isSavingEdit: Boolean = false,
    val items: List<RecordSummary> = emptyList(), val filter: RecordFilter = RecordFilter(),
    val isLoading: Boolean = false, val isMutating: Boolean = false, val message: String? = null,
    val nextCursor: RecordCursor? = null, val targetForCancellation: RecordSummary? = null,
) { override fun toString() = "RecordTrayUiState(count=${items.size}, redacted)" }

/** Cancela lecturas obsoletas; un commit en curso no se descarta ni se duplica desde la UI. */
class RecordTrayViewModel(private val repository: RecordRepository?) : ViewModel() {
    private val mutableState = MutableStateFlow(RecordTrayUiState())
    val uiState = mutableState.asStateFlow()
    private var reading: Job? = null
    init { load() }

    @MainThread fun refresh(): Job? = if (uiState.value.isMutating) null else load()
    @MainThread fun updateQuery(text: String): Job? {
        if (uiState.value.isMutating) return null
        mutableState.update { it.copy(filter = it.filter.copy(query = text), message = null) }
        return load()
    }
    @MainThread fun filterState(state: CaptureState?): Job? {
        if (uiState.value.isMutating) return null
        mutableState.update { it.copy(filter = it.filter.copy(states = state?.let(::setOf).orEmpty()), message = null) }
        return load()
    }
    @MainThread
    fun openDetail(id: UUID): Job? {
        val repo = repository ?: run {
            mutableState.update { it.copy(message = "Repositorio local no disponible.") }
            return null
        }
        mutableState.update { it.copy(isLoading = true, message = null) }
        return viewModelScope.launch {
            when (val result = repo.get(id)) {
                is ReadAdmissionResult.Found -> {
                    mutableState.update {
                        it.copy(
                            selectedRecord = result.capture,
                            isDetailOpen = true,
                            isEditing = false,
                            isLoading = false,
                        )
                    }
                }
                is ReadAdmissionResult.Legacy -> {
                    mutableState.update {
                        it.copy(
                            isLoading = false,
                            message = "El registro pertenece a una versión anterior y no tiene vista detallada F2027.",
                        )
                    }
                }
                ReadAdmissionResult.Unavailable -> {
                    mutableState.update {
                        it.copy(
                            isLoading = false,
                            message = "Registro no disponible o fuera de su alcance autorizado.",
                        )
                    }
                }
                ReadAdmissionResult.StorageUnavailable -> {
                    mutableState.update {
                        it.copy(
                            isLoading = false,
                            message = "Almacenamiento local no disponible al leer el detalle.",
                        )
                    }
                }
            }
        }
    }

    @MainThread
    fun closeDetail() {
        mutableState.update {
            it.copy(
                selectedRecord = null,
                isDetailOpen = false,
                isEditing = false,
            )
        }
    }

    @MainThread
    fun startEditing() {
        mutableState.update { it.copy(isEditing = true) }
    }

    @MainThread
    fun cancelEditing() {
        mutableState.update { it.copy(isEditing = false) }
    }

    @MainThread
    fun saveEdit(editedInput: AdmissionInput, reason: String?): Job? {
        val selected = uiState.value.selectedRecord ?: return null
        val repo = repository ?: run {
            mutableState.update { it.copy(message = "Repositorio local no disponible.") }
            return null
        }

        mutableState.update { it.copy(isSavingEdit = true, message = null) }
        return viewModelScope.launch {
            val command = EditAdmission(
                id = selected.id,
                expectedRevision = selected.revision,
                input = editedInput,
                intent = if (selected.state == CaptureState.DRAFT) SaveIntent.SAVE_DRAFT else SaveIntent.CONFIRM_REGISTRATION,
                location = selected.location,
                reason = reason,
            )
            val result = repo.edit(command)
            when (result) {
                is RecordMutationResult.Saved -> {
                    mutableState.update {
                        it.copy(
                            isSavingEdit = false,
                            isEditing = false,
                            message = "Inscripción modificada correctamente.",
                        )
                    }
                    // Recargar detalle actualizado y refrescar bandeja
                    openDetail(selected.id)
                    load()
                }
                RecordMutationResult.ReasonRequired, RecordMutationResult.InvalidReason -> {
                    mutableState.update {
                        it.copy(
                            isSavingEdit = false,
                            message = "El motivo de supervisión debe tener entre 5 y 300 caracteres.",
                        )
                    }
                }
                RecordMutationResult.RevisionConflict -> {
                    mutableState.update {
                        it.copy(
                            isSavingEdit = false,
                            message = "Conflicto de edición: el registro fue modificado externamente.",
                        )
                    }
                    openDetail(selected.id)
                    load()
                }
                RecordMutationResult.InvalidTransition -> {
                    mutableState.update {
                        it.copy(
                            isSavingEdit = false,
                            message = "No es posible modificar este registro en su estado actual o intentar cambiar el RUT confirmado.",
                        )
                    }
                }
                RecordMutationResult.StorageUnavailable -> {
                    mutableState.update {
                        it.copy(
                            isSavingEdit = false,
                            message = "Almacenamiento no disponible al guardar la modificación.",
                        )
                    }
                }
                else -> {
                    mutableState.update {
                        it.copy(
                            isSavingEdit = false,
                            message = "No fue posible guardar las modificaciones.",
                        )
                    }
                }
            }
        }
    }

    @MainThread fun loadMore(): Job? {
        if (uiState.value.isLoading || uiState.value.isMutating || uiState.value.nextCursor == null) return null
        return load(append = true)
    }
    @MainThread fun openCancelDialog(record: RecordSummary) {
        if (uiState.value.isMutating || record.state == CaptureState.CANCELLED) return
        mutableState.update { it.copy(targetForCancellation = record, message = null) }
    }
    @MainThread fun closeCancelDialog() {
        if (!uiState.value.isMutating) mutableState.update { it.copy(targetForCancellation = null) }
    }
    @MainThread fun confirmCancellation(reason: String): Job? {
        val current = uiState.value
        if (current.isMutating) return null
        val target = current.targetForCancellation ?: return null
        val repo = repository ?: run {
            mutableState.update { it.copy(message = "Repositorio local no disponible.") }; return null
        }
        val trimmed = reason.trim()
        if (trimmed.codePointCount(0, trimmed.length) !in 5..300) {
            mutableState.update { it.copy(message = "El motivo debe tener entre 5 y 300 caracteres.") }; return null
        }
        reading?.cancel()
        mutableState.update { it.copy(isMutating = true, isLoading = false, message = null) }
        return viewModelScope.launch {
            val result = repo.cancel(CancelRecord(target.id, target.revision, trimmed))
            val message = when (result) {
                is RecordMutationResult.Saved -> "Inscripción anulada correctamente. Se liberó la reserva del RUT."
                RecordMutationResult.RevisionConflict -> "El registro fue modificado externamente. Actualiza la lista antes de volver a anular."
                RecordMutationResult.Unavailable -> "Registro no disponible o no autorizado."
                RecordMutationResult.InvalidTransition -> "El estado del registro no permite anulación."
                RecordMutationResult.StorageUnavailable -> "Almacenamiento no disponible. El motivo se conserva para reintentar."
                else -> "No se pudo anular. Revisa el motivo y la revisión."
            }
            mutableState.update { it.copy(isMutating = false, message = message,
                targetForCancellation = if (result is RecordMutationResult.Saved || result == RecordMutationResult.RevisionConflict ||
                    result == RecordMutationResult.Unavailable) null else target) }
            if (result is RecordMutationResult.Saved || result == RecordMutationResult.RevisionConflict || result == RecordMutationResult.Unavailable) load()
        }
    }
    private fun load(append: Boolean = false): Job {
        reading?.cancel()
        val filter = uiState.value.filter
        val cursor = if (append) uiState.value.nextCursor else null
        mutableState.update { it.copy(isLoading = true,
            items = if (append) it.items else emptyList(), nextCursor = if (append) it.nextCursor else null) }
        return viewModelScope.launch {
            val result = repository?.list(RecordQuery(filter, cursor, 50)) ?: RecordPageResult.StorageUnavailable
            currentCoroutineContext().ensureActive()
            when (result) {
                is RecordPageResult.Page -> mutableState.update { it.copy(isLoading = false,
                    items = (if (append) it.items + result.items else result.items).distinctBy { row -> row.id }, nextCursor = result.next) }
                RecordPageResult.AccessDenied -> mutableState.update { it.copy(isLoading = false, items = emptyList(), nextCursor = null,
                    message = "Acceso no autorizado para este perfil.") }
                RecordPageResult.InvalidQuery -> mutableState.update { it.copy(isLoading = false, items = emptyList(), nextCursor = null,
                    message = "Filtro de búsqueda inválido.") }
                RecordPageResult.StorageUnavailable -> mutableState.update { it.copy(isLoading = false,
                    message = "Almacenamiento local no disponible al leer la bandeja.") }
            }
        }.also { reading = it }
    }
    override fun onCleared() { mutableState.value = RecordTrayUiState() }
}
