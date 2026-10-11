package io.github.grupo6.capturaprospectos.ui.context

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** El punto de composición entregará un ViewModel con dueño/fábrica explícitos. */
@Composable
fun DemoContextRoute(
    viewModel: DemoContextViewModel,
    modifier: Modifier = Modifier,
    onContinueToHub: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DemoContextScreen(
        state = state,
        onInitialize = { viewModel.initialize() },
        onRetry = { viewModel.retry() },
        onProfileSelected = { viewModel.selectProfile(it) },
        onEventSelected = { viewModel.selectEvent(it) },
        onClearSelection = { viewModel.clearSelection() },
        modifier = modifier,
        onContinueToHub = onContinueToHub,
    )
}
