package io.github.grupo6.capturaprospectos.ui.records

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun RecordTrayRoute(
    viewModel: RecordTrayViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RecordTrayScreen(
        state = state,
        onQueryChanged = { viewModel.updateQuery(it) },
        onStateFilterSelected = { viewModel.filterState(it) },
        onOpenCancel = { viewModel.openCancelDialog(it) },
        onCloseCancel = { viewModel.closeCancelDialog() },
        onConfirmCancel = { viewModel.confirmCancellation(it) },
        onNavigateBack = onNavigateBack,
        onOpenDetail = { viewModel.openDetail(it) },
        onCloseDetail = { viewModel.closeDetail() },
        onStartEdit = { viewModel.startEditing() },
        onCancelEdit = { viewModel.cancelEditing() },
        onSaveEdit = { input, reason -> viewModel.saveEdit(input, reason) },
        modifier = modifier,
    )
}
