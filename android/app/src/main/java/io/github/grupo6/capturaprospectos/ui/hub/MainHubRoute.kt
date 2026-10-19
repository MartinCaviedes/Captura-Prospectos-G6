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
    suppliedMetrics: MainHubMetricsViewModel? = null,
    contextViewModel: io.github.grupo6.capturaprospectos.ui.context.DemoContextViewModel? = null,
    summaryOnly: Boolean = false,
    onOpenSummary: () -> Unit = {},
) {
    // Conservar el Flow evita cancelar/recrear el callback de red en cada recomposición.
    val connectivityFlow = remember(connectivityObserver) { connectivityObserver.observe() }
    val connectivity by connectivityFlow.collectAsStateWithLifecycle(initialValue = ConnectivityStatus.UNKNOWN)
    val localStore = remember(profile.actor, event, recordRepository) { androidx.lifecycle.ViewModelStore() }
    val metricsVm = suppliedMetrics ?: remember(localStore) {
        androidx.lifecycle.ViewModelProvider(localStore, object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                MainHubMetricsViewModel(recordRepository, event) as T
        })[MainHubMetricsViewModel::class.java]
    }
    val metricsState by metricsVm.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(metricsVm) { if (suppliedMetrics != null) metricsVm.refresh() }
    DisposableEffect(localStore) { onDispose { localStore.clear() } }
    MainHubScreen(
        state = MainHubUiState(
            profile = profile,
            event = event,
            connectivity = connectivity,
            metrics = metricsState.metrics,
            isLoadingMetrics = metricsState.isLoading, metricsError = metricsState.error,
            fromDate = metricsState.fromDate, untilDate = metricsState.untilDate, showRange = metricsState.showRange,
        ),
        onNewCapture = onNewCapture,
        onChangeContext = onChangeContext,
        modifier = modifier,
        summaryOnly = summaryOnly, onOpenSummary = onOpenSummary,
        calendar = metricsVm.calendar, onToggleRange = metricsVm::toggleRange,
        onOpenTray = onOpenTray,
        onCreateEvent = { contextViewModel?.beginEventEditor() },
        onEditEvent = { contextViewModel?.beginEventEditor(edit=true) },
        onRefreshMetrics = { metricsVm.refresh() },
        onFromDate = metricsVm::fromDate, onUntilDate = metricsVm::untilDate,
        onToday = { metricsVm.today() }, onAll = { metricsVm.all() }, onRange = { metricsVm.range() },
    )
    contextViewModel?.let { io.github.grupo6.capturaprospectos.ui.context.EventEditorRoute(it) }
}
