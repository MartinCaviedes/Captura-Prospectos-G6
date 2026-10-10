package io.github.grupo6.capturaprospectos.ui.hub

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.grupo6.capturaprospectos.domain.capture.CaptureEvent
import io.github.grupo6.capturaprospectos.domain.context.DemoProfile
import io.github.grupo6.capturaprospectos.domain.device.ConnectivityObserver
import io.github.grupo6.capturaprospectos.domain.device.ConnectivityStatus

@Composable
fun MainHubRoute(
    profile: DemoProfile,
    event: CaptureEvent,
    connectivityObserver: ConnectivityObserver,
    onNewCapture: () -> Unit,
    onChangeContext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val connectivity by connectivityObserver.observe().collectAsStateWithLifecycle(initialValue = ConnectivityStatus.UNKNOWN)
    MainHubScreen(
        state = MainHubUiState(
            profile = profile,
            event = event,
            connectivity = connectivity,
        ),
        onNewCapture = onNewCapture,
        onChangeContext = onChangeContext,
        modifier = modifier,
    )
}
