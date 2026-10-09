package io.github.grupo6.capturaprospectos.data.device

import androidx.test.platform.app.InstrumentationRegistry
import io.github.grupo6.capturaprospectos.domain.device.ConnectivityStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertNotNull
import org.junit.Test

class ConnectivityAndroidTest {
    @Test fun readsSystemStateAndReleasesCallbacksAcrossRepeatedCollections() = runBlocking {
        val observer = AndroidConnectivityObserver(InstrumentationRegistry.getInstrumentation().targetContext)
        // Más de 100 registros secuenciales detectan callbacks no liberados (límite por UID).
        repeat(110) {
            val value = withTimeout(5000) { observer.observe().first { it != ConnectivityStatus.UNKNOWN } }
            assertNotNull(value)
        }
    }
}
