package io.github.grupo6.capturaprospectos.ui.access

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.grupo6.capturaprospectos.data.local.DemoAccessFixtures
import io.github.grupo6.capturaprospectos.data.remote.BackendSession
import io.github.grupo6.capturaprospectos.data.remote.LoginRequestDto
import io.github.grupo6.capturaprospectos.domain.capture.LocalRole
import io.github.grupo6.capturaprospectos.domain.context.AuthResult
import io.github.grupo6.capturaprospectos.domain.context.DemoContextState
import io.github.grupo6.capturaprospectos.domain.context.RegisterAccountCommand
import io.github.grupo6.capturaprospectos.ui.context.DemoContextViewModel
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DemoAccessState(
    val welcome: Boolean = true,
    val actorId: UUID? = null,
    val busy: Boolean = false,
    val error: String? = null,
    val email: String = "",
    val password: String = "",
    val registerMode: Boolean = false,
    val displayName: String = "",
    val selectedRole: LocalRole = LocalRole.CAPTOR,
    val selectedSiteIds: Set<UUID> = emptySet(),
) {
    override fun toString() = "DemoAccessState(welcome=$welcome, actorId=$actorId, busy=$busy, registerMode=$registerMode, credentials=redacted)"
}

/** Controla el inicio de sesión y registro de cuentas reales (Room cifrado y API Gateway JWT). */
class DemoAccessViewModel : ViewModel() {
    private val mutable = MutableStateFlow(DemoAccessState())
    val uiState = mutable.asStateFlow()

