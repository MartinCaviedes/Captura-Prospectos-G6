package io.github.grupo6.capturaprospectos.ui.hub

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import io.github.grupo6.capturaprospectos.domain.capture.*
import io.github.grupo6.capturaprospectos.domain.context.DemoProfile
import io.github.grupo6.capturaprospectos.domain.device.*
import io.github.grupo6.capturaprospectos.ui.theme.CapturaProspectosG6Theme
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** La composición no debe interrumpir la escucha de red al mostrar un estado nuevo.
 * Usa un proveedor controlado; no cambia redes/permisos ni abre Room. */
class HubConnectivityUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private class CountingObserver(initial: ConnectivityStatus) : ConnectivityObserver {
        val status = MutableStateFlow(initial)
        val starts = AtomicInteger()
        val stops = AtomicInteger()
        override fun observe(): Flow<ConnectivityStatus> = flow {
            starts.incrementAndGet()
            try { emitAll(status) } finally { stops.incrementAndGet() }
        }
    }
    private val actor = LocalActor(UUID(0,1), LocalRole.CAPTOR, setOf(UUID(0,2)))
    private val profile = DemoProfile(actor,"Captador ficticio")
    private val event = CaptureEvent(UUID(0,3),UUID(0,2),"Evento ficticio",Instant.EPOCH,
        Instant.EPOCH.plusSeconds(3600),ZoneOffset.UTC,setOf(actor.id),null)

    @Test fun lossAndRecoveryUpdateBannerWithoutNavigationOrRestartingObservation() {
        val observer = CountingObserver(ConnectivityStatus.VALIDATED)
        compose.setContent { CapturaProspectosG6Theme {
            MainHubRoute(profile,event,observer,{}, {})
        } }
        compose.onNodeWithText("Conexión a internet verificada").assertExists()
        compose.runOnIdle { assertEquals("Una escucha mientras Inicio permanece abierto",1,observer.starts.get()) }
        compose.runOnIdle { observer.status.value = ConnectivityStatus.OFFLINE }
        compose.onNodeWithText("Sin conexión a red (Modo offline)").assertExists()
        compose.runOnIdle { observer.status.value = ConnectivityStatus.CONNECTED_UNVALIDATED }
        compose.onNodeWithText("Red conectada (sin validación completa)").assertExists()
        compose.runOnIdle { observer.status.value = ConnectivityStatus.VALIDATED }
        compose.onNodeWithText("Conexión a internet verificada").assertExists()
        compose.runOnIdle { assertEquals(1,observer.starts.get());assertEquals(0,observer.stops.get()) }
    }

    @Test fun changingObserverReleasesPreviousSubscriptionAndIgnoresItsLateUpdates() {
        val first = CountingObserver(ConnectivityStatus.VALIDATED)
        val second = CountingObserver(ConnectivityStatus.OFFLINE)
        val selected = mutableStateOf<ConnectivityObserver>(first)
        compose.setContent { CapturaProspectosG6Theme {
            MainHubRoute(profile,event,selected.value,{}, {})
        } }
        compose.onNodeWithText("Conexión a internet verificada").assertExists()
        compose.runOnIdle { selected.value = second }
        compose.onNodeWithText("Sin conexión a red (Modo offline)").assertExists()
        compose.runOnIdle { first.status.value = ConnectivityStatus.UNKNOWN }
        compose.onNodeWithText("Sin conexión a red (Modo offline)").assertExists()
        compose.runOnIdle {
            assertEquals(1,first.starts.get());assertEquals(1,first.stops.get())
            assertEquals(1,second.starts.get());assertEquals(0,second.stops.get())
        }
    }
}
