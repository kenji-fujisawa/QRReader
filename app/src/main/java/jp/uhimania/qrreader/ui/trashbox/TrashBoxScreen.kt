package jp.uhimania.qrreader.ui.trashbox

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import jp.uhimania.qrreader.R
import jp.uhimania.qrreader.ui.common.LoadingScreen
import jp.uhimania.qrreader.ui.common.ScannedResultCard
import jp.uhimania.qrreader.ui.common.ScannedResultUiState
import jp.uhimania.qrreader.ui.theme.QRReaderTheme

@Composable
fun TrashBoxScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrashBoxViewModel = viewModel(factory = TrashBoxViewModel.Factory),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedItem by remember { mutableStateOf<ScannedResultUiState?>(null) }
    var showConfirmBulkRemove by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AnimatedContent(targetState = uiState.state) {
                when (it) {
                    TrashBoxScreenState.Normal -> {
                        DefaultAppBar(
                            results = uiState.results,
                            onBack = onBack,
                            onEnterRestoreMode = { viewModel.setScreenState(TrashBoxScreenState.RestoreMode) },
                            onEnterRemoveMode = { viewModel.setScreenState(TrashBoxScreenState.ForceRemoveMode) }
                        )
                    }
                    TrashBoxScreenState.RestoreMode -> {
                        RestoreModeAppBar(
                            onExitRestoreMode = {
                                viewModel.setScreenState(TrashBoxScreenState.Normal)
                                viewModel.clearSelection()
                            }
                        )
                    }
                    TrashBoxScreenState.ForceRemoveMode -> {
                        RemoveModeAppBar(
                            onExitRemoveMode = {
                                viewModel.setScreenState(TrashBoxScreenState.Normal)
                                viewModel.clearSelection()
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            AnimatedContent(targetState = uiState.state) {
                when (it) {
                    TrashBoxScreenState.Normal -> {}
                    TrashBoxScreenState.RestoreMode -> {
                        FloatingActionButton(
                            onClick = {
                                viewModel.restoreSelected()
                                viewModel.setScreenState(TrashBoxScreenState.Normal)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = Icons.Default.Restore.name
                            )
                        }
                    }
                    TrashBoxScreenState.ForceRemoveMode -> {
                        FloatingActionButton(
                            onClick = {
                                if (uiState.results.count { res -> res.selected } > 0) {
                                    showConfirmBulkRemove = true
                                } else {
                                    viewModel.setScreenState(TrashBoxScreenState.Normal)
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = Icons.Default.Delete.name
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else if (uiState.results.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.no_deleted_items))
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding)
            ) {
                items(uiState.results) { result ->
                    ResultItem(
                        result = result,
                        showCheckBox = uiState.state == TrashBoxScreenState.RestoreMode || uiState.state == TrashBoxScreenState.ForceRemoveMode,
                        onCheckChange = {
                            if (it) {
                                viewModel.select(result.id)
                            } else {
                                viewModel.unselect(result.id)
                            }
                        },
                        onRestore = { viewModel.restore(it.id) },
                        onDelete = { selectedItem = it },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            selectedItem?.let {
                ConfirmDialog(
                    onDismissRequest = { selectedItem = null },
                    onConfirmDelete = {
                        viewModel.forceRemove(it.id)
                        selectedItem = null
                    }
                )
            }

            if (showConfirmBulkRemove) {
                ConfirmDialog(
                    onDismissRequest = { showConfirmBulkRemove = false },
                    onConfirmDelete = {
                        viewModel.forceRemoveSelected()
                        viewModel.setScreenState(TrashBoxScreenState.Normal)
                        showConfirmBulkRemove = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultAppBar(
    results: List<ScannedResultUiState>,
    onBack: () -> Unit,
    onEnterRestoreMode: () -> Unit,
    onEnterRemoveMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    CenterAlignedTopAppBar(
        title = { Text(stringResource(R.string.title_trash_box)) },
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = Icons.AutoMirrored.Filled.ArrowBack.name
                )
            }
        },
        actions = {
            var expanded by remember { mutableStateOf(false) }

            IconButton(onClick = { expanded = !expanded}) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = Icons.Default.MoreVert.name
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.title_restore_mode)) },
                        onClick = {
                            onEnterRestoreMode()
                            expanded = false
                        },
                        enabled = !results.isEmpty()
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.title_remove_mode),
                                style = if (results.isEmpty()) {
                                    TextStyle.Default
                                } else {
                                    TextStyle.Default.copy(color = Color.Red)
                                }
                            )
                        },
                        onClick = {
                            onEnterRemoveMode()
                            expanded = false
                        },
                        enabled = !results.isEmpty()
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RestoreModeAppBar(
    onExitRestoreMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    CenterAlignedTopAppBar(
        title = { Text(stringResource(R.string.title_restore_mode)) },
        modifier = modifier,
        actions = {
            IconButton(onClick = onExitRestoreMode) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = Icons.Default.Check.name
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemoveModeAppBar(
    onExitRemoveMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = stringResource(R.string.title_remove_mode),
                color = Color.Red
            )
        },
        modifier = modifier,
        actions = {
            IconButton(onClick = onExitRemoveMode) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = Icons.Default.Check.name
                )
            }
        }
    )
}

@Composable
private fun ResultItem(
    result: ScannedResultUiState,
    showCheckBox: Boolean,
    onCheckChange: (Boolean) -> Unit,
    onRestore: (ScannedResultUiState) -> Unit,
    onDelete: (ScannedResultUiState) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ScannedResultCard(
        uiState = result,
        showCheckBox = showCheckBox,
        onCheckChange = onCheckChange,
        modifier = modifier
    ) {
        IconButton({ expanded = !expanded }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = Icons.Default.MoreVert.name
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.label_restore)) },
                    onClick = {
                        onRestore(result)
                        expanded = false
                    }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.label_force_remove),
                            style = TextStyle.Default.copy(color = Color.Red)
                        )
                    },
                    onClick = {
                        onDelete(result)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmDialog(
    onDismissRequest: () -> Unit,
    onConfirmDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.wrapContentSize(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.text_confirm_delete))

                Spacer(Modifier.height(24.dp))

                Row(Modifier.align(Alignment.End)) {
                    TextButton(onDismissRequest) {
                        Text(stringResource(R.string.caption_cancel))
                    }
                    TextButton(onConfirmDelete) {
                        Text(
                            text = stringResource(R.string.caption_delete),
                            style = TextStyle.Default.copy(color = Color.Red)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ResultItemPreview() {
    QRReaderTheme {
        val result = ScannedResultUiState(text = "aaa")
        ResultItem(
            result = result,
            showCheckBox = false,
            onCheckChange = {},
            onRestore = {},
            onDelete = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfirmDialogPreview() {
    QRReaderTheme {
        ConfirmDialog(
            onDismissRequest = {},
            onConfirmDelete = {}
        )
    }
}
