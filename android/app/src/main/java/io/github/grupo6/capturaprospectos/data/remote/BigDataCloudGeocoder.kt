package io.github.grupo6.capturaprospectos.data.remote

import com.google.gson.JsonParseException
import io.github.grupo6.capturaprospectos.domain.capture.CaptureLocation
import io.github.grupo6.capturaprospectos.domain.device.*
import java.io.IOException
import java.time.Clock
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

internal data class LocalityResponse(val latitude:Double?,val longitude:Double?,val lookupSource:String?,
    val locality:String?,val city:String?,val principalSubdivision:String?,val countryName:String?) {
    override fun toString()="LocalityResponse(location=redacted)"
}
internal interface BigDataCloudApi {
    @GET("data/reverse-geocode-client")
    suspend fun reverse(@Query("latitude") latitude:Double,@Query("longitude") longitude:Double,
        @Query("localityLanguage") language:String="es"):Response<LocalityResponse>
}
internal fun createGeocodingApi(baseUrl:String="https://api.bigdatacloud.net/"):BigDataCloudApi {
    // Cliente separado: nunca reutilizar JWT, cookies, interceptores o caché del gateway.
    val client=OkHttpClient.Builder().callTimeout(5,TimeUnit.SECONDS).connectTimeout(3,TimeUnit.SECONDS)
        .readTimeout(3,TimeUnit.SECONDS).writeTimeout(3,TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).build()
    return Retrofit.Builder().baseUrl(baseUrl).client(client).addConverterFactory(GsonConverterFactory.create())
        .build().create(BigDataCloudApi::class.java)
}
internal class BigDataCloudGeocoder(private val api:BigDataCloudApi=createGeocodingApi(),
    private val clock:Clock=Clock.systemUTC(),private val elapsedNanos:()->Long=System::nanoTime):ReverseGeocoder {
    private val mutex=Mutex()
    private var previousRequest:Long?=null
    override suspend fun lookup(position:CaptureLocation.Device):GeocodingResult=mutex.withLock {
        if(!GeocodingPolicy.isFresh(position,clock.instant())) return@withLock GeocodingResult.Failed(GeocodingFailure.STALE_POSITION)
        val elapsed=elapsedNanos()
        if(previousRequest?.let { elapsed-it in 0 until 5_000_000_000L }==true)
            return@withLock GeocodingResult.Failed(GeocodingFailure.LIMITED)
        previousRequest=elapsed
        try {
            val response=api.reverse(position.coordinates.latitude,position.coordinates.longitude)
            if(!response.isSuccessful) return@withLock GeocodingResult.Failed(
                if(response.code() in listOf(402,429)) GeocodingFailure.LIMITED else GeocodingFailure.UNAVAILABLE)
            val body=response.body() ?: return@withLock GeocodingResult.Failed(GeocodingFailure.INVALID_RESPONSE)
            val lat=body.latitude;val lon=body.longitude
            if(lat==null || lon==null || !lat.isFinite() || !lon.isFinite() ||
                abs(lat-position.coordinates.latitude)>0.000001 || abs(lon-position.coordinates.longitude)>0.000001 ||
                body.lookupSource !in listOf("coordinates","reverseGeocoding"))
                return@withLock GeocodingResult.Failed(GeocodingFailure.INVALID_RESPONSE)
            fun name(value:String?)=value?.trim()?.takeIf { it.length in 1..160 && it.none(Char::isISOControl) }
            val locality=name(body.locality) ?: name(body.city)
                ?: return@withLock GeocodingResult.Failed(GeocodingFailure.INVALID_RESPONSE)
            val label=listOfNotNull(locality,name(body.principalSubdivision),name(body.countryName))
                .distinctBy { it.lowercase(java.util.Locale.ROOT) }.joinToString(", ")
            GeocodingResult.Found(label)
        } catch(cancelled:CancellationException) { throw cancelled }
        catch(_:com.google.gson.stream.MalformedJsonException) { GeocodingResult.Failed(GeocodingFailure.INVALID_RESPONSE) }
        catch(_:java.io.EOFException) { GeocodingResult.Failed(GeocodingFailure.INVALID_RESPONSE) }
        catch(_:IOException) { GeocodingResult.Failed(GeocodingFailure.NETWORK) }
        catch(_:JsonParseException) { GeocodingResult.Failed(GeocodingFailure.INVALID_RESPONSE) }
        catch(_:IllegalArgumentException) { GeocodingResult.Failed(GeocodingFailure.INVALID_RESPONSE) }
    }
}
