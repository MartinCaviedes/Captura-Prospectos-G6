package io.github.grupo6.capturaprospectos.ui.access

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.grupo6.capturaprospectos.data.remote.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class UserAdminState(
    val users: List<UserSummaryDto> = emptyList(), val sites: List<ManagedSiteDto> = emptyList(),
    val filter: String = "PENDING", val page: Int = 0, val total: Long = 0, val busy: Boolean = false,
    val selected: UserSummaryDto? = null, val role: String = "CAPTADOR", val status: String = "ACTIVE",
    val assigned: Set<String> = emptySet(), val error: String? = null, val message: String? = null,
    val sessionError: String? = null,
)
class UserAdminViewModel(private val remote: AccountRemote = RetrofitAccounts()) : ViewModel() {
    private val mutable = MutableStateFlow(UserAdminState())
    val uiState = mutable.asStateFlow()
    private var operation: Job? = null
    private var generation = 0L
    private var ownerId: String? = null
    fun reset() { generation++; operation?.cancel(); ownerId = null; mutable.value = UserAdminState() }
    fun openFor(accountId: String) {
        if (ownerId == accountId) return
        reset(); ownerId = accountId; load()
    }
    fun load(filter: String = mutable.value.filter, page: Int = mutable.value.page) {
        if (mutable.value.busy || filter !in listOf("PENDING","ACTIVE","DISABLED") || page < 0) return
        mutable.update { it.copy(busy = true, filter = filter, page = page, users = emptyList(), total = 0, error = null, message = null, selected = null) }
        val epoch = generation
        operation = viewModelScope.launch {
            try {
                val sites = remote.sites()
                val result = remote.users(filter, page)
                result.items.forEach(::validateAccount)
                mutable.update { it.copy(users = result.items, sites = sites.filter { s -> s.enabled }, total = result.total) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: AccountApiException) { failed(error) }
            catch (_: IllegalArgumentException) { failed(AccountApiException("INVALID_RESPONSE")) }
            catch (_: NullPointerException) { failed(AccountApiException("INVALID_RESPONSE")) }
            finally { if (epoch == generation) mutable.update { it.copy(busy = false) } }
        }
    }
    fun select(user: UserSummaryDto) {
        if (mutable.value.busy) return
        mutable.update { it.copy(selected = user, role = user.role ?: "CAPTADOR", status = if (user.status == "PENDING") "ACTIVE" else user.status,
            assigned = user.siteIds.toSet(), error = null, message = null) }
    }
    fun close() { if (!mutable.value.busy) mutable.update { it.copy(selected = null, error = null) } }
    fun role(value: String) { if (!mutable.value.busy && value in listOf("CAPTADOR","SUPERVISOR")) mutable.update { it.copy(role = value) } }
    fun status(value: String) { if (!mutable.value.busy && value in listOf("ACTIVE","DISABLED")) mutable.update { it.copy(status = value) } }
    fun toggleSite(id: String) {
        if (!mutable.value.busy && mutable.value.sites.any { it.id == id && it.enabled }) mutable.update {
            it.copy(assigned = if (id in it.assigned) it.assigned - id else it.assigned + id)
        }
    }
    fun save() {
        val draft = mutable.value
        val user = draft.selected ?: return
        if (draft.busy || user.role == "ADMIN") return
        if (draft.status == "ACTIVE" && draft.assigned.isEmpty()) {
            mutable.update { it.copy(error = "Selecciona al menos una sede para habilitar la cuenta.") }; return
        }
        mutable.update { it.copy(busy = true, error = null, message = null) }
        val epoch = generation
        operation = viewModelScope.launch {
            try {
                val role = if (draft.status == "DISABLED" && user.role == null && draft.assigned.isEmpty()) null else draft.role
                val saved = remote.update(user.id, AccountAccessRequestDto(draft.status, role, draft.assigned.sorted(), user.version))
                validateAccount(saved)
                mutable.update { it.copy(selected = null, users = it.users.filterNot { row -> row.id == saved.id } + listOfNotNull(saved.takeIf { row -> row.status == it.filter }), message = "Permisos guardados. Los tokens anteriores pueden seguir vigentes hasta 16 minutos.") }
                val result = remote.users(draft.filter, draft.page)
                mutable.update { it.copy(selected = null, users = result.items, total = result.total, message = "Permisos guardados. Los tokens anteriores pueden seguir vigentes hasta 16 minutos.") }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: AccountApiException) {
                if (error.code == "VERSION_CONFLICT") {
                    try {
                        val fresh = remote.user(user.id); validateAccount(fresh)
                        mutable.update { it.copy(selected = fresh, role = fresh.role ?: "CAPTADOR", status = if (fresh.status == "PENDING") "ACTIVE" else fresh.status, assigned = fresh.siteIds.toSet()) }
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (refresh: AccountApiException) { failed(refresh) }
                }
                failed(error)
            } catch (_: IllegalArgumentException) { failed(AccountApiException("INVALID_RESPONSE")) }
            catch (_: NullPointerException) { failed(AccountApiException("INVALID_RESPONSE")) }
            finally { if (epoch == generation) mutable.update { it.copy(busy = false) } }
        }
    }
    private fun failed(error: AccountApiException) {
        mutable.update { it.copy(error = accountErrorMessage(error.code), sessionError = if (error.httpStatus in listOf(401,403)) error.code else it.sessionError) }
    }
}
