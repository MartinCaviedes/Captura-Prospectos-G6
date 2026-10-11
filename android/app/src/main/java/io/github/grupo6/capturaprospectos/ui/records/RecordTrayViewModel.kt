package io.github.grupo6.capturaprospectos.ui.records

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelProvider
import io.github.grupo6.capturaprospectos.domain.device.LocationProvider
import io.github.grupo6.capturaprospectos.ui.admission.AdmissionFormViewModel
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
    val editForm: AdmissionFormViewModel? = null, val editReason: String = "", val editConflict: Boolean = false,
    val confirmEditDiscard: Boolean = false, val reloadAfterDiscard: Boolean = false,
    val isReadingDetail: Boolean = false, val cancellationReason: String = "",
    val items: List<RecordSummary> = emptyList(), val filter: RecordFilter = RecordFilter(),
    val isLoading: Boolean = false, val isMutating: Boolean = false, val message: String? = null,
    val nextCursor: RecordCursor? = null, val targetForCancellation: RecordSummary? = null,
) { override fun toString() = "RecordTrayUiState(count=${items.size}, redacted)" }

/** Cancela lecturas obsoletas; un commit en curso no se descarta ni se duplica desde la UI. */
class RecordTrayViewModel(private val repository: RecordRepository?, private val locationProvider: LocationProvider? = null) : ViewModel() {
    private val mutableState = MutableStateFlow(RecordTrayUiState())
    val uiState = mutableState.asStateFlow()
    private var reading: Job? = null
    private var detailReading: Job? = null
    private val editors = ViewModelStore()
    private var active = true
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
    /** Una lectura tardía no reabre el detalle después de salir o cambiar de registro. */
    @MainThread fun openDetail(id: UUID): Job? {
        if (!active || uiState.value.isMutating || uiState.value.isEditing) return null
        val repo = repository ?: return null
        detailReading?.cancel()
        mutableState.update { it.copy(isReadingDetail = true) }
        return viewModelScope.launch {
            val result = repo.get(id)
            currentCoroutineContext().ensureActive()
            when (result) {
                is ReadAdmissionResult.Found -> mutableState.update { it.copy(selectedRecord = result.capture,
                    isDetailOpen = true, isReadingDetail = false) }
                is ReadAdmissionResult.Legacy -> mutableState.update { it.copy(isReadingDetail = false,
                    message = "Registro histórico: edición F2027 no disponible.") }
                ReadAdmissionResult.Unavailable -> mutableState.update { it.copy(isReadingDetail = false,
                    message = "Registro no disponible o fuera de su alcance autorizado.") }
                ReadAdmissionResult.StorageUnavailable -> mutableState.update { it.copy(isReadingDetail = false,
                    message = "Almacenamiento no disponible al leer la ficha.") }
            }
        }.also { detailReading = it }
    }
    @MainThread fun closeDetail() {
        if (uiState.value.isMutating) return
        if (uiState.value.isEditing) { requestEditDiscard(); return }
        detailReading?.cancel()
        mutableState.update { it.copy(selectedRecord = null, isDetailOpen = false, isReadingDetail = false) }
    }
    @MainThread fun startEditing(): Job? {
        val record = uiState.value.selectedRecord ?: return null
        if (!active || uiState.value.isMutating || record.state == CaptureState.CANCELLED) return null
        if (uiState.value.isEditing) return null
        mutableState.update { it.copy(isEditing = true, isReadingDetail = true, editReason = "", editConflict = false, message = null) }
        val repo = repository ?: return null
        detailReading?.cancel()
        return viewModelScope.launch {
            val result = repo.editContext(record.id)
            currentCoroutineContext().ensureActive()
            when (result) {
                is RecordEditContextResult.Available -> {
                    val editor = ViewModelProvider(editors, object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T =
                            AdmissionFormViewModel(result.environment, result.suggestion,
                                locationProvider = locationProvider, initialRecord = record) as T
                    })[AdmissionFormViewModel::class.java]
                    mutableState.update { it.copy(editForm = editor, isReadingDetail = false) }
                }
                RecordEditContextResult.Unavailable -> mutableState.update { it.copy(isReadingDetail = false,
                    message = "Contexto de edición no disponible o no autorizado. Puedes cerrar y volver a consultar.") }
                RecordEditContextResult.StorageUnavailable -> mutableState.update { it.copy(isReadingDetail = false,
                    message = "Almacenamiento no disponible al preparar la edición. Puedes volver a intentar.") }
            }
        }.also { detailReading = it }
    }
    @MainThread fun updateEditReason(value: String) {
        if (!uiState.value.isMutating) mutableState.update { it.copy(editReason = value) }
    }
    @MainThread fun requestEditDiscard(reload: Boolean = false) {
        if (!uiState.value.isMutating) mutableState.update { it.copy(confirmEditDiscard = true, reloadAfterDiscard = reload) }
    }
    @MainThread fun confirmEditDiscard(confirmed: Boolean) {
        if (uiState.value.isMutating) return
        val current = uiState.value
        mutableState.update { it.copy(confirmEditDiscard = false, reloadAfterDiscard = false) }
        if (!confirmed) return
        cancelEditing()
        if (current.reloadAfterDiscard) current.selectedRecord?.let { openDetail(it.id) }
    }
    /** Sólo tras descarte explícito o guardado satisfactorio; no se llama al resolver conflictos. */
    @MainThread fun cancelEditing() {
        if (uiState.value.isMutating) return
        detailReading?.cancel(); editors.clear()
        mutableState.update { it.copy(isEditing = false, isReadingDetail = false, editForm = null,
            editReason = "", editConflict = false) }
    }
    @MainThread fun saveEditor(intent: SaveIntent): Job? {
        val current = uiState.value
        val form = current.editForm ?: return null
        val fields = form.uiState.value
        if (fields.isAcquiringLocation || fields.locationCandidate != null || fields.isQrScannerOpen) return null
        return saveEdit(fields.input, current.editReason, intent, fields.location)
    }
    @MainThread fun saveEdit(editedInput: AdmissionInput, reason: String?,
        intent: SaveIntent? = null, location: io.github.grupo6.capturaprospectos.domain.capture.CaptureLocation? = uiState.value.selectedRecord?.location): Job? {
        val current = uiState.value
        if (!active || current.isMutating || current.isReadingDetail || current.editConflict) return null
        val selected = current.selectedRecord ?: return null
        val repo = repository ?: return null
        val saveIntent = intent ?: if (selected.state == CaptureState.DRAFT) SaveIntent.SAVE_DRAFT else SaveIntent.CONFIRM_REGISTRATION
        reading?.cancel(); detailReading?.cancel()
        current.editForm?.editingMutation(true)
        mutableState.update { it.copy(isSavingEdit = true, isMutating = true, isLoading = false, message = null) }
        return viewModelScope.launch {
            val result = repo.edit(EditAdmission(selected.id, selected.revision, editedInput, saveIntent, location, reason))
            currentCoroutineContext().ensureActive()
            val message = when (result) {
                is RecordMutationResult.Saved -> "Inscripción modificada correctamente."
                is RecordMutationResult.Invalid -> "Revisa los campos indicados; los cambios se conservan."
                RecordMutationResult.ReasonRequired, RecordMutationResult.InvalidReason -> "El motivo de supervisión debe tener entre 5 y 300 caracteres."
                RecordMutationResult.RevisionConflict -> "El registro fue modificado externamente. Tus cambios se conservan; recarga explícitamente antes de volver a guardar."
                RecordMutationResult.InvalidTransition -> "El estado no permite esa acción o cambiar el RUT confirmado."
                is RecordMutationResult.DuplicateOwn -> "Ya existe una inscripción propia con ese RUT en el evento. Conserva los cambios y consulta la bandeja."
                RecordMutationResult.DuplicateRestricted -> "Ya existe una inscripción con ese RUT en el evento. Tus cambios se conservan."
                RecordMutationResult.StorageUnavailable -> "Almacenamiento no disponible. Los cambios se conservan para reintentar."
                RecordMutationResult.Unavailable -> "Registro no disponible o fuera de su alcance autorizado."
                else -> "No fue posible guardar las modificaciones. Los cambios se conservan."
            }
            current.editForm?.editingMutation(false, (result as? RecordMutationResult.Invalid)?.issues.orEmpty(), message)
            mutableState.update { it.copy(isSavingEdit = false, isMutating = false, message = message,
                editConflict = result == RecordMutationResult.RevisionConflict) }
            if (result is RecordMutationResult.Saved) {
                cancelEditing(); openDetail(selected.id); load()
            }
        }
    }

    @MainThread fun loadMore(): Job? {
        if (uiState.value.isLoading || uiState.value.isMutating || uiState.value.nextCursor == null) return null
        return load(append = true)
    }
    @MainThread fun openCancelDialog(record: RecordSummary) {
        if (uiState.value.isMutating || record.state == CaptureState.CANCELLED) return
        mutableState.update { it.copy(targetForCancellation = record, cancellationReason = "", message = null) }
    }
    @MainThread fun updateCancellationReason(value: String) {
        if (!uiState.value.isMutating) mutableState.update { it.copy(cancellationReason = value) }
    }
    @MainThread fun closeCancelDialog() {
        if (!uiState.value.isMutating) mutableState.update { it.copy(targetForCancellation = null, cancellationReason = "") }
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
    override fun onCleared() { active = false; detailReading?.cancel(); editors.clear(); mutableState.value = RecordTrayUiState() }
}
