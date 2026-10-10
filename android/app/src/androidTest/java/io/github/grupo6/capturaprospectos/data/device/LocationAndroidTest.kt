package io.github.grupo6.capturaprospectos.data.device

import androidx.test.platform.app.InstrumentationRegistry
import io.github.grupo6.capturaprospectos.domain.device.LocationResult
import java.time.Duration
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test

class LocationAndroidTest {
    @Test
    fun handlesSystemLocationSafelyReturningTypedResult() = runBlocking {
        val provider = AndroidLocationProvider(InstrumentationRegistry.getInstrumentation().targetContext)
        // Solicitud acotada a 1 segundo para verificar manejo del recurso sin colgar la suite.
        val result = provider.request(Duration.ofSeconds(1))
        assertNotNull(result)
        // El resultado debe ser una de las variantes del contrato tipado (Candidate, PermissionDenied, ServicesDisabled, TimedOut, Unavailable)
        val isValidVariant = result is LocationResult.Candidate ||
            result is LocationResult.PermissionDenied ||
            result is LocationResult.ServicesDisabled ||
            result is LocationResult.TimedOut ||
            result is LocationResult.Unavailable
        org.junit.Assert.assertTrue(isValidVariant)
    }
}
