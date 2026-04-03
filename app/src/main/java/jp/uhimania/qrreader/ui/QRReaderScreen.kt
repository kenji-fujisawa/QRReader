package jp.uhimania.qrreader.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.graphics.Rect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import jp.uhimania.qrreader.R
import java.net.URL

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "ConfigurationScreenWidthHeight")
@Composable
fun QRReaderScreen(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        val density = LocalDensity.current
        val configuration = LocalConfiguration.current
        val screenSize = with(density) {
            Size(
                width = configuration.screenWidthDp.dp.toPx(),
                height = configuration.screenHeightDp.dp.toPx()
            )
        }

        var decodedText by remember { mutableStateOf<String?>(null) }
        var barcodeRect by remember { mutableStateOf<Rect?>(null) }
        var imageSize by remember { mutableStateOf(Size.Zero) }

        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val cameraProvider = remember {
            CameraProvider(
                context = context,
                lifecycleOwner = lifecycleOwner,
                callback = { barcodes, size ->
                    barcodes.firstOrNull()?.displayValue?.let {
                        if (it != decodedText)
                            decodedText = it
                    }
                    barcodeRect = barcodes.firstOrNull()?.boundingBox
                    imageSize = size
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
            barcodeRect?.let {
                BarcodeMarker(
                    barcodeRect = it,
                    screenSize = screenSize,
                    imageSize = imageSize
                )
            }
        }

        decodedText?.let {
            val urlHandler = LocalUriHandler.current
            val clipboard = LocalClipboard.current
            LaunchedEffect(snackbarHostState, decodedText) {
                val open = context.getString(R.string.action_label_open)
                val copy = context.getString(R.string.action_label_copy)
                val result = snackbarHostState.showSnackbar(
                    message = it,
                    actionLabel = if (it.isURL()) open else copy,
                    duration = SnackbarDuration.Long
                )
                when (result) {
                    SnackbarResult.ActionPerformed -> {
                        if (it.isURL()) {
                            urlHandler.openUri(it)
                        } else {
                            val data = ClipData.newPlainText(it, it)
                            clipboard.setClipEntry(data.toClipEntry())
                        }
                    }
                    SnackbarResult.Dismissed -> {
                        decodedText = null
                    }
                }
            }
        }
    }
}

private fun String.isURL(): Boolean {
    return try {
        URL(this)
        true
    } catch (_: Exception) {
        false
    }
}
