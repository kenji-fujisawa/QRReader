package jp.uhimania.qrreader.ui.reader

import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.viewinterop.AndroidView

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
