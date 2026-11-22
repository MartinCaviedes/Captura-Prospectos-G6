package io.github.grupo6.capturaprospectos.ui.access

import com.sun.net.httpserver.HttpServer
import io.github.grupo6.capturaprospectos.data.remote.*
import java.net.InetSocketAddress
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

class AccountRetrofitTest {
    @Test fun registrationHasNoPrivilegeFieldsAndParsesActualContract() = runBlocking {
        var body="";var path=""
        val server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
        server.createContext("/") { exchange ->
            path=exchange.requestURI.path;body=exchange.requestBody.bufferedReader().readText()
            val data="""{"userId":"11111111-1111-4111-8111-111111111111","status":"PENDING","message":"Recibida"}""".toByteArray()
            exchange.responseHeaders.add("Content-Type","application/json");exchange.sendResponseHeaders(201,data.size.toLong())
            exchange.responseBody.use { it.write(data) }
        }
        server.start()
        try {
            val api=RetrofitAccounts { BackendSession.createService("http://127.0.0.1:${server.address.port}/") }
            val result=api.register(RegisterRequestDto("Persona ficticia","persona@example.invalid","ClaveDemo-2026"))
            assertEquals("PENDING",result.status);assertEquals("/api/v1/auth/register",path)
            val json=com.google.gson.JsonParser.parseString(body).asJsonObject
            assertEquals(setOf("displayName","email","password"),json.keySet())
        } finally { server.stop(0) }
    }
    @Test fun backendErrorCodeSurvivesRetrofitWithoutDisplayingUntrustedMessage() = runBlocking {
        val server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
        server.createContext("/") { exchange ->
            val data="""{"code":"ACCOUNT_DISABLED","message":"SERVER DETAIL NOT FOR UI"}""".toByteArray()
            exchange.sendResponseHeaders(403,data.size.toLong());exchange.responseBody.use { it.write(data) }
        }
        server.start()
        try {
            val api=RetrofitAccounts { BackendSession.createService("http://127.0.0.1:${server.address.port}/") }
            try { api.login("persona","ClaveDemo-2026");fail("Expected denial") }
            catch (error: AccountApiException) { assertEquals("ACCOUNT_DISABLED",error.code);assertEquals(403,error.httpStatus) }
            assertFalse(accountErrorMessage("ACCOUNT_DISABLED").contains("SERVER DETAIL"))
        } finally { server.stop(0) }
    }
}
