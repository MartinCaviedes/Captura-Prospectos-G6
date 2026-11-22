package io.github.grupo6.capturaprospectos.data.remote

import com.google.gson.Gson
import java.io.IOException
import kotlinx.coroutines.CancellationException
import retrofit2.Response

class AccountApiException(val code: String, val httpStatus: Int = 0) : Exception(code)
interface AccountRemote {
    suspend fun register(request: RegisterRequestDto): RegistrationReceiptDto
    suspend fun login(username: String, password: String): TokenResponseDto
    suspend fun me(): UserSummaryDto
    suspend fun users(status: String, page: Int): UserPageDto
    suspend fun user(id: String): UserSummaryDto
    suspend fun sites(): List<ManagedSiteDto>
    suspend fun update(id: String, request: AccountAccessRequestDto): UserSummaryDto
    suspend fun logout()
}

class RetrofitAccounts(private val service: () -> GatewayApiService = { BackendSession.createService() }) : AccountRemote {
    private data class ErrorBody(val code: String?)
    private fun bearer() = BackendSession.bearerHeader() ?: throw AccountApiException("TOKEN_EXPIRED", 401)
    private suspend fun <T> execute(block: suspend () -> Response<T>): T {
        try {
            val response = block()
            if (!response.isSuccessful) {
                val code = runCatching { Gson().fromJson(response.errorBody()?.string(), ErrorBody::class.java)?.code }.getOrNull()
                throw AccountApiException(code ?: when (response.code()) {
                    401 -> "INVALID_TOKEN"; 403 -> "ACCESS_DENIED"; 429 -> "RATE_LIMITED"; else -> "SERVICE_UNAVAILABLE"
                }, response.code())
            }
            return response.body() ?: throw AccountApiException("INVALID_RESPONSE")
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: IOException) { throw AccountApiException("NETWORK_UNAVAILABLE") }
        catch (error: com.google.gson.JsonParseException) { throw AccountApiException("INVALID_RESPONSE") }
    }
    override suspend fun register(request: RegisterRequestDto) = execute { service().register(request) }
    override suspend fun login(username: String, password: String) = execute { service().login(LoginRequestDto(username, password)) }
    override suspend fun me() = execute { service().me(BackendSession.validationHeader() ?: throw AccountApiException("TOKEN_EXPIRED", 401)) }
    override suspend fun users(status: String, page: Int) = execute { service().users(bearer(), status, page, 20) }
    override suspend fun user(id: String) = execute { service().user(bearer(), id) }
    override suspend fun sites() = execute { service().managedSites(bearer()) }
    override suspend fun update(id: String, request: AccountAccessRequestDto) = execute { service().setAccess(bearer(), id, request) }
    override suspend fun logout() { execute { service().logout(bearer()) } }
}

fun accountErrorMessage(code: String): String = when (code) {
    "ACCOUNT_PENDING" -> "Tu cuenta está pendiente de habilitación por el administrador."
    "ACCOUNT_DISABLED" -> "Tu cuenta está desactivada. Contacta al administrador."
    "INVALID_CREDENTIALS" -> "Usuario o contraseña incorrectos."
    "ACCOUNT_EXISTS" -> "Ya existe una cuenta con ese correo o usuario."
    "INVALID_REQUEST" -> "Revisa los datos y las sedes seleccionadas."
    "VERSION_CONFLICT" -> "Otra persona modificó esta cuenta. Actualiza los datos antes de guardar."
    "ADMIN_PROTECTED" -> "La cuenta administradora está protegida."
    "INVALID_TOKEN", "TOKEN_EXPIRED" -> "Tu sesión venció. Inicia sesión nuevamente."
    "ACCESS_DENIED" -> "No tienes permisos para esta operación."
    "RATE_LIMITED" -> "Demasiados intentos. Espera antes de reintentar."
    "NETWORK_UNAVAILABLE" -> "No se pudo conectar. Revisa la conexión y vuelve a intentar."
    "LOCAL_CONTEXT_BUSY" -> "Espera a que termine la operación actual y vuelve a intentar."
    "LOCAL_STORAGE_UNAVAILABLE" -> "No se pudo preparar tu perfil local. Tus datos se conservan."
    "INVALID_RESPONSE" -> "El servidor devolvió datos incompatibles. Comprueba que esté ejecutándose la versión actual del backend."
    else -> "El servicio no está disponible. Tus datos locales se conservan."
}
