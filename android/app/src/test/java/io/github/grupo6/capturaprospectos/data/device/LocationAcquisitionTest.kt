package io.github.grupo6.capturaprospectos.data.device

import io.github.grupo6.capturaprospectos.domain.capture.*
import io.github.grupo6.capturaprospectos.domain.device.LocationResult
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class LocationAcquisitionTest {
    private val now=Instant.parse("2026-10-10T15:00:00Z")
    private fun sample(accuracy: Double = 20.0, time: Instant = now)=CaptureLocation.Device(Coordinates(-33.0,-71.0),time,accuracy)
    private class Updates: NativeLocationUpdates {
        val positions=mutableMapOf<NativeLocationSource,(CaptureLocation.Device)->Unit>()
        val disabled=mutableMapOf<NativeLocationSource,()->Unit>()
        val closed=mutableListOf<NativeLocationSource>()
        var immediate: CaptureLocation.Device?=null
        var deniedSource: NativeLocationSource?=null
        override fun isEnabled(source: NativeLocationSource)=true
        override fun subscribe(source: NativeLocationSource,onPosition:(CaptureLocation.Device)->Unit,onDisabled:()->Unit):AutoCloseable {
            if (source == deniedSource) throw SecurityException("Fixture")
            positions[source]=onPosition; disabled[source]=onDisabled; immediate?.let(onPosition)
            return AutoCloseable { closed += source }
        }
    }
    private fun provider(updates: Updates, fine: Boolean = true)=AndroidLocationProvider(
        permissionChecker={ it == "android.permission.ACCESS_COARSE_LOCATION" || fine },updates=updates,now={now})
    @Test fun networkCanResolveWhileGpsIsWaitingAndClosesBothListeners()=runTest {
        val updates=Updates(); val task=async { provider(updates).request(Duration.ofSeconds(10)) }; runCurrent()
        assertEquals(setOf(NativeLocationSource.GPS,NativeLocationSource.NETWORK),updates.positions.keys)
        updates.positions.getValue(NativeLocationSource.NETWORK)(sample())
        assertEquals(LocationResult.Candidate(sample()),task.await()); assertEquals(2,updates.closed.size)
        updates.positions.getValue(NativeLocationSource.GPS)(sample()) // Tardía: no segundo resume.
    }
    @Test fun coarseNeverSubscribesGps()=runTest {
        val updates=Updates(); val task=async { provider(updates,fine=false).request(Duration.ofSeconds(10)) }; runCurrent()
        assertEquals(setOf(NativeLocationSource.NETWORK),updates.positions.keys)
        updates.positions.getValue(NativeLocationSource.NETWORK)(sample()); task.await(); assertEquals(1,updates.closed.size)
    }
    @Test fun impreciseOrStaleNetworkDoesNotStopGpsAndFutureSamplesAreRejected()=runTest {
        val updates=Updates(); val task=async { provider(updates).request(Duration.ofSeconds(10)) }; runCurrent()
        for (bad in listOf(sample(2000.0),sample(time=now.minusSeconds(301)),sample(time=now.plusSeconds(1)))) {
            updates.positions.getValue(NativeLocationSource.NETWORK)(bad); assertFalse(task.isCompleted)
        }
        updates.positions.getValue(NativeLocationSource.GPS)(sample()); assertEquals(LocationResult.Candidate(sample()),task.await())
    }
    @Test fun timeoutAndCancellationRemoveEveryListener()=runTest {
        val updates=Updates(); val task=async { provider(updates).request(Duration.ofSeconds(10)) }; runCurrent()
        advanceTimeBy(10001); assertEquals(LocationResult.TimedOut,task.await()); assertEquals(2,updates.closed.size)
        val cancel=Updates(); val waiting=launch { provider(cancel).request(Duration.ofSeconds(10)) }; runCurrent()
        waiting.cancelAndJoin(); assertEquals(2,cancel.closed.size)
    }
    @Test fun disablingOneProviderKeepsOtherActiveAndAllDisabledIsExplicit()=runTest {
        val updates=Updates(); val task=async { provider(updates).request(Duration.ofSeconds(10)) }; runCurrent()
        updates.disabled.getValue(NativeLocationSource.GPS)(); assertFalse(task.isCompleted)
        updates.disabled.getValue(NativeLocationSource.NETWORK)(); assertEquals(LocationResult.ServicesDisabled,task.await())
        assertEquals(2,updates.closed.size)
    }
    @Test fun synchronousCallbackBeforeRegistrationReturnsClosesSubscription()=runTest {
        val updates=Updates(); updates.immediate=sample()
        assertEquals(LocationResult.Candidate(sample()),provider(updates).request(Duration.ofSeconds(10)))
        assertEquals(listOf(NativeLocationSource.GPS),updates.closed)
    }
    @Test fun timeoutWithOnlyRejectedSampleReportsQualityInsteadOfInventingPosition()=runTest {
        val updates=Updates(); val task=async { provider(updates).request(Duration.ofSeconds(10)) }; runCurrent()
        updates.positions.getValue(NativeLocationSource.NETWORK)(sample(500.0)); advanceTimeBy(10001)
        assertEquals(LocationResult.Candidate(sample(500.0)),task.await()); assertEquals(2,updates.closed.size)
    }
    @Test fun permissionRevokedWhileSubscribingClosesAlreadyRegisteredGps()=runTest {
        val updates=Updates(); updates.deniedSource=NativeLocationSource.NETWORK
        assertEquals(LocationResult.PermissionDenied,provider(updates).request(Duration.ofSeconds(10)))
        assertEquals(listOf(NativeLocationSource.GPS),updates.closed)
    }

}
