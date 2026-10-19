package io.github.grupo6.capturaprospectos.ui.access

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.grupo6.capturaprospectos.data.local.DemoAccessFixtures
import io.github.grupo6.capturaprospectos.domain.context.DemoContextState
import io.github.grupo6.capturaprospectos.ui.context.DemoContextViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class DemoAccessState(val welcome: Boolean = true, val actorId: UUID? = null,
    val busy: Boolean = false, val error: String? = null,
    val email: String = "", val password: String = "") {
    override fun toString() = "DemoAccessState(welcome=$welcome, actorId=$actorId, busy=$busy, credentials=redacted)"
}

/** Barrera de evaluación local, no autenticación ni control de seguridad institucional.
 * El borrador de credenciales sobrevive a recreaciones sólo en memoria del ViewModel.
 * No persiste contraseñas, tokens o sesión en SavedState/Room. */
class DemoAccessViewModel : ViewModel() {
    private val mutable = MutableStateFlow(DemoAccessState())
    val uiState = mutable.asStateFlow()
    fun begin() { mutable.update { it.copy(welcome = false) } }
    fun editEmail(value: String) { if (!mutable.value.busy) mutable.update { it.copy(email = value, error = null) } }
    fun editPassword(value: String) { if (!mutable.value.busy) mutable.update { it.copy(password = value, error = null) } }
    fun selectSample(email: String) {
        if (mutable.value.busy || DemoAccessFixtures.accounts.none { it.email == email }) return
        mutable.update { it.copy(email = email, password = DemoAccessFixtures.PASSWORD, error = null) }
    }
    /** Consume la contraseña antes de validar; no reaparece al recrear tras un intento fallido. */
    fun submitDraft(onSubmit: (String, String) -> Unit) {
        val draft = mutable.value
        if (draft.busy) return
        mutable.update { it.copy(password = "") }
        onSubmit(draft.email, draft.password)
    }

    fun submit(email: String, password: String, context: DemoContextViewModel) {
        if (mutable.value.busy || context.uiState.value.busy) return
        val profiles = (context.uiState.value.context as? DemoContextState.Ready)?.snapshot?.profiles.orEmpty()
        val account = DemoAccessFixtures.match(email, password, profiles)
        if (account == null) {
            mutable.update { it.copy(error = "Revisa las credenciales demo. Usa una de las cuentas ficticias indicadas.") }
            return
        }
        mutable.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                val selection = context.selectProfile(account.actorId)
                if (selection == null) {
                    mutable.update { it.copy(error = "El contexto está ocupado. Reintenta el acceso.") }
                    return@launch
                }
                selection.join()
                val selected = (context.uiState.value.context as? DemoContextState.Ready)?.snapshot?.currentProfile
                if (selected?.actor?.id == account.actorId) mutable.update { it.copy(actorId = account.actorId) }
                else mutable.update { it.copy(error = "No se pudo seleccionar el perfil demo. Los datos se conservan.") }
            } finally { mutable.update { it.copy(busy = false) } }
        }
    }
    fun logout() { if (!mutable.value.busy) mutable.value = DemoAccessState(welcome = false) }
    override fun onCleared() { mutable.value = DemoAccessState(); super.onCleared() }
}
