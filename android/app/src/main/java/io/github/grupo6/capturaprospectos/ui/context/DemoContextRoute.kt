package io.github.grupo6.capturaprospectos.ui.context

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Selector y editor comparten el dueño retenido del contexto; no seleccionan antes del resultado real. */
@Composable fun DemoContextRoute(viewModel:DemoContextViewModel,modifier:Modifier=Modifier,onContinueToHub:()->Unit={},showProfiles:Boolean=true) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DemoContextScreen(state, {viewModel.initialize()},{viewModel.retry()},{viewModel.selectProfile(it)},
        {viewModel.selectEvent(it)},{viewModel.clearSelection()},modifier,onContinueToHub,
        onCreateEvent={viewModel.beginEventEditor()},showProfiles=showProfiles)
    EventEditorRoute(viewModel)
}
