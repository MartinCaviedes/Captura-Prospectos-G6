package io.github.grupo6.capturaprospectos.ui.access

import androidx.lifecycle.ViewModelStore
import io.github.grupo6.capturaprospectos.data.remote.*
import io.github.grupo6.capturaprospectos.data.session.*
import io.github.grupo6.capturaprospectos.domain.device.ConnectivityStatus.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class RememberedAccessTest {
    private val dispatcher=StandardTestDispatcher()
    private val owners=ViewModelStore()
    private var now=1000L;private var boot=7;private var index=0
    private val clock=object:OfflineClock { override fun elapsedMillis()=now; override fun bootId()=boot }
    private val store=object:RememberedSessionStore {
        var saved:RememberedSession?=null;var rev=0L
        override suspend fun read()=saved
        override suspend fun write(session:RememberedSession,expectedRevision:Long):Boolean {
            if(rev!=expectedRevision)return false
            saved=session;return true
        }
        override fun revision()=rev
        override fun clear(){rev++;saved=null}
    }
    private val fake=FakeAccounts()
    private var meError:AccountApiException?=null
    private var meGate:CompletableDeferred<Unit>?=null
    private var logins=0
    private val remote=object:AccountRemote by fake {
        override suspend fun login(username:String,password:String):TokenResponseDto {
            logins++;return fake.login(username,password).copy(expiresIn=MAX_ONLINE_SESSION_SECONDS)
        }
        override suspend fun me():UserSummaryDto { meGate?.await();meError?.let{throw it};return fake.current }
    }
    private fun tick()=dispatcher.scheduler.runCurrent()
    private fun model()=DemoAccessViewModel(remote,clock=clock,rememberedStore=store).also {owners.put("${index++}",it);it.networkChanged(VALIDATED)}
    private fun login():DemoAccessViewModel=model().also{it.submit("persona","Clave-ficticia"){};tick();assertNotNull(store.saved)}
    @Before fun setup(){Dispatchers.setMain(dispatcher);BackendSession.clear()}
    @After fun cleanup(){owners.clear();tick();BackendSession.clear();Dispatchers.resetMain()}
    @Test fun closingProcessThenRestoringUsesMeWithoutPasswordAndDoesNotRenewLifetime() {
        login();val original=store.saved!!;owners.clear();tick();now+=60*60*1000
        val restored=model();var imports=0;restored.restoreRemembered{imports++};tick()
        assertEquals(1,imports);assertEquals(1,logins);assertEquals(account().id,restored.uiState.value.account?.id)
        assertFalse(restored.uiState.value.welcome);assertEquals("",restored.uiState.value.password)
        assertEquals(original,store.saved);assertTrue(BackendSession.remainingMillis()<=3*60*60*1000)
    }
    @Test fun monotonicBoundaryRebootClockRollbackAndBackendChangeRejectSavedToken() {
        login();val saved=store.saved!!
        now+=MAX_ONLINE_SESSION_SECONDS*1000-1;assertEquals(1L,saved.remaining(clock,BackendSession.baseUrl))
        now++;assertEquals(0L,saved.remaining(clock,BackendSession.baseUrl))
        now=999;assertEquals(0L,saved.remaining(clock,BackendSession.baseUrl))
        now=1000;boot++;assertEquals(0L,saved.remaining(clock,BackendSession.baseUrl));boot--
        assertEquals(0L,saved.remaining(clock,"https://otro.example.invalid/"))
    }
    @Test fun expiredTokenRequiresLoginWithoutImporting() {
        login();owners.clear();tick();now+=MAX_ONLINE_SESSION_SECONDS*1000
        val vm=model();vm.restoreRemembered{fail("Expired token")};tick()
        assertNull(vm.uiState.value.account);assertNull(store.saved);assertNull(BackendSession.validationHeader())
    }
    @Test fun disabledOrChangedPermissionsInvalidateRememberedAccess() {
        for(value in listOf(account(status="DISABLED"),account("SUPERVISOR"),account().copy(siteIds=listOf("site-2")))) {
            fake.current=account();login();owners.clear();tick();fake.current=value
            val vm=model();vm.restoreRemembered{fail("Unverified permissions")};tick()
            assertNull(vm.uiState.value.account);assertNull(store.saved);assertNull(BackendSession.bearerHeader())
        }
    }
    @Test fun serverRejectionClearsSavedSession() {
        login();owners.clear();tick();meError=AccountApiException("INVALID_TOKEN",401)
        val vm=model();vm.restoreRemembered{fail("Rejected token")};tick()
        assertNull(vm.uiState.value.account);assertNull(store.saved)
    }
    @Test fun networkErrorKeepsTokenForRetryButNeverAuthorizesOrSends() {
        login();owners.clear();tick();meError=AccountApiException("NETWORK_UNAVAILABLE")
        val vm=model();vm.restoreRemembered{fail("No successful server validation")};tick()
        assertNull(vm.uiState.value.account);assertNotNull(store.saved);assertNull(BackendSession.bearerHeader())
        meError=null;vm.restoreRemembered{};tick();assertNotNull(vm.uiState.value.account)
    }
    @Test fun logoutDuringRestorePreventsLateIdentityAndTokenResurrection() {
        login();owners.clear();tick();meGate=CompletableDeferred()
        val vm=model();vm.restoreRemembered{fail("Logged out")};tick()
        vm.logout();meGate!!.complete(Unit);tick()
        assertNull(vm.uiState.value.account);assertNull(store.saved);assertNull(BackendSession.validationHeader())
        vm.restoreRemembered{fail("Explicit logout")};tick();assertNull(vm.uiState.value.account)
    }
    @Test fun adminCanRestoreOnlineWithoutImportingAnOperationalRole() {
        fake.current=account("ADMIN");login();owners.clear();tick()
        val vm=model();vm.restoreRemembered{fail("ADMIN must not import CAPTADOR")};tick()
        assertEquals("ADMIN",vm.uiState.value.account?.role);assertNull(vm.uiState.value.actorId)
    }
}
