package jp.uhimania.qrreader

import android.content.ClipData
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import jp.uhimania.qrreader.ui.theme.QRReaderTheme
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QRReaderTheme {
                val context = LocalContext.current
                val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
                LaunchedEffect(context, request) {
                    val permission = android.Manifest.permission.CAMERA
                    if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                        request.launch(permission)
                    }
                }

                QRReaderView()
            }
        }
    }
}

@Composable
fun QRReaderView(
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
                    decodedText = barcodes.firstOrNull()?.displayValue
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
                    SnackbarResult.Dismissed -> {}
                }
            }
        }
    }
}

@Composable
fun CameraPreview(
    screenSize: Size,
    update: (PreviewView) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            PreviewView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    screenSize.width.toInt(),
                    screenSize.height.toInt()
                )
            }
        },
        update = update,
        modifier = modifier
    )
}

@Composable
fun BarcodeMarker(
    barcodeRect: Rect,
    screenSize: Size,
    imageSize: Size,
    modifier: Modifier = Modifier
) {
    val scale = maxOf(screenSize.width / imageSize.width, screenSize.height / imageSize.height)
    val diffWidth = (imageSize.width * scale - screenSize.width) / 2f
    val diffHeight = (imageSize.height * scale - screenSize.height) / 2f
    val offset = Offset(barcodeRect.left.toFloat() * scale - diffWidth, barcodeRect.top.toFloat() * scale - diffHeight)
    val size = Size(barcodeRect.width() * scale, barcodeRect.height() * scale)
    Canvas(modifier = modifier) {
        drawRoundRect(
            topLeft = offset,
            size = size,
            cornerRadius = CornerRadius(x = 50f, y = 50f),
            color = Color.Green,
            style = Stroke(8f)
        )
    }
}

class CameraProvider(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    callback: (List<Barcode>, Size) -> Unit
) {
    private val scanner = BarcodeScanning.getClient()
    private val analyzer = CodeAnalyzer(scanner, callback)
    private val workerExecutor = Executors.newSingleThreadExecutor()

    init {
        lifecycleOwner.lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_DESTROY) {
                    workerExecutor.shutdown()
                    scanner.close()
                }
            }
        )
    }

    fun bind(previewView: PreviewView) {
        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .build()

        val preview = Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .build()
        preview.surfaceProvider = previewView.surfaceProvider

        val analysis = ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelector)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        analysis.setAnalyzer(workerExecutor, analyzer)

        val provider = ProcessCameraProvider.getInstance(context).get()
        provider.unbindAll()
        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
    }
}

class CodeAnalyzer(
    private val scanner: BarcodeScanner,
    private val callback: (List<Barcode>, Size) -> Unit
) : ImageAnalysis.Analyzer {
    override fun analyze(proxy: ImageProxy) {
        val image = proxy.image
        if (image == null) {
            proxy.close()
            return
        }

        val size = when (proxy.imageInfo.rotationDegrees) {
            90, 270 -> Size(proxy.height.toFloat(), proxy.width.toFloat())
            else -> Size(proxy.width.toFloat(), proxy.height.toFloat())
        }

        val inputImage = InputImage.fromMediaImage(image, proxy.imageInfo.rotationDegrees)
        scanner.process(inputImage)
            .addOnSuccessListener { callback(it, size) }
            .addOnCompleteListener { proxy.close() }
    }
}

fun String.isURL(): Boolean {
    return try {
        URL(this)
        true
    } catch (_: Exception) {
        false
    }
}
