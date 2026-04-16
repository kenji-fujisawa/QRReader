package jp.uhimania.qrreader.ui.scannedlist

import android.content.ClipData
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import jp.uhimania.qrreader.R
import jp.uhimania.qrreader.ui.common.LoadingScreen
import jp.uhimania.qrreader.ui.common.ScannedResultCard
import jp.uhimania.qrreader.ui.common.ScannedResultUiState
import jp.uhimania.qrreader.ui.theme.QRReaderTheme
import kotlinx.coroutines.launch

@Composable
fun ScannedListScreen(
    onStartScanning: () -> Unit,
    onMoveToTrashBox: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScannedListViewModel = viewModel(factory = ScannedListViewModel.Factory),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedItem by remember { mutableStateOf<ScannedResultUiState?>(null) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AnimatedContent(targetState = uiState.state) {
                when (it) {
                    ScannedListScreenState.Normal -> {
                        DefaultAppBar(
                            results = uiState.results,
                            onEnterRemoveMode = { viewModel.setScreenState(ScannedListScreenState.RemoveMode) },
                            onMoveToTrashBox = onMoveToTrashBox
                        )
                    }
                    ScannedListScreenState.RemoveMode -> {
                        RemoveModeAppBar(
                            onExitRemoveMode = {
                                viewModel.setScreenState(ScannedListScreenState.Normal)
                                viewModel.clearSelection()
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(!uiState.isLoading) {
                AnimatedContent(targetState = uiState.state) {
                    when (it) {
                        ScannedListScreenState.Normal -> {
                            FloatingActionButton(onClick = onStartScanning) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = Icons.Filled.Add.name
                                )
                            }
                        }
                        ScannedListScreenState.RemoveMode -> {
                            FloatingActionButton(
                                onClick = {
                                    viewModel.removeSelected()
                                    viewModel.setScreenState(ScannedListScreenState.Normal)
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
                Text(stringResource(R.string.no_scanned_results))
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding)
            ) {
                items(uiState.results) { result ->
                    ResultItem(
                        result = result,
                        showCheckBox = uiState.state == ScannedListScreenState.RemoveMode,
                        onCheckChange = {
                            if (it) {
                                viewModel.select(result.id)
                            } else {
                                viewModel.unselect(result.id)
                            }
                        },
                        onEditTitle = { selectedItem = it },
                        onRemove = { viewModel.remove(it.id) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            selectedItem?.let {
                TitleEditDialog(
                    title = it.title,
                    onDismissRequest = { selectedItem = null },
                    onTitleFixed = { title ->
                        viewModel.updateTitle(it.id, title)
                        selectedItem = null
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
    onEnterRemoveMode: () -> Unit,
    onMoveToTrashBox: () -> Unit,
    modifier: Modifier = Modifier
) {
    CenterAlignedTopAppBar(
        title = { Text(stringResource(R.string.title_scanned_list)) },
        modifier = modifier,
        actions = {
            var expanded by remember { mutableStateOf(false) }

            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = Icons.Default.MoreVert.name
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.title_remove_mode)) },
                        onClick = {
                            onEnterRemoveMode()
                            expanded = false
                        },
                        enabled = !results.isEmpty()
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.title_trash_box)) },
                        onClick = {
                            onMoveToTrashBox()
                            expanded = false
                        }
                    )
                }
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
        title = { Text(stringResource(R.string.title_remove_mode)) },
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
    onEditTitle: (ScannedResultUiState) -> Unit,
    onRemove: (ScannedResultUiState) -> Unit,
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
                val clipboard = LocalClipboard.current
                val scope = rememberCoroutineScope()

                DropdownMenuItem(
                    text = { Text(stringResource(R.string.label_edit_title)) },
                    onClick = {
                        onEditTitle(result)
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_label_copy)) },
                    onClick = {
                        scope.launch {
                            val data = ClipData.newPlainText(result.text, result.text)
                            clipboard.setClipEntry(data.toClipEntry())
                            expanded = false
                        }
                    }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.label_remove)) },
                    onClick = {
                        onRemove(result)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TitleEditDialog(
    title: String,
    onDismissRequest: () -> Unit,
    onTitleFixed: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf(title) }

    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.wrapContentSize(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(Modifier.padding(16.dp)) {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(stringResource(R.string.text_entry_title)) }
                )
                Row(Modifier.align(Alignment.End)) {
                    TextButton(onDismissRequest) {
                        Text(stringResource(R.string.caption_cancel))
                    }
                    TextButton({ onTitleFixed(text) }) {
                        Text(stringResource(R.string.caption_ok))
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
            onEditTitle = {},
            onRemove = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TitleEditDialogPreview() {
    QRReaderTheme {
        TitleEditDialog(
            title = "title",
            onDismissRequest = {},
            onTitleFixed = {}
        )
    }
}
