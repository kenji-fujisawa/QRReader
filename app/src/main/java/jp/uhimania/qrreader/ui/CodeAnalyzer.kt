package jp.uhimania.qrreader.ui

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Size
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class CodeAnalyzer(
    private val scanner: BarcodeScanner,
    private val callback: (List<Barcode>, Size) -> Unit
) : ImageAnalysis.Analyzer {
    @OptIn(ExperimentalGetImage::class)
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
