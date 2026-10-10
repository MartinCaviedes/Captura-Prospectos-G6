package io.github.grupo6.capturaprospectos.ui.hub

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.grupo6.capturaprospectos.domain.capture.CaptureEvent
import io.github.grupo6.capturaprospectos.domain.context.DemoProfile
import io.github.grupo6.capturaprospectos.domain.device.ConnectivityObserver
import io.github.grupo6.capturaprospectos.domain.device.ConnectivityStatus
import io.github.grupo6.capturaprospectos.domain.records.DeviceMetrics
import io.github.grupo6.capturaprospectos.domain.records.RecordRepository
import kotlinx.coroutines.launch

@Composable
fun MainHubRoute(
    profile: DemoProfile,
    event: CaptureEvent,
    connectivityObserver: ConnectivityObserver,
    onNewCapture: () -> Unit,
    onChangeContext: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenTray: () -> Unit = {},
    recordRepository: RecordRepository? = null,
) {
    val connectivity by connectivityObserver.observe().collectAsStateWithLifecycle(initialValue = ConnectivityStatus.UNKNOWN)
    var metrics by remember { mutableStateOf<DeviceMetrics?>(null) }
    var isLoadingMetrics by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(profile.actor.id, event.id, recordRepository) {
        if (recordRepository != null) {
            isLoadingMetrics = true
            try {
                metrics = recordRepository.getMetrics(event.id)
            } finally {
                isLoadingMetrics = false
            }
        }
    }

    MainHubScreen(
        state = MainHubUiState(
            profile = profile,
            event = event,
            connectivity = connectivity,
            metrics = metrics,
            isLoadingMetrics = isLoadingMetrics,
        ),
        onNewCapture = onNewCapture,
        onChangeContext = onChangeContext,
        modifier = modifier,
        onOpenTray = onOpenTray,
        onRefreshMetrics = {
            if (recordRepository != null) {
                scope.launch {
                    isLoadingMetrics = true
                    try {
                        metrics = recordRepository.getMetrics(event.id)
                    } finally {
                        isLoadingMetrics = false
                    }
                }
            }
        },
    )
}
