package io.github.grupo6.capturaprospectos.data.device

import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import io.github.grupo6.capturaprospectos.domain.device.ConnectivityStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import org.junit.Test
import org.junit.Assert.assertEquals

/** Prueba asistida: Wi-Fi validado al comenzar; apagar/restaurar externamente, sin red móvil alternativa. */
class ConnectivityTransitionTest {
    @Test fun reportsRealLossAndRecovery() = runBlocking {
        org.junit.Assume.assumeTrue("Requiere orquestación externa autorizada de Wi-Fi",
            InstrumentationRegistry.getArguments().getString("g6WifiTransitions") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val statuses = Channel<ConnectivityStatus>(Channel.UNLIMITED)
        val observer = launch { AndroidConnectivityObserver(instrumentation.targetContext).observe().collect { statuses.send(it) } }
        suspend fun waitFor(expected: ConnectivityStatus) = withTimeout(60000) {
            var next: ConnectivityStatus
            do { next = statuses.receive() } while (next != expected)
            assertEquals(expected, next)
        }
        fun phase(value: String) = instrumentation.sendStatus(2, Bundle().apply { putString("network_phase", value) })
        try {
            waitFor(ConnectivityStatus.VALIDATED); phase("READY_FOR_WIFI_OFF")
            waitFor(ConnectivityStatus.OFFLINE); phase("READY_FOR_WIFI_ON")
            waitFor(ConnectivityStatus.VALIDATED); phase("RECOVERED")
        } finally { observer.cancelAndJoin(); statuses.close() }
    }
}
