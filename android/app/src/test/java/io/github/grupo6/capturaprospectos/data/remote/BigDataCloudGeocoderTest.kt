package io.github.grupo6.capturaprospectos.data.remote

import com.sun.net.httpserver.HttpServer
import io.github.grupo6.capturaprospectos.domain.capture.*
import io.github.grupo6.capturaprospectos.domain.device.*
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.time.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

/** Solo HTTP loopback: nunca se envían coordenadas ficticias al proveedor. */
class BigDataCloudGeocoderTest {
    private val now=Instant.parse("2026-11-22T16:00:00Z")
    private val position=CaptureLocation.Device(Coordinates(-33.0,-71.0),now,10.0)
    private var elapsed=0L
    private var code=200
    private var json="""{"latitude":-33.0,"longitude":-71.0,"lookupSource":"coordinates","locality":"Localidad ficticia","city":"Ciudad ficticia","principalSubdivision":"Región ficticia","countryName":"Chile"}"""
    private var calls=0
    private var query=""
    private var path=""
    private var authorization:String?=null
    private var cookie:String?=null
    private lateinit var server:HttpServer
    private lateinit var adapter:BigDataCloudGeocoder
    @Before fun setup() {
        BackendSession.clear()
        server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
        server.createContext("/") { exchange ->
            calls++;query=exchange.requestURI.rawQuery.orEmpty();path=exchange.requestURI.path
            authorization=exchange.requestHeaders.getFirst("Authorization");cookie=exchange.requestHeaders.getFirst("Cookie")
            val bytes=json.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.set("Content-Type","application/json; charset=utf-8")
            exchange.sendResponseHeaders(code,bytes.size.toLong());exchange.responseBody.use {it.write(bytes)}
        }
        server.start()
        adapter=BigDataCloudGeocoder(createGeocodingApi("http://127.0.0.1:${server.address.port}/"),Clock.fixed(now,ZoneOffset.UTC)) {elapsed}
    }
    @After fun cleanup() { server.stop(0);BackendSession.clear() }
    @Test fun sendsOnlyCoordinateQueriesAndNeverGatewayCredentials()=runBlocking {
        BackendSession.setToken("FAKE-APP-JWT",listOf("site-1"))
        assertEquals(GeocodingResult.Found("Localidad ficticia, Región ficticia, Chile"),adapter.lookup(position))
        assertEquals("/data/reverse-geocode-client",path)
        assertEquals(setOf("latitude=-33.0","longitude=-71.0","localityLanguage=es"),query.split('&').toSet())
        assertNull(authorization);assertNull(cookie);assertNotNull(BackendSession.bearerHeader())
    }
    @Test fun fallsBackToCityAndAvoidsDuplicateNames()=runBlocking {
        json="""{"latitude":-33,"longitude":-71,"lookupSource":"reverseGeocoding","locality":"","city":"Ciudad ficticia","principalSubdivision":"CIUDAD FICTICIA","countryName":"Chile"}"""
        assertEquals(GeocodingResult.Found("Ciudad ficticia, Chile"),adapter.lookup(position))
    }
    @Test fun rejectsIpFallbackWrongCoordinatesMissingNamesAndMalformedJson()=runBlocking {
        val original=json
        for(value in listOf(original.replace("coordinates","ipGeolocation"),original.replace("-33.0","-34.0"),
            original.replace("Localidad ficticia"," ").replace("Ciudad ficticia"," "),"{}","not-json",
            original.replace("Localidad ficticia","x".repeat(161)).replace("Ciudad ficticia",""))) {
            elapsed+=6_000_000_000L;json=value
            assertEquals(GeocodingResult.Failed(GeocodingFailure.INVALID_RESPONSE),adapter.lookup(position))
        }
    }
    @Test fun externalRejectionsNeverLogOutApplicationAccount()=runBlocking {
        BackendSession.setToken("FAKE-APP-JWT",listOf("site-1"))
        for(status in listOf(401,402,403,429,500)) {
            code=status;elapsed+=6_000_000_000L
            val expected=if(status in listOf(402,429)) GeocodingFailure.LIMITED else GeocodingFailure.UNAVAILABLE
            assertEquals(GeocodingResult.Failed(expected),adapter.lookup(position))
            assertEquals("Bearer FAKE-APP-JWT",BackendSession.bearerHeader())
        }
    }
    @Test fun rejectsOldFutureInvalidOrInaccuratePositionBeforeHttp()=runBlocking {
        for(point in listOf(position.copy(measuredAt=now.minusSeconds(61)),position.copy(measuredAt=now.plusSeconds(1)),
            position.copy(accuracyMeters=101.0),position.copy(accuracyMeters=Double.NaN),
            position.copy(coordinates=Coordinates(Double.NaN,-71.0)))) {
            assertEquals(GeocodingResult.Failed(GeocodingFailure.STALE_POSITION),adapter.lookup(point))
        }
        assertEquals(0,calls)
        assertTrue(adapter.lookup(position.copy(measuredAt=now.minusSeconds(60))) is GeocodingResult.Found)
    }
    @Test fun throttlesRepeatedUserRequestsWithoutSleepingOrSendingAnotherQuery()=runBlocking {
        assertTrue(adapter.lookup(position) is GeocodingResult.Found)
        assertEquals(GeocodingResult.Failed(GeocodingFailure.LIMITED),adapter.lookup(position));assertEquals(1,calls)
        elapsed=5_000_000_000L;assertTrue(adapter.lookup(position) is GeocodingResult.Found);assertEquals(2,calls)
    }
    @Test fun propagatesCancellationInsteadOfReportingNetworkFailure()=runBlocking {
        val api=object:BigDataCloudApi {
            override suspend fun reverse(latitude:Double,longitude:Double,language:String):retrofit2.Response<LocalityResponse> = throw CancellationException("test cancellation")
        }
        try { BigDataCloudGeocoder(api,Clock.fixed(now,ZoneOffset.UTC)).lookup(position);fail("Cancellation must propagate") }
        catch(_:CancellationException) {}
    }
    @Test fun timeoutIsRecoverableAndDoesNotUseIpFallback()=runBlocking {
        val api=object:BigDataCloudApi {
            override suspend fun reverse(latitude:Double,longitude:Double,language:String):retrofit2.Response<LocalityResponse> = throw SocketTimeoutException("test timeout")
        }
        assertEquals(GeocodingResult.Failed(GeocodingFailure.NETWORK),BigDataCloudGeocoder(api,Clock.fixed(now,ZoneOffset.UTC)).lookup(position))
    }
}
