package jp.uhimania.qrreader.ui

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import jp.uhimania.qrreader.ui.theme.QRReaderTheme
import kotlinx.coroutines.launch

@Composable
fun ScannedListScreen(
    onStartScanning: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScannedListViewModel = viewModel(factory = ScannedListViewModel.Factory),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else if (uiState.results.isEmpty()) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.no_scanned_results))
            }
        } else {
            LazyColumn(
                modifier = modifier.padding(innerPadding)
            ) {
                items(uiState.results) {
                    ResultItem(
                        result = it,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ResultItem(
    result: ScannedListUiState.ScannedResult,
    modifier: Modifier = Modifier
) {
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
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingScreenPreview() {
    QRReaderTheme {
        LoadingScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun ResultItemPreview() {
    QRReaderTheme {
        val result = ScannedListUiState.ScannedResult(text = "aaa")
        ResultItem(result)
    }
}
