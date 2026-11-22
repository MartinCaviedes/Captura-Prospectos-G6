package io.github.grupo6.capturaprospectos.ui.access

import androidx.lifecycle.ViewModelStore
import io.github.grupo6.capturaprospectos.data.remote.*
import io.github.grupo6.capturaprospectos.data.session.*
import io.github.grupo6.capturaprospectos.domain.device.ConnectivityStatus.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

private class MemoryLease : OfflineSessionStore {
    var saved: OfflineSession? = null
    var rev = 0L
    override fun revision() = rev
    override suspend fun read() = saved
    override suspend fun write(session: OfflineSession, expectedRevision: Long): Boolean {
        if (rev != expectedRevision) return false
        saved = session; return true
    }
    override fun clear() { rev++; saved = null }
}
private class TestClock : OfflineClock {
    var now = 1000L
    var boot = 7
    override fun elapsedMillis() = now
    override fun bootId() = boot
}

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineAccessTest {
    private val dispatcher = StandardTestDispatcher()
    private val owners = ViewModelStore()
    private val storage = MemoryLease()
    private val clock = TestClock()
    private val fake = FakeAccounts()
    private var meError: AccountApiException? = null
    private var meGate: CompletableDeferred<Unit>? = null
    private val remote = object : AccountRemote by fake {
        override suspend fun me(): UserSummaryDto {
            meGate?.await(); meError?.let { throw it }; return fake.current
        }
    }
    private var index = 0
    private fun model() = DemoAccessViewModel(remote, storage, clock).also { owners.put("${index++}", it) }
    private fun tick() = dispatcher.scheduler.runCurrent()
    private fun login(model: DemoAccessViewModel) { model.submit("persona", "ClaveSoloPrueba") {}; tick() }
    @Before fun setup() { Dispatchers.setMain(dispatcher); BackendSession.clear() }
    @After fun cleanup() { owners.clear(); tick(); BackendSession.clear(); Dispatchers.resetMain() }

    @Test fun leaseUsesElapsedTimeAndRejectsRebootClockRollbackAndBoundary() {
        val lease = OfflineSession(account(), clock.now, clock.boot)
        assertEquals(OFFLINE_WINDOW_MS, lease.remaining(clock))
        clock.now += OFFLINE_WINDOW_MS - 1; assertEquals(1L, lease.remaining(clock))
        clock.now++; assertEquals(0L, lease.remaining(clock))
        clock.now = 999; assertEquals(0L, lease.remaining(clock))
        clock.now = 1000; clock.boot++; assertEquals(0L, lease.remaining(clock))
    }
    @Test fun onlyActiveOperationalRolesCanGetOfflineLease() {
        for (value in listOf(account("ADMIN"), account(status="DISABLED"), account(null,"PENDING"), account().copy(siteIds=emptyList()))) {
            assertEquals(0L, OfflineSession(value,clock.now,clock.boot).remaining(clock))
        }
        assertTrue(OfflineSession(account("SUPERVISOR"),clock.now,clock.boot).remaining(clock)>0)
    }
    @Test fun onlineLoginThenLossOfNetworkKeepsLocalIdentityAndBlocksBearer() {
        val vm=model(); login(vm); assertNotNull(storage.saved); assertNotNull(BackendSession.bearerHeader())
        vm.networkChanged(OFFLINE);tick()
        assertTrue(vm.uiState.value.offline); assertNotNull(vm.uiState.value.account)
        assertNull(BackendSession.bearerHeader()); assertNotNull(BackendSession.validationHeader())
    }
    @Test fun processRecreationRequiresExplicitOfflineResumeAndNeverRestoresJwt() {
        login(model()); owners.clear(); tick()
        val vm=model(); vm.networkChanged(OFFLINE); vm.begin();tick()
        assertNull(vm.uiState.value.account); assertTrue(vm.uiState.value.offlineAvailable)
        var imported=0; vm.resumeOffline { imported++ };tick()
        assertEquals(1,imported); assertTrue(vm.uiState.value.offline)
        assertEquals(account().id,vm.uiState.value.account?.id);assertNull(BackendSession.validationHeader())
        assertEquals("",vm.uiState.value.password)
    }
    @Test fun offlineResumeCannotBeOfferedOnUnknownOrUnvalidatedNetwork() {
        login(model());owners.clear();tick();val vm=model()
        vm.begin();tick();assertFalse(vm.uiState.value.offlineAvailable)
        vm.networkChanged(CONNECTED_UNVALIDATED); vm.resumeOffline { fail("No confirmed offline network") };tick()
        assertNull(vm.uiState.value.account)
    }
    @Test fun rebootAndExpiryRejectResumeWithoutImporting() {
        login(model());owners.clear();tick();clock.boot++
        val vm=model();vm.networkChanged(OFFLINE);tick()
        assertFalse(vm.uiState.value.offlineAvailable)
        vm.resumeOffline { fail("Reboot requires online validation") };tick()
        assertNull(vm.uiState.value.account);assertNull(storage.saved)
    }
    @Test fun twelveHourExpiryClosesOnlyAccess() {
        val vm=model();login(vm);vm.networkChanged(OFFLINE);tick()
        clock.now += OFFLINE_WINDOW_MS
        dispatcher.scheduler.advanceTimeBy(OFFLINE_WINDOW_MS);tick()
        assertNull(vm.uiState.value.account);assertNull(storage.saved);assertNull(BackendSession.bearerHeader())
    }
    @Test fun logoutDurablyDiscardsLeaseAndNextOfflineAccessIsUnavailable() {
        val vm=model();login(vm);vm.logout();vm.networkChanged(OFFLINE);tick()
        assertNull(storage.saved);assertFalse(vm.uiState.value.offlineAvailable)
    }
    @Test fun rejectedLoginCannotReusePreviousOfflineAuthorization() {
        val vm=model();login(vm);fake.loginError=AccountApiException("INVALID_CREDENTIALS",401)
        login(vm);vm.networkChanged(OFFLINE);tick()
        assertNull(storage.saved);assertNull(vm.uiState.value.account);assertFalse(vm.uiState.value.offlineAvailable)
    }
    @Test fun adminNeverRestoresOffline() {
        fake.current=account("ADMIN");val vm=model();login(vm)
        assertNull(storage.saved);vm.networkChanged(OFFLINE);tick();assertNull(vm.uiState.value.account)
    }
    @Test fun reconnectBlocksAllSendsUntilServerValidationCompletes() {
        val vm=model();login(vm);vm.networkChanged(OFFLINE);tick()
        meGate=CompletableDeferred(); vm.networkChanged(VALIDATED);tick()
        assertNull(BackendSession.bearerHeader())
        meGate!!.complete(Unit);tick()
        assertNotNull(BackendSession.bearerHeader());assertFalse(vm.uiState.value.offline)
    }
    @Test fun serverFailureKeepsSendsBlockedAndServerRejectionRevokesLease() {
        val vm=model();login(vm);vm.networkChanged(OFFLINE);tick()
        meError=AccountApiException("NETWORK_UNAVAILABLE");vm.networkChanged(VALIDATED);tick()
        assertNotNull(vm.uiState.value.account);assertNull(BackendSession.bearerHeader())
        meError=AccountApiException("ACCOUNT_DISABLED",403);vm.revalidate();tick()
        assertNull(vm.uiState.value.account);assertNull(storage.saved)
    }
    @Test fun reconnectAfterProcessRecreationRequiresNewLogin() {
        login(model());owners.clear();tick();val vm=model();vm.networkChanged(OFFLINE);tick()
        vm.resumeOffline {};tick();vm.networkChanged(VALIDATED);tick()
        assertNull(vm.uiState.value.account);assertNull(BackendSession.bearerHeader())
        assertNotNull(storage.saved) // Can still return to local work within the original window.
    }
    @Test fun permissionChangeOnRevalidationRevokesCachedAccess() {
        val vm=model();login(vm);fake.current=account("SUPERVISOR",version=2)
        vm.revalidate();tick();assertNull(vm.uiState.value.account);assertNull(storage.saved)
    }
    @Test fun logoutDuringRevalidationCannotResurrectSession() {
        val vm=model();login(vm);meGate=CompletableDeferred();vm.revalidate();tick()
        vm.logout();meGate!!.complete(Unit);tick()
        assertNull(vm.uiState.value.account);assertNull(storage.saved);assertNull(BackendSession.bearerHeader())
    }
}