    fun begin() { mutable.update { it.copy(welcome = false) } }
    fun editEmail(value: String) { if (!mutable.value.busy) mutable.update { it.copy(email = value, error = null) } }
    fun editPassword(value: String) { if (!mutable.value.busy) mutable.update { it.copy(password = value, error = null) } }
    fun editDisplayName(value: String) { if (!mutable.value.busy) mutable.update { it.copy(displayName = value, error = null) } }
    fun selectRole(role: LocalRole) { if (!mutable.value.busy) mutable.update { it.copy(selectedRole = role, error = null) } }
    fun toggleSite(siteId: UUID) {
        if (!mutable.value.busy) {
            mutable.update { current ->
                val next = if (siteId in current.selectedSiteIds) current.selectedSiteIds - siteId else current.selectedSiteIds + siteId
                current.copy(selectedSiteIds = next, error = null)
            }
        }
    }
    fun toggleRegisterMode() {
        if (!mutable.value.busy) {
            mutable.update { it.copy(registerMode = !it.registerMode, password = "", error = null) }
        }
    }

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
        val current = mutable.value
        if (current.registerMode) {
            register(current.displayName, email, password, current.selectedRole, current.selectedSiteIds, context)
            return
        }
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || password.isEmpty()) {
            mutable.update { it.copy(error = "Ingresa tu correo electrónico y contraseña.") }
            return
        }
        mutable.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                // 1. Intentar autenticación remota contra API Gateway (/api/v1/auth/login) si está disponible
                val remoteToken = withContext(Dispatchers.IO) {
                    try {
                        val response = BackendSession.createService().login(LoginRequestDto(trimmedEmail, password))
                        if (response.isSuccessful) response.body() else null
                    } catch (_: Exception) {
                        null
                    }
                }
                if (remoteToken != null) {
                    BackendSession.setToken(remoteToken.accessToken, remoteToken.siteIds)
                    val mappedRole = BackendSession.mapBackendRole(remoteToken.rol, remoteToken.roles)
                    val mappedSites = remoteToken.siteIds.map(BackendSession::mapBackendSiteToUuid).toSet()
                    val remoteUuid = runCatching { UUID.fromString(remoteToken.userId) }.getOrNull()
                    var authResult: AuthResult? = null
                    val job = context.registerAccount(
                        RegisterAccountCommand(
                            displayName = remoteToken.displayName.ifBlank { remoteToken.username },
                            email = remoteToken.email.ifBlank { "${remoteToken.username}@duocuc.cl" },
                            password = password,
                            role = mappedRole,
                            siteIds = mappedSites,
                            explicitId = remoteUuid,
                        )
                    ) { authResult = it }
                    job?.join()
                    val resolved = authResult
                    if (resolved is AuthResult.Authenticated) {
                        mutable.update { it.copy(actorId = resolved.profile.actor.id, error = null) }
                        return@launch
                    }
                }

                // 2. Autenticación local contra cuentas reales persistidas en Room cifrado
                var localResult: AuthResult? = null
                val authJob = context.authenticate(trimmedEmail, password) { localResult = it }
                if (authJob == null) {
                    mutable.update { it.copy(error = "El contexto está ocupado. Reintenta el acceso.") }
                    return@launch
                }
                authJob.join()
                when (val resolved = localResult) {
                    is AuthResult.Authenticated -> {
                        mutable.update { it.copy(actorId = resolved.profile.actor.id, error = null) }
                        return@launch
                    }
                    AuthResult.StorageUnavailable -> {
                        mutable.update { it.copy(error = "Almacenamiento local no disponible. Reintenta.") }
                        return@launch
                    }
                    else -> Unit
                }

                // 3. Compatibilidad con perfiles preexistentes en pruebas instrumentadas si existen en el snapshot
                val profiles = (context.uiState.value.context as? DemoContextState.Ready)?.snapshot?.profiles.orEmpty()
                val fallbackAccount = DemoAccessFixtures.match(trimmedEmail, password, profiles)
                if (fallbackAccount != null) {
                    val selection = context.selectProfile(fallbackAccount.actorId)
                    if (selection == null) {
                        mutable.update { it.copy(error = "El contexto está ocupado. Reintenta el acceso.") }
                        return@launch
                    }
                    selection.join()
                    val selected = (context.uiState.value.context as? DemoContextState.Ready)?.snapshot?.currentProfile
                    if (selected?.actor?.id == fallbackAccount.actorId) {
                        mutable.update { it.copy(actorId = fallbackAccount.actorId) }
                    } else {
                        mutable.update { it.copy(error = "No se pudo abrir el perfil seleccionado. Los datos se conservan.") }
                    }
                } else {
                    mutable.update { it.copy(error = "Credenciales inválidas. Verifica tu correo y contraseña o crea una cuenta.") }
                }
            } finally {
                mutable.update { it.copy(busy = false) }
            }
        }
    }

    private fun register(
        displayName: String,
        email: String,
        password: String,
        role: LocalRole,
        siteIds: Set<UUID>,
        context: DemoContextViewModel,
    ) {
        mutable.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                var regResult: AuthResult? = null
                val job = context.registerAccount(
                    RegisterAccountCommand(
                        displayName = displayName,
                        email = email,
                        password = password,
                        role = role,
                        siteIds = siteIds,
                    )
                ) { regResult = it }
                if (job == null) {
                    mutable.update { it.copy(error = "El contexto está ocupado. Reintenta el registro.") }
                    return@launch
                }
                job.join()
                when (val resolved = regResult) {
                    is AuthResult.Authenticated -> mutable.update {
                        it.copy(
                            actorId = resolved.profile.actor.id,
                            registerMode = false,
                            displayName = "",
                            error = null,
                        )
                    }
                    is AuthResult.Invalid -> mutable.update { it.copy(error = resolved.message) }
                    AuthResult.DuplicateAccount -> mutable.update { it.copy(error = "Ya existe una cuenta registrada con ese correo electrónico.") }
                    AuthResult.StorageUnavailable -> mutable.update { it.copy(error = "No se pudo guardar la cuenta en el almacenamiento local.") }
                    else -> mutable.update { it.copy(error = "No fue posible completar el registro.") }
                }
            } finally {
                mutable.update { it.copy(busy = false) }
            }
        }
    }

    fun logout() {
        if (!mutable.value.busy) {
            val bearer = BackendSession.bearerHeader()
            BackendSession.clear()
            if (bearer != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    try { BackendSession.createService().logout(bearer) } catch (_: Exception) { }
                }
            }
            mutable.value = DemoAccessState(welcome = false)
        }
    }

    override fun onCleared() {
        mutable.value = DemoAccessState()
        super.onCleared()
    }
}
