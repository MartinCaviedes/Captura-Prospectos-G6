package io.github.grupo6.capturaprospectos.ui.context

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.grupo6.capturaprospectos.domain.capture.LocalRole
import io.github.grupo6.capturaprospectos.domain.context.DemoContextState
import io.github.grupo6.capturaprospectos.domain.context.SaveEventResult

/** El punto de composición entregará un ViewModel con dueño/fábrica explícitos. */
@Composable
fun DemoContextRoute(
    viewModel: DemoContextViewModel,
    modifier: Modifier = Modifier,
    onContinueToHub: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showEventDialog by remember { mutableStateOf(false) }

    DemoContextScreen(
        state = state,
        onInitialize = { viewModel.initialize() },
        onRetry = { viewModel.retry() },
        onProfileSelected = { viewModel.selectProfile(it) },
        onEventSelected = { viewModel.selectEvent(it) },
        onClearSelection = { viewModel.clearSelection() },
        onCreateEvent = { showEventDialog = true },
        modifier = modifier,
        onContinueToHub = onContinueToHub,
    )

    if (showEventDialog) {
        val ready = state.context as? DemoContextState.Ready
        val profile = ready?.snapshot?.currentProfile
        if (ready != null && profile != null && profile.actor.role == LocalRole.SUPERVISOR) {
            val authorizedSites = ready.snapshot.catalog.sites.filter { it.id in profile.actor.siteIds }
            val captors = ready.snapshot.profiles.filter { it.actor.role == LocalRole.CAPTOR }
            EventAdminDialog(
                availableSites = authorizedSites,
                availableCaptors = captors,
                onDismiss = { showEventDialog = false },
                onSave = { cmd ->
                    viewModel.saveEvent(cmd) { result ->
                        if (result is SaveEventResult.Success) {
                            showEventDialog = false
                            viewModel.selectEvent(result.eventId)
                        }
                    }
                },
            )
        }
    }
}
