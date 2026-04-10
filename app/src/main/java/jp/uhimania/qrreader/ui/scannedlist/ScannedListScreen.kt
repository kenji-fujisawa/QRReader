package jp.uhimania.qrreader.ui.scannedlist

import android.content.ClipData
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import jp.uhimania.qrreader.R
import jp.uhimania.qrreader.domain.DateFormat
import jp.uhimania.qrreader.ui.common.LoadingScreen
import jp.uhimania.qrreader.ui.theme.QRReaderTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannedListScreen(
    onStartScanning: () -> Unit,
    onMoveToTrashBox: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScannedListViewModel = viewModel(factory = ScannedListViewModel.Factory),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(stringResource(R.string.title_scanned_list))
                },
                actions = {
                    IconButton(onClick = onMoveToTrashBox) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = Icons.Default.Delete.name
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(!uiState.isLoading) {
                FloatingActionButton(
                    onClick = onStartScanning
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = Icons.Filled.Add.name
                    )
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
                        onRemove = { viewModel.remove(it) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultItem(
    result: ScannedListUiState.ScannedResult,
    onRemove: (ScannedListUiState.ScannedResult) -> Unit,
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

        if (result.isUrl) {
            val uriHandler = LocalUriHandler.current
            TextButton(
                onClick = { uriHandler.openUri(result.text) }
            ) {
                Text(stringResource(R.string.action_label_open))
            }
        } else {
            val clipboard = LocalClipboard.current
            val scope = rememberCoroutineScope()
            TextButton(
                onClick = {
                    scope.launch {
                        val data = ClipData.newPlainText(result.text, result.text)
                        clipboard.setClipEntry(data.toClipEntry())
                    }
                }
            ) {
                Text(stringResource(R.string.action_label_copy))
            }
        }

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

@Preview(showBackground = true)
@Composable
private fun ResultItemPreview() {
    QRReaderTheme {
        val result = ScannedListUiState.ScannedResult(text = "aaa")
        ResultItem(
            result = result,
            onRemove = {}
        )
    }
}
