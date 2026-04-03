package jp.uhimania.qrreader.ui

import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

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
