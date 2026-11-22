package io.github.grupo6.capturaprospectos.ui.access

import io.github.grupo6.capturaprospectos.data.remote.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import java.util.UUID

/** Ejecutar solo con backend de prueba aislado en host:18080; nunca con cuentas reales. */
@io.github.grupo6.capturaprospectos.testing.RequiresExternalFixture
class RemoteAccountJourneyTest {
    @Test fun actualRetrofitOnEmulatorTraversesPendingApprovalLoginAndDisable() = runBlocking {
        val original=BackendSession.baseUrl
        BackendSession.baseUrl="http://10.0.2.2:18080/"
        val remote=RetrofitAccounts()
        suspend fun denied(code:String, action:suspend ()->Unit) {
            try { action();fail("Expected $code") } catch(error:AccountApiException) { assertEquals(code,error.code) }
        }
        try {
            val email="android-${UUID.randomUUID()}@example.invalid"
            val receipt=remote.register(RegisterRequestDto("Persona ficticia Android",email,"ClaveDemo-2026"))
            assertEquals("PENDING",receipt.status);assertNull(BackendSession.bearerHeader())
            denied("ACCOUNT_PENDING") { remote.login(email,"ClaveDemo-2026") }
            val admin=remote.login("demo-admin","test-admin-password-only")
            BackendSession.setToken(admin.accessToken,admin.siteIds,admin.expiresIn)
            assertEquals("ADMIN",remote.me().role)
            assertEquals(8,remote.sites().size)
            assertTrue(remote.users("PENDING",0).items.any { it.id==receipt.userId })
            val approved=remote.update(receipt.userId,AccountAccessRequestDto("ACTIVE","CAPTADOR",listOf("site-1"),0))
            assertEquals(1L,approved.version)
            denied("VERSION_CONFLICT") { remote.update(receipt.userId,AccountAccessRequestDto("ACTIVE","SUPERVISOR",listOf("site-1"),0)) }
            val captor=remote.login(email,"ClaveDemo-2026")
            BackendSession.setToken(captor.accessToken,captor.siteIds,captor.expiresIn)
            assertEquals(receipt.userId,remote.me().id)
            denied("ACCESS_DENIED") { remote.users("PENDING",0) }
            BackendSession.setToken(admin.accessToken,admin.siteIds,admin.expiresIn)
            assertEquals(2L,remote.update(receipt.userId,AccountAccessRequestDto("DISABLED","CAPTADOR",listOf("site-1"),1)).version)
            denied("ACCOUNT_DISABLED") { remote.login(email,"ClaveDemo-2026") }
        } finally { BackendSession.clear();BackendSession.baseUrl=original }
    }
}
