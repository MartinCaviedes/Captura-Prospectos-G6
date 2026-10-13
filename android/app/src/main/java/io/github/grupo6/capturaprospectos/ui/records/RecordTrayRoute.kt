package io.github.grupo6.capturaprospectos.ui.records

import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler
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
    BackHandler {
        if (!state.isMutating) {
            if (state.isDetailOpen) viewModel.closeDetail() else onNavigateBack()
        }
    }
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
        onSaveEditor = { viewModel.saveEditor(it) },
        onEditReason = viewModel::updateEditReason,
        onReloadDetail = { viewModel.requestEditDiscard(reload = true) },
        onDiscardEditConfirmation = viewModel::confirmEditDiscard,
        onCancellationReason = viewModel::updateCancellationReason,
        modifier = modifier,
        onLoadMore = { viewModel.loadMore() },
        onRefresh = { viewModel.refresh() },
    )
}
