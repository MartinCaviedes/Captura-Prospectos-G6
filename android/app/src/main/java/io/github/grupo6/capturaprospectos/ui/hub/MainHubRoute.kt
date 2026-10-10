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
    contextViewModel: io.github.grupo6.capturaprospectos.ui.context.DemoContextViewModel? = null,
) {
    val connectivity by connectivityObserver.observe().collectAsStateWithLifecycle(initialValue = ConnectivityStatus.UNKNOWN)
    var metrics by remember { mutableStateOf<DeviceMetrics?>(null) }
    var isLoadingMetrics by remember { mutableStateOf(false) }
    var showEventDialog by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<CaptureEvent?>(null) }
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
        onCreateEvent = {
            eventToEdit = null
            showEventDialog = true
        },
        onEditEvent = {
            eventToEdit = event
            showEventDialog = true
        },
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

    if (showEventDialog && contextViewModel != null) {
        val ready = contextViewModel.uiState.value.context as? io.github.grupo6.capturaprospectos.domain.context.DemoContextState.Ready
        val snapshot = ready?.snapshot
        val authorizedSites = snapshot?.catalog?.sites?.filter { it.id in profile.actor.siteIds }.orEmpty()
        val captors = snapshot?.profiles?.filter { it.actor.role == io.github.grupo6.capturaprospectos.domain.capture.LocalRole.CAPTOR }.orEmpty()
        io.github.grupo6.capturaprospectos.ui.context.EventAdminDialog(
            eventToEdit = eventToEdit,
            availableSites = authorizedSites,
            availableCaptors = captors,
            onDismiss = { showEventDialog = false },
            onSave = { cmd ->
                contextViewModel.saveEvent(cmd) { result ->
                    if (result is io.github.grupo6.capturaprospectos.domain.context.SaveEventResult.Success) {
                        showEventDialog = false
                        if (recordRepository != null) {
                            scope.launch {
                                metrics = recordRepository.getMetrics(result.eventId)
                            }
                        }
                    }
                }
            },
        )
    }
}
