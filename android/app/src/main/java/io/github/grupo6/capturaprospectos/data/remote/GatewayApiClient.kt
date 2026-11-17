package io.github.grupo6.capturaprospectos.data.remote

import io.github.grupo6.capturaprospectos.data.local.DemoSeed
import io.github.grupo6.capturaprospectos.domain.capture.LocalRole
import java.util.UUID
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

data class LoginRequestDto(
    val username: String,
    val password: String,
)

data class TokenResponseDto(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long = 900,
    val userId: String,
    val username: String,
    val email: String,
    val displayName: String,
    val rol: String,
    val roles: List<String> = emptyList(),
    val siteIds: List<String> = emptyList(),
)

data class CreateProspectRequestDto(
    val rut: String,
    val eventId: String,
    val siteId: String,
    val givenNames: String,
    val familyNames: String,
    val email: String,
    val phone: String,
    val consentAccepted: Boolean,
    val consentVersion: String,
)

data class UpdateProspectRequestDto(
    val givenNames: String,
    val familyNames: String,
    val email: String,
    val phone: String,
)

data class CancelProspectRequestDto(
    val reason: String,
)

data class ProspectResponseDto(
    val id: String,
    val rut: String,
    val eventId: String,
    val siteId: String,
    val captorId: String,
    val givenNames: String,
    val familyNames: String,
    val email: String,
    val phone: String,
    val consentVersion: String,
    val status: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val cancelledAt: String? = null,
    val cancellationReason: String? = null,
)

data class ProspectPageDto(
    val items: List<ProspectResponseDto> = emptyList(),
    val page: Int = 0,
    val size: Int = 20,
    val total: Long = 0,
)

/** Contrato Retrofit alineado con api-gateway, users-service y prospects-service. */
interface GatewayApiService {
    @POST("/api/v1/auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<TokenResponseDto>

    @POST("/api/v1/auth/logout")
    suspend fun logout(@Header("Authorization") bearerToken: String): Response<Map<String, String>>

    @GET("/api/v1/me")
    suspend fun me(@Header("Authorization") bearerToken: String): Response<Map<String, Any>>

    @POST("/api/v1/prospects")
    suspend fun createProspect(
        @Header("Authorization") bearerToken: String,
        @Body request: CreateProspectRequestDto,
    ): Response<ProspectResponseDto>

    @GET("/api/v1/prospects/{id}")
    suspend fun getProspect(
        @Header("Authorization") bearerToken: String,
        @Path("id") id: String,
    ): Response<ProspectResponseDto>

    @GET("/api/v1/prospects")
    suspend fun listProspects(
        @Header("Authorization") bearerToken: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): Response<ProspectPageDto>

    @PUT("/api/v1/prospects/{id}")
    suspend fun updateProspect(
        @Header("Authorization") bearerToken: String,
        @Path("id") id: String,
        @Body request: UpdateProspectRequestDto,
    ): Response<ProspectResponseDto>

    @POST("/api/v1/prospects/{id}/cancel")
    suspend fun cancelProspect(
        @Header("Authorization") bearerToken: String,
        @Path("id") id: String,
        @Body request: CancelProspectRequestDto,
    ): Response<ProspectResponseDto>
}

/** Mantiene en memoria la sesión JWT activa sin persistir tokens ni contraseñas en texto plano. */
object BackendSession {
    @Volatile var baseUrl: String = "http://10.0.2.2:8080/"
    @Volatile var accessToken: String? = null
        private set
    @Volatile var rawSiteIds: Set<String> = emptySet()
        private set

    private val remoteIdsByLocalCapture = java.util.concurrent.ConcurrentHashMap<UUID, String>()

    fun setToken(token: String?, siteIds: Collection<String> = emptyList()) {
        accessToken = token
        rawSiteIds = siteIds.toSet()
    }

    fun clear() {
        accessToken = null
        rawSiteIds = emptySet()
    }

    fun bearerHeader(): String? = accessToken?.takeIf { it.isNotBlank() }?.let { "Bearer $it" }

    fun recordRemoteId(localId: UUID, remoteId: String) {
        remoteIdsByLocalCapture[localId] = remoteId
    }

    fun remoteIdFor(localId: UUID): String? = remoteIdsByLocalCapture[localId]

    fun createService(customBaseUrl: String = baseUrl): GatewayApiService {
        val normalizedUrl = if (customBaseUrl.endsWith("/")) customBaseUrl else "$customBaseUrl/"
        val client = OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GatewayApiService::class.java)
    }

    fun mapBackendRole(rol: String, roles: List<String>): LocalRole {
        val all = (listOf(rol) + roles).map { it.uppercase() }
        return if ("SUPERVISOR" in all || "ADMIN" in all) LocalRole.SUPERVISOR else LocalRole.CAPTOR
    }

    fun mapBackendSiteToUuid(rawSiteId: String): UUID {
        val trimmed = rawSiteId.trim()
        return try {
            UUID.fromString(trimmed)
        } catch (_: IllegalArgumentException) {
            UUID.fromString(DemoSeed.id(trimmed))
        }
    }

    fun mapLocalSiteToBackend(siteUuid: UUID): String {
        for (index in 1..8) {
            val key = "site-$index"
            if (DemoSeed.id(key) == siteUuid.toString() && (rawSiteIds.isEmpty() || key in rawSiteIds)) {
                return key
            }
        }
        return siteUuid.toString()
    }
}
