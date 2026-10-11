package io.github.grupo6.capturaprospectos.data.device

import io.github.grupo6.capturaprospectos.domain.device.LocationResult
import java.time.Duration
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class AndroidLocationProviderTest {

    @Test
    fun rejectsNegativeOrZeroTimeout() = runTest {
        val provider = AndroidLocationProvider(
            permissionChecker = { true },
        )
        try {
            provider.request(Duration.ZERO)
            fail("Debió fallar con timeout cero")
        } catch (e: IllegalArgumentException) {
            assertEquals("Timeout must be positive", e.message)
        }

        try {
            provider.request(Duration.ofSeconds(-1))
            fail("Debió fallar con timeout negativo")
        } catch (e: IllegalArgumentException) {
            assertEquals("Timeout must be positive", e.message)
        }
    }

    @Test
    fun returnsPermissionDeniedWhenNeitherFineNorCoarseGranted() = runTest {
        val provider = AndroidLocationProvider(
            permissionChecker = { false },
        )

        val result = provider.request(Duration.ofSeconds(5))
        assertEquals(LocationResult.PermissionDenied, result)
    }

    @Test
    fun returnsUnavailableWhenLocationManagerIsNull() = runTest {
        val provider = AndroidLocationProvider(
            permissionChecker = { true },
            locationManager = null,
        )

        val result = provider.request(Duration.ofSeconds(5))
        assertEquals(LocationResult.Unavailable, result)
    }
}
