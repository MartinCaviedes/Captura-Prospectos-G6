package io.github.grupo6.capturaprospectos.data.device

import io.github.grupo6.capturaprospectos.domain.capture.*
import io.github.grupo6.capturaprospectos.domain.device.*
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidAddressGeocoderTest {
    private val now=Instant.parse("2026-11-22T16:00:00Z")
    private val point=CaptureLocation.Device(Coordinates(-33.0,-71.0),now,10.0)
    private class Source:NativeAddressLookup {
        var present=true;var pending=false;var auto=true;var calls=0
        var parts:StreetParts?=StreetParts("Calle ficticia","123")
        lateinit var answer:(StreetParts?)->Unit
        lateinit var failure:(GeocodingFailure)->Unit
        override fun isPresent()=present
        override fun request(coordinates:Coordinates,result:(StreetParts?)->Unit,error:(GeocodingFailure)->Unit):Boolean {
            if(pending) return false
            calls++;pending=true
            answer={pending=false;result(it)};failure={pending=false;error(it)}
            if(auto) answer(parts)
            return true
        }
    }
    private fun adapter(source:Source)=AndroidAddressGeocoder(source,Clock.fixed(now,ZoneOffset.UTC))
    @Test fun streetAndOptionalNumberAreReturnedWithoutInventingMissingParts()=runTest {
        val source=Source();val geo=adapter(source)
        assertEquals(GeocodingResult.Found("Calle ficticia 123"),geo.lookup(point))
        source.parts=StreetParts("Calle ficticia",null)
        assertEquals(GeocodingResult.Found("Calle ficticia"),geo.lookup(point))
    }
    @Test fun missingStreetOrInvalidNamesDoNotBecomeAnAddress()=runTest {
        val source=Source();val geo=adapter(source)
        for(parts in listOf(null,StreetParts(null,"123"),StreetParts(" ","123"),StreetParts("x".repeat(161),null),StreetParts("Calle\ninyectada",null))) {
            source.parts=parts;assertEquals(GeocodingResult.Failed(GeocodingFailure.UNAVAILABLE),geo.lookup(point))
        }
    }
    @Test fun absentServiceAndStalePositionDoNotStartNativeLookup()=runTest {
        val source=Source().apply {present=false};val geo=adapter(source)
        assertEquals(GeocodingResult.Failed(GeocodingFailure.UNAVAILABLE),geo.lookup(point))
        source.present=true
        assertEquals(GeocodingResult.Failed(GeocodingFailure.STALE_POSITION),geo.lookup(point.copy(measuredAt=now.minusSeconds(61))))
        assertEquals(0,source.calls)
    }
    @Test fun timeoutKeepsAtMostOneNativeOperationPendingAndIgnoresLateResult()=runTest {
        val source=Source().apply {auto=false};val geo=adapter(source)
        val first=async {geo.lookup(point)};runCurrent();advanceTimeBy(4999);runCurrent();assertFalse(first.isCompleted)
        advanceTimeBy(1);runCurrent();assertEquals(GeocodingResult.Failed(GeocodingFailure.UNAVAILABLE),first.await())
        assertEquals(GeocodingResult.Failed(GeocodingFailure.LIMITED),geo.lookup(point));assertEquals(1,source.calls)
        source.answer(StreetParts("Respuesta tardía","99"))
        source.auto=true;assertEquals(GeocodingResult.Found("Calle ficticia 123"),geo.lookup(point));assertEquals(2,source.calls)
    }
    @Test fun callerCancellationPropagatesAndLateCallbackDoesNotResumeAgain()=runTest {
        val source=Source().apply {auto=false};val task=async {adapter(source).lookup(point)};runCurrent()
        task.cancelAndJoin();assertTrue(task.isCancelled)
        source.answer(StreetParts("Respuesta tardía","99"));assertEquals(1,source.calls)
    }
    @Test fun nativeNetworkFailureIsRecoverable()=runTest {
        val source=Source().apply {auto=false};val task=async {adapter(source).lookup(point)};runCurrent()
        source.failure(GeocodingFailure.NETWORK)
        assertEquals(GeocodingResult.Failed(GeocodingFailure.NETWORK),task.await())
    }
    @Test fun duplicateNativeCallbacksDoNotResumeTwice()=runTest {
        val source=Source().apply {auto=false};val task=async {adapter(source).lookup(point)};runCurrent()
        source.answer(source.parts);source.failure(GeocodingFailure.NETWORK)
        assertEquals(GeocodingResult.Found("Calle ficticia 123"),task.await())
    }
}
