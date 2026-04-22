package jp.uhimania.qrreader.ui.reader

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import jp.uhimania.qrreader.R
import jp.uhimania.qrreader.ui.common.LoadingScreen

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun QRReaderScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QRReaderViewModel = viewModel(factory = QRReaderViewModel.Factory),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigateToBack.collect {
            onBack()
        }
    }

    RequestPermission()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(!uiState.isLoading) {
                LargeFloatingActionButton(
                    onClick = {
                        if (uiState.decodedText != null) {
                            viewModel.saveResult()
                        }
                    },
                    containerColor = if (uiState.decodedText != null) {
                        FloatingActionButtonDefaults.containerColor
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = Icons.Filled.Check.name
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
        } else {
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
                        viewModel.updateBarcodeRect(barcodes.firstOrNull()?.boundingBox?.toComposeRect())
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
                FilledIconButton(
                    onClick = onBack,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = Icons.AutoMirrored.Filled.ArrowBack.name
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
}

@Composable
private fun RequestPermission() {
    val context = LocalContext.current
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(context, request) {
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            request.launch(permission)
        }
    }
}
