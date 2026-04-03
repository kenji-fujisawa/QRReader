package jp.uhimania.qrreader.ui

import android.annotation.SuppressLint
import android.content.ClipData
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import jp.uhimania.qrreader.R

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "ConfigurationScreenWidthHeight")
@Composable
fun QRReaderScreen(
    modifier: Modifier = Modifier,
    viewModel: QRReaderViewModel = viewModel(factory = QRReaderViewModel.Factory),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        val uiState by viewModel.uiState.collectAsState()
        val density = LocalDensity.current
        val configuration = LocalConfiguration.current
        val screenSize = with(density) {
            Size(
                width = configuration.screenWidthDp.dp.toPx(),
                height = configuration.screenHeightDp.dp.toPx()
            )
        }

        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val cameraProvider = remember {
            CameraProvider(
                context = context,
                lifecycleOwner = lifecycleOwner,
                callback = { barcodes, size ->
                    barcodes.firstOrNull()?.displayValue?.let {
                        viewModel.updateDecodedText(it)
                    }
                    viewModel.updateBarcodeRect(barcodes.firstOrNull()?.boundingBox)
                    viewModel.updateImageSize(size)
                }
            )
        }

        Box(modifier = modifier) {
            CameraPreview(
                screenSize = screenSize,
                update = {
                    cameraProvider.bind(it)
                }
            )
            uiState.barCodeRect?.let {
                BarcodeMarker(
                    barcodeRect = it,
                    screenSize = screenSize,
                    imageSize = uiState.imageSize
                )
            }
        }

        uiState.decodedText?.let {
            val urlHandler = LocalUriHandler.current
            val clipboard = LocalClipboard.current
            LaunchedEffect(snackbarHostState, it) {
                val open = context.getString(R.string.action_label_open)
                val copy = context.getString(R.string.action_label_copy)
                val result = snackbarHostState.showSnackbar(
                    message = it,
                    actionLabel = if (uiState.isUrl) open else copy,
                    duration = SnackbarDuration.Long
                )
                when (result) {
                    SnackbarResult.ActionPerformed -> {
                        if (uiState.isUrl) {
                            urlHandler.openUri(it)
                        } else {
                            val data = ClipData.newPlainText(it, it)
                            clipboard.setClipEntry(data.toClipEntry())
                        }
                    }
                    SnackbarResult.Dismissed -> {
                        viewModel.updateDecodedText(null)
                    }
                }
            }
        }
    }
}
