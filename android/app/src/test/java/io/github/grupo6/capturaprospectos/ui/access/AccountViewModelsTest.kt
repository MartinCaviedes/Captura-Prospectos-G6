package io.github.grupo6.capturaprospectos.ui.access

import androidx.lifecycle.ViewModelStore
import io.github.grupo6.capturaprospectos.data.remote.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

internal fun account(role: String? = "CAPTADOR", status: String = "ACTIVE", version: Long = 1) = UserSummaryDto(
    "11111111-1111-4111-8111-111111111111", "persona", "persona@example.invalid", "Persona ficticia", status, role,
    if (role == null || role == "ADMIN") emptyList() else listOf("site-1"), version, "2026-11-21T12:00:00Z", "2026-11-21T12:00:00Z")
internal class FakeAccounts : AccountRemote {
    var current = account()
    var loginError: AccountApiException? = null
    var listError: AccountApiException? = null
    var updateError: AccountApiException? = null
    var afterUpdateListError = false
    var registrations = 0
    var request: AccountAccessRequestDto? = null
    var updates = 0
    override suspend fun register(request: RegisterRequestDto): RegistrationReceiptDto { registrations++; return RegistrationReceiptDto(current.id,"PENDING","Solicitud recibida") }
    override suspend fun login(username: String,password: String): TokenResponseDto {
        loginError?.let { throw it }
        return TokenResponseDto("TOKEN-FICTICIO",userId=current.id,username=current.username,email=current.email,
            displayName=current.displayName,rol=requireNotNull(current.role),roles=listOf(current.role!!),siteIds=current.siteIds)
    }
    override suspend fun me() = current
    override suspend fun users(status: String,page: Int): UserPageDto {
        listError?.let { throw it }; return UserPageDto(listOf(current),page,20,1)
    }
    override suspend fun user(id: String) = current
    override suspend fun sites() = listOf(ManagedSiteDto("site-1","Sede de prueba",true))
    override suspend fun update(id: String,request: AccountAccessRequestDto): UserSummaryDto {
        updates++; this.request=request; updateError?.let { throw it }
        current=current.copy(status=request.status,role=request.role,siteIds=request.siteIds,version=current.version+1)
        if (afterUpdateListError) listError=AccountApiException("NETWORK_UNAVAILABLE")
        return current
    }
    override suspend fun logout() {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelsTest {
    private val dispatcher=StandardTestDispatcher()
    private val store=ViewModelStore()
    private val remote=FakeAccounts()
    private lateinit var access: DemoAccessViewModel
    private lateinit var admin: UserAdminViewModel
    @Before fun setup() {
        Dispatchers.setMain(dispatcher); BackendSession.clear()
        access=DemoAccessViewModel(remote);admin=UserAdminViewModel(remote)
        store.put("access",access);store.put("admin",admin)
    }
    @After fun cleanup() { store.clear(); dispatcher.scheduler.runCurrent(); BackendSession.clear(); Dispatchers.resetMain() }
    private fun tick() = dispatcher.scheduler.runCurrent()

    @Test fun registrationOnlyShowsPendingAndNeverImportsOrAuthenticatesLocally() {
        access.editDisplayName("Persona ficticia");access.toggleRegisterMode()
        var imported=false
        access.submit("persona@example.invalid","ClaveDemo-2026") { imported=true };tick()
        assertEquals(1,remote.registrations);assertFalse(imported);assertNotNull(access.uiState.value.pendingMessage)
        assertNull(access.uiState.value.actorId);assertNull(access.uiState.value.account);assertNull(BackendSession.bearerHeader())
        assertEquals("",access.uiState.value.password)
    }
    @Test fun pendingDisabledInvalidCredentialsAndNetworkErrorsNeverFallBackToLocalAccount() {
        var imported=false
        for (error in listOf(AccountApiException("ACCOUNT_PENDING",403),AccountApiException("ACCOUNT_DISABLED",403),
            AccountApiException("INVALID_CREDENTIALS",401),AccountApiException("NETWORK_UNAVAILABLE"))) {
            access.dismissPending();remote.loginError=error
            access.submit("persona@example.invalid","ClaveDemo-2026") { imported=true };tick()
            assertFalse(imported);assertNull(access.uiState.value.account);assertNull(BackendSession.bearerHeader())
            assertFalse(access.uiState.value.busy)
        }
    }
    @Test fun administratorDoesNotBecomeLocalSupervisorOrRequireAnEvent() {
        remote.current=account("ADMIN")
        access.submit("persona","ClaveDemo-2026") { fail("ADMIN must not import a local capture role") };tick()
        assertEquals("ADMIN",access.uiState.value.account?.role);assertNull(access.uiState.value.actorId)
        assertNotNull(BackendSession.bearerHeader())
    }
    @Test fun captorImportsVerifiedIdentityWithoutPasswordAndLogoutClearsToken() {
        var imported: io.github.grupo6.capturaprospectos.domain.context.DemoProfile?=null
        access.submit("persona","ClaveDemo-2026") { imported=it };tick()
        assertEquals(remote.current.id,imported?.actor?.id.toString());assertEquals(remote.current.email,imported?.email)
        assertEquals(io.github.grupo6.capturaprospectos.domain.capture.LocalRole.CAPTOR,imported?.actor?.role)
        access.logout();assertNull(access.uiState.value.account);assertNull(BackendSession.bearerHeader())
    }
    @Test fun failedLocalImportDoesNotLeaveAuthenticatedSession() {
        access.submit("persona","ClaveDemo-2026") { throw AccountApiException("LOCAL_STORAGE_UNAVAILABLE") };tick()
        assertNull(access.uiState.value.account);assertNull(BackendSession.bearerHeader());assertNotNull(access.uiState.value.error)
    }
    @Test fun expiredOnlineSessionRequiresLoginAgain() {
        access.submit("persona","ClaveDemo-2026") {};tick()
        dispatcher.scheduler.advanceTimeBy(900_001);tick()
        assertNull(access.uiState.value.account);assertNull(BackendSession.bearerHeader());assertNotNull(access.uiState.value.error)
    }
    @Test fun revalidationBlocksAccountDisabledAfterLogin() {
        access.submit("persona","ClaveDemo-2026") {};tick()
        remote.current=remote.current.copy(status="DISABLED")
        access.revalidate();tick()
        assertNull(access.uiState.value.account);assertNull(BackendSession.bearerHeader())
    }
    @Test fun utf8PasswordLimitIsCheckedBeforeRequest() {
        access.editDisplayName("Persona ficticia");access.toggleRegisterMode()
        access.submit("persona@example.invalid","á".repeat(37)) {};tick()
        assertEquals(0,remote.registrations);assertNotNull(access.uiState.value.error)
    }
    @Test fun activationRequiresSitesAndSendsServerVersion() {
        remote.current=account(null,"PENDING",7)
        admin.load();tick();admin.select(remote.current);admin.save();tick()
        assertEquals(0,remote.updates);assertNotNull(admin.uiState.value.error)
        admin.toggleSite("site-1");admin.save();tick()
        assertEquals(7L,remote.request?.expectedVersion);assertEquals("ACTIVE",remote.request?.status)
        assertEquals(listOf("site-1"),remote.request?.siteIds);assertNotNull(admin.uiState.value.message)
    }
    @Test fun versionConflictRefreshesWithoutAutomaticallyRepeatingMutation() {
        admin.load();tick();admin.select(remote.current)
        remote.current=account("SUPERVISOR",version=8);remote.updateError=AccountApiException("VERSION_CONFLICT",409)
        admin.save();tick()
        assertEquals(1,remote.updates);assertEquals(8L,admin.uiState.value.selected?.version)
        assertEquals("SUPERVISOR",admin.uiState.value.role);assertNotNull(admin.uiState.value.error)
    }
    @Test fun successfulSaveIsNotReportedAsLostWhenListRefreshFails() {
        admin.load();tick();admin.select(remote.current);remote.afterUpdateListError=true
        admin.save();tick()
        assertEquals(1,remote.updates);assertNull(admin.uiState.value.selected)
        assertNotNull(admin.uiState.value.message);assertNotNull(admin.uiState.value.error)
    }
    @Test fun protectedAdministratorCannotBeSavedAndForbiddenLoadEndsAdministration() {
        remote.current=account("ADMIN");admin.load();tick();admin.select(remote.current);admin.save();tick()
        assertEquals(0,remote.updates)
        admin.close();remote.listError=AccountApiException("ACCESS_DENIED",403);admin.load();tick()
        assertEquals("ACCESS_DENIED",admin.uiState.value.sessionError)
    }
    @Test fun reopeningSameAdministratorPreservesUnsavedDraftAndChangingOwnerClearsIt() {
        admin.openFor("admin-1");tick();admin.select(remote.current);admin.role("SUPERVISOR")
        admin.openFor("admin-1");tick()
        assertNotNull(admin.uiState.value.selected);assertEquals("SUPERVISOR",admin.uiState.value.role)
        admin.openFor("admin-2");tick();assertNull(admin.uiState.value.selected)
    }
}
