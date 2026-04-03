package jp.uhimania.qrreader.ui

import android.graphics.Rect
import androidx.compose.ui.geometry.Size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.net.URL

data class QRReaderUiState(
    val decodedText: String? = null,
    val isUrl: Boolean = false,
    val barCodeRect: Rect? = null,
    val imageSize: Size = Size.Zero
)

class QRReaderViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(QRReaderUiState())
    val uiState = _uiState.asStateFlow()

    fun updateDecodedText(text: String?) {
        if (text != _uiState.value.decodedText) {
            _uiState.update { it.copy(decodedText = text, isUrl = isUrl(text)) }
        }
    }

    fun updateBarcodeRect(rect: Rect?) {
        if (rect != _uiState.value.barCodeRect) {
            _uiState.update { it.copy(barCodeRect = rect) }
        }
    }

    fun updateImageSize(size: Size) {
        if (size != _uiState.value.imageSize) {
            _uiState.update { it.copy(imageSize = size) }
        }
    }

    private fun isUrl(text: String?): Boolean {
        return try {
            URL(text)
            true
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                QRReaderViewModel()
            }
        }
    }
}
