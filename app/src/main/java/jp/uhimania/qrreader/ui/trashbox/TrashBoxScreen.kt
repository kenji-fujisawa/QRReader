package jp.uhimania.qrreader.ui.trashbox

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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import jp.uhimania.qrreader.domain.DateFormat
import jp.uhimania.qrreader.ui.common.LoadingScreen
import jp.uhimania.qrreader.ui.theme.QRReaderTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashBoxScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrashBoxViewModel = viewModel(factory = TrashBoxViewModel.Factory),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(stringResource(R.string.title_trash_box))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = Icons.AutoMirrored.Filled.ArrowBack.name
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        val uiState by viewModel.uiState.collectAsState()
        var selectedItem by remember { mutableStateOf<TrashBoxUiState.ScannedResult?>(null) }

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
                        onRestore = { viewModel.restore(it) },
                        onDelete = { selectedItem = it },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            selectedItem?.let {
                ConfirmDialog(
                    onDismissRequest = { selectedItem = null },
                    onConfirmDelete = {
                        viewModel.forceRemove(it)
                        selectedItem = null
                    }
                )
            }
        }
    }
}

@Composable
private fun ResultItem(
    result: TrashBoxUiState.ScannedResult,
    onRestore: (TrashBoxUiState.ScannedResult) -> Unit,
    onDelete: (TrashBoxUiState.ScannedResult) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Row(modifier = modifier) {
        Column {
            Text(result.text)
            Text(
                text = when (result.date) {
                    is DateFormat.Today -> stringResource(R.string.date_format_today)
                    is DateFormat.DaysAgo -> stringResource(R.string.date_format_days_ago, result.date.day)
                    is DateFormat.MonthsAgo -> stringResource(R.string.date_format_months_ago, result.date.month)
                    is DateFormat.Date -> result.date.date
                },
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        IconButton(
            onClick = { expanded = !expanded }
        ) {
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
        val result = TrashBoxUiState.ScannedResult(text = "aaa")
        ResultItem(
            result = result,
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
