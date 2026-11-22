package io.github.grupo6.capturaprospectos.ui.access

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.grupo6.capturaprospectos.data.remote.*
import io.github.grupo6.capturaprospectos.domain.capture.LocalActor
import io.github.grupo6.capturaprospectos.domain.capture.ContactFormats
import io.github.grupo6.capturaprospectos.domain.context.*
import io.github.grupo6.capturaprospectos.domain.device.ConnectivityStatus
import io.github.grupo6.capturaprospectos.data.session.*
import io.github.grupo6.capturaprospectos.ui.context.DemoContextViewModel
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class DemoAccessState(
    val welcome: Boolean = true, val actorId: UUID? = null, val account: UserSummaryDto? = null,
    val busy: Boolean = false, val error: String? = null, val pendingMessage: String? = null,
    val email: String = "", val password: String = "", val registerMode: Boolean = false, val displayName: String = "",
    val offline: Boolean = false, val offlineAvailable: Boolean = false, val sessionNotice: String? = null,
) { override fun toString() = "DemoAccessState(welcome=$welcome,busy=$busy,credentials=redacted)" }

/** Primer acceso remoto: sin registro local ni fallback de contraseña tras rechazos o fallos de red. */
class DemoAccessViewModel(private val remote: AccountRemote = RetrofitAccounts(),
    private val offlineStore: OfflineSessionStore = NoOfflineSession,
    private val clock: OfflineClock = ProcessClock,
    private val rememberedStore: RememberedSessionStore = NoRememberedSession) : ViewModel() {
    private val mutable = MutableStateFlow(DemoAccessState())
    val uiState = mutable.asStateFlow()
    private var expiration: Job? = null
    private var network = ConnectivityStatus.UNKNOWN
    private var lease: OfflineSession? = null
    private var epoch = 0L
    private var restoreAttempted = false
    fun begin() { mutable.update { it.copy(welcome = false) }; refreshOfflineOffer() }
    fun networkChanged(status: ConnectivityStatus) {
        val previous = network; network = status
        if (status == ConnectivityStatus.OFFLINE) {
            BackendSession.allowTransmission(false)
            if (mutable.value.account != null && !mutable.value.busy) enterOffline()
            else refreshOfflineOffer()
        } else if (previous == ConnectivityStatus.OFFLINE || status == ConnectivityStatus.VALIDATED) {
            if (mutable.value.account != null) { BackendSession.allowTransmission(false); revalidate() }
            else mutable.update { it.copy(offlineAvailable = false) }
        }
    }
    private fun refreshOfflineOffer() {
        val revision = offlineStore.revision()
        val generation = epoch
        viewModelScope.launch {
            val saved = offlineStore.read()
            if (revision != offlineStore.revision() || generation != epoch) return@launch
            lease = saved?.takeIf { it.remaining(clock) > 0 }
            mutable.update { it.copy(offlineAvailable = network == ConnectivityStatus.OFFLINE && lease != null) }
        }
    }
    private fun clearLease() { lease = null; offlineStore.clear() }
    private fun onlineLoginRequired() {
        rememberedStore.clear()
        epoch++; BackendSession.clear(); expiration?.cancel()
        mutable.value = DemoAccessState(welcome = false, email = mutable.value.email,
            error = "Inicia sesión con conexión para revalidar tus permisos y habilitar los envíos.")
        refreshOfflineOffer()
    }
    fun restoreRemembered(context: DemoContextViewModel) {
        if (context.uiState.value.busy || context.uiState.value.context !is DemoContextState.Ready) return
        restoreRemembered { profile ->
            var accepted: AuthResult? = null
            val job = context.acceptRemoteProfile(profile) { accepted = it } ?: throw AccountApiException("LOCAL_CONTEXT_BUSY")
            job.join()
            if (accepted !is AuthResult.Authenticated) throw AccountApiException("LOCAL_STORAGE_UNAVAILABLE")
        }
    }
    /** Solo restaura con JWT vigente y /me confirmado; nunca recupera una contraseña. */
    fun restoreRemembered(importProfile: suspend (DemoProfile) -> Unit) {
        if (restoreAttempted || mutable.value.busy || mutable.value.account != null || network in listOf(ConnectivityStatus.OFFLINE, ConnectivityStatus.UNKNOWN)) return
        restoreAttempted = true
        if (mutable.value.email.isNotBlank() || mutable.value.password.isNotBlank() || mutable.value.registerMode) return
        val generation = ++epoch
        mutable.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                val saved = rememberedStore.read() ?: return@launch
                if (generation != epoch) return@launch
                val remaining = saved.remaining(clock, BackendSession.baseUrl) / 1000
                if (remaining < 1) { onlineLoginRequired(); return@launch }
                mutable.update { it.copy(welcome = false, error = null) }
                BackendSession.setToken(saved.token.accessToken, saved.token.siteIds, remaining)
                BackendSession.allowTransmission(false)
                val account = remote.me()
                if (generation != epoch) return@launch
                validateAccount(account)
                if (account.status != "ACTIVE") throw AccountApiException("ACCOUNT_${account.status}", 403)
                require(account.id == saved.token.userId && account.role == saved.token.rol && account.siteIds.toSet() == saved.token.siteIds.toSet())
                if (account.role != "ADMIN") importProfile(account.toProfile())
                if (generation != epoch) return@launch
                if (network == ConnectivityStatus.OFFLINE) throw AccountApiException("NETWORK_UNAVAILABLE")
                val offlineLease = offlineStore.read()?.takeIf { it.account.id == account.id && it.remaining(clock) > 0 }
                if (generation != epoch) return@launch
                lease = offlineLease
                BackendSession.allowTransmission(true)
                mutable.update { it.copy(account = account, actorId = if (account.role == "ADMIN") null else UUID.fromString(account.id), offline = false, sessionNotice = null) }
                expiration?.cancel()
                expiration = viewModelScope.launch { delay(BackendSession.remainingMillis()); if (network == ConnectivityStatus.OFFLINE) enterOffline() else onlineLoginRequired() }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: AccountApiException) {
                if (generation == epoch) {
                    if (error.httpStatus in listOf(401,403)) invalidate(error.code)
                    else {
                        BackendSession.clear(); restoreAttempted = false
                        mutable.update { it.copy(error = accountErrorMessage(error.code), welcome = false) }
                        refreshOfflineOffer()
                    }
                }
            } catch (_: IllegalArgumentException) { if (generation == epoch) invalidate("INVALID_RESPONSE") }
            catch (_: NullPointerException) { if (generation == epoch) invalidate("INVALID_RESPONSE") }
            finally { if (generation == epoch) mutable.update { it.copy(busy = false) } }
        }
    }
    private fun enterOffline() {
        val saved = lease
        if (mutable.value.account?.role == "ADMIN" || saved == null || saved.remaining(clock) <= 0 || saved.account.id != mutable.value.account?.id) {
            onlineLoginRequired(); return
        }
        BackendSession.allowTransmission(false)
        mutable.update { it.copy(offline = true, offlineAvailable = false,
            sessionNotice = "Sesión local sin envíos. Al recuperar red se revalidará el acceso.") }
        expiration?.cancel()
        expiration = viewModelScope.launch { delay(saved.remaining(clock)); invalidate("TOKEN_EXPIRED") }
    }
    fun resumeOffline(context: DemoContextViewModel) {
        resumeOffline { profile ->
            var accepted: AuthResult? = null
            val job = context.acceptRemoteProfile(profile) { accepted = it } ?: throw AccountApiException("LOCAL_CONTEXT_BUSY")
            job.join()
            if (accepted !is AuthResult.Authenticated) throw AccountApiException("LOCAL_STORAGE_UNAVAILABLE")
        }
    }
    fun resumeOffline(importProfile: suspend (DemoProfile) -> Unit) {
        if (mutable.value.busy || network != ConnectivityStatus.OFFLINE) return
        val generation = ++epoch
        mutable.update { it.copy(busy = true, error = null, password = "") }
        viewModelScope.launch {
            try {
                val saved = offlineStore.read() ?: throw AccountApiException("TOKEN_EXPIRED")
                validateAccount(saved.account)
                require(saved.remaining(clock) > 0)
                importProfile(saved.account.toProfile())
                if (generation != epoch) return@launch
                if (network != ConnectivityStatus.OFFLINE) { onlineLoginRequired(); return@launch }
                BackendSession.clear(); lease = saved
                mutable.update { it.copy(account = saved.account, actorId = UUID.fromString(saved.account.id)) }
                enterOffline()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: AccountApiException) { invalidate(error.code) }
            catch (_: IllegalArgumentException) { invalidate("TOKEN_EXPIRED") }
            catch (_: NullPointerException) { invalidate("INVALID_RESPONSE") }
            finally { if (generation == epoch) mutable.update { it.copy(busy = false) } }
        }
    }
    fun editEmail(value: String) { if (!mutable.value.busy) mutable.update { it.copy(email = value, error = null) } }
    fun editPassword(value: String) { if (!mutable.value.busy) mutable.update { it.copy(password = value, error = null) } }
    fun editDisplayName(value: String) { if (!mutable.value.busy) mutable.update { it.copy(displayName = value, error = null) } }
    fun toggleRegisterMode() { if (!mutable.value.busy) mutable.update { it.copy(registerMode = !it.registerMode, password = "", error = null, pendingMessage = null) } }
    fun dismissPending() { mutable.update { it.copy(registerMode = false, pendingMessage = null, error = null, password = "") } }
    fun submitDraft(onSubmit: (String,String) -> Unit) {
        if (mutable.value.busy) return
        val draft = mutable.value
        mutable.update { it.copy(password = "") }
        onSubmit(draft.email, draft.password)
    }
    fun submit(email: String, password: String, context: DemoContextViewModel) {
        if (context.uiState.value.busy) return
        submit(email, password) { profile ->
            var result: AuthResult? = null
            val job = context.acceptRemoteProfile(profile) { result = it }
                ?: throw AccountApiException("LOCAL_CONTEXT_BUSY")
            job.join()
            if (result !is AuthResult.Authenticated) throw AccountApiException("LOCAL_STORAGE_UNAVAILABLE")
        }
    }
    /** Importador inyectable para probar que rechazos nunca seleccionan perfiles locales. */
    fun submit(email: String, password: String, importProfile: suspend (DemoProfile) -> Unit) {
        if (mutable.value.busy) return
        val draft = mutable.value
        val identifier = email.trim()
        mutable.update { it.copy(password = "", error = null) }
        val invalid = when {
            identifier.isBlank() || password.isBlank() -> "Ingresa tu usuario o correo y contraseña."
            draft.registerMode && (draft.displayName.trim().codePointCount(0, draft.displayName.trim().length) !in 2..120) -> "El nombre debe tener entre 2 y 120 caracteres."
            draft.registerMode && (!ContactFormats.validEmail(identifier) || identifier.length > 160) -> "Ingresa un correo electrónico válido."
            draft.registerMode && (password.length !in 6..128 || password.toByteArray(Charsets.UTF_8).size > 72) -> "La contraseña debe tener al menos 6 caracteres y un máximo de 72 bytes."
            else -> null
        }
        if (invalid != null) { mutable.update { it.copy(error = invalid) }; return }
        epoch++; clearLease(); rememberedStore.clear(); restoreAttempted = true
        val generation = epoch
        val authRevision = rememberedStore.revision()
        BackendSession.clear()
        mutable.update { it.copy(busy = true, actorId = null, account = null, error = null, offline = false, offlineAvailable = false) }
        viewModelScope.launch {
            try {
                if (draft.registerMode) {
                    val receipt = remote.register(RegisterRequestDto(draft.displayName.trim(), identifier, password))
                    if (generation != epoch) return@launch
                    require(receipt.status == "PENDING" && UUID.fromString(receipt.userId).toString().isNotEmpty())
                    mutable.update { it.copy(pendingMessage = "Solicitud recibida. El administrador debe habilitar tu cuenta y asignar tus permisos.", registerMode = false, displayName = "") }
                } else {
                    val startedAt = clock.elapsedMillis()
                    val token = remote.login(identifier, password)
                    if (generation != epoch) return@launch
                    require(token.accessToken.isNotBlank() && token.expiresIn in 1..MAX_ONLINE_SESSION_SECONDS)
                    val remembered = RememberedSession(token, startedAt, clock.bootId(), BackendSession.baseUrl)
                    BackendSession.setToken(token.accessToken, token.siteIds, token.expiresIn)
                    BackendSession.allowTransmission(false)
                    val account = remote.me()
                    if (generation != epoch) return@launch
                    val checkedAt = clock.elapsedMillis()
                    validateAccount(account)
                    require(account.id == token.userId && account.role == token.rol && account.siteIds.toSet() == token.siteIds.toSet())
                    if (account.status != "ACTIVE") throw AccountApiException("ACCOUNT_${account.status}", 403)
                    if (account.role != "ADMIN") importProfile(account.toProfile())
                    if (generation != epoch) return@launch
                    val rememberedReady = remembered.remaining(clock, BackendSession.baseUrl) > 0 && rememberedStore.write(remembered, authRevision)
                    if (generation != epoch) return@launch
                    val saved = OfflineSession(account, checkedAt, clock.bootId())
                    val offlineReady = saved.remaining(clock) > 0 && offlineStore.write(saved, offlineStore.revision())
                    if (generation != epoch) return@launch
                    lease = saved.takeIf { offlineReady }
                    BackendSession.allowTransmission(network != ConnectivityStatus.OFFLINE)
                    mutable.update { it.copy(welcome = false, account = account, actorId = if (account.role == "ADMIN") null else UUID.fromString(account.id),
                        sessionNotice = if (rememberedReady || rememberedStore === NoRememberedSession) null else "Sesión activa; no se pudo guardar el acceso para la próxima apertura.") }
                    expiration?.cancel()
                    expiration = viewModelScope.launch {
                        delay(BackendSession.remainingMillis())
                        if (network == ConnectivityStatus.OFFLINE) enterOffline() else onlineLoginRequired()
                    }
                    if (network == ConnectivityStatus.OFFLINE) enterOffline()
                }
            } catch (cancelled: CancellationException) { if (generation == epoch) BackendSession.clear(); throw cancelled }
            catch (error: AccountApiException) {
                if (generation != epoch) return@launch
                BackendSession.clear()
                mutable.update { if (error.code == "ACCOUNT_PENDING") it.copy(pendingMessage = accountErrorMessage(error.code)) else it.copy(error = accountErrorMessage(error.code)) }
            } catch (_: IllegalArgumentException) { if (generation == epoch) invalidate("INVALID_RESPONSE") }
            catch (_: NullPointerException) { if (generation == epoch) invalidate("INVALID_RESPONSE") }
            catch (_: IllegalStateException) { if (generation == epoch) invalidate("INVALID_RESPONSE") }
            finally { if (generation == epoch) mutable.update { it.copy(busy = false) } }
        }
    }
    /** Revalida identidad al volver a primer plano; fallo de red no inventa una validación exitosa. */
    fun revalidate() {
        if (mutable.value.busy || mutable.value.account == null) return
        if (network == ConnectivityStatus.OFFLINE) { enterOffline(); return }
        if (BackendSession.validationHeader() == null) { onlineLoginRequired(); return }
        val generation = epoch
        BackendSession.allowTransmission(false)
        viewModelScope.launch {
            try {
                val fresh = remote.me(); validateAccount(fresh)
                val old = mutable.value.account ?: return@launch
                if (generation != epoch) return@launch
                if (fresh.status != "ACTIVE") invalidate("ACCOUNT_${fresh.status}")
                else if (fresh.id != old.id || fresh.role != old.role || fresh.siteIds.toSet() != old.siteIds.toSet()) invalidate("INVALID_TOKEN")
                else if (network != ConnectivityStatus.OFFLINE) {
                    BackendSession.allowTransmission(true)
                    mutable.update { it.copy(offline = false, sessionNotice = null) }
                    expiration?.cancel()
                    expiration = viewModelScope.launch { delay(BackendSession.remainingMillis()); if (network == ConnectivityStatus.OFFLINE) enterOffline() else onlineLoginRequired() }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: AccountApiException) {
                if (generation != epoch) return@launch
                if (error.httpStatus in listOf(401,403)) invalidate(error.code)
                else if (network == ConnectivityStatus.OFFLINE) enterOffline()
                else mutable.update { it.copy(sessionNotice = "Sin validación del servidor: los envíos permanecen bloqueados.") }
            }
            catch (_: IllegalArgumentException) { invalidate("INVALID_RESPONSE") }
            catch (_: NullPointerException) { invalidate("INVALID_RESPONSE") }
        }
    }
    fun invalidate(code: String) {
        epoch++; clearLease(); rememberedStore.clear(); restoreAttempted = true
        BackendSession.clear(); expiration?.cancel()
        mutable.value = DemoAccessState(welcome = false, email = mutable.value.email, error = accountErrorMessage(code))
    }
    fun logout() {
        epoch++; clearLease(); rememberedStore.clear(); restoreAttempted = true
        BackendSession.clear(); expiration?.cancel(); mutable.value = DemoAccessState(welcome = false)
    }
    override fun onCleared() { BackendSession.clear(); mutable.value = DemoAccessState(); super.onCleared() }
}

fun validateAccount(account: UserSummaryDto) {
    UUID.fromString(account.id)
    require(account.status in listOf("PENDING","ACTIVE","DISABLED") && account.version >= 0)
    require(account.displayName.isNotBlank() && account.email.isNotBlank())
    if (account.status == "ACTIVE") require(account.role in listOf("ADMIN","CAPTADOR","SUPERVISOR"))
    if (account.status == "ACTIVE" && account.role != "ADMIN") require(account.siteIds.isNotEmpty())
}
private fun UserSummaryDto.toProfile() = DemoProfile(
    LocalActor(UUID.fromString(id), BackendSession.mapBackendRole(requireNotNull(role), listOf(role)), siteIds.map(BackendSession::mapBackendSiteToUuid).toSet()), displayName, email)
