package jp.uhimania.qrreader.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import jp.uhimania.qrreader.R

@Composable
fun ScannedListScreen(
    onStartScanning: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScannedListViewModel = viewModel(factory = ScannedListViewModel.Factory),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onStartScanning
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = Icons.Filled.Add.name
                )
            }
        }
    ) { innerPadding ->
        val uiState by viewModel.uiState.collectAsState()

        if (uiState.results.isEmpty()) {
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
                    Text(it.text)
                }
            }
        }
    }
}
