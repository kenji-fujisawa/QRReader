package jp.uhimania.qrreader.ui.reader

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import jp.uhimania.qrreader.QRReaderApplication
import jp.uhimania.qrreader.data.DefaultScannedResultRepository
import jp.uhimania.qrreader.data.ScannedResult
import jp.uhimania.qrreader.data.ScannedResultRepository
import jp.uhimania.qrreader.domain.DefaultGetPageTitleUseCase
import jp.uhimania.qrreader.domain.GetPageTitleUseCase
import jp.uhimania.qrreader.domain.ValidateUrlUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QRReaderUiState(
    val decodedText: String? = null,
    val isUrl: Boolean = false,
    val barCodeRect: Rect? = null,
    val imageSize: Size = Size.Zero
)

class QRReaderViewModel(
    private val repository: ScannedResultRepository,
    private val validateUrlUseCase: ValidateUrlUseCase,
    private val getPageTitleUseCase: GetPageTitleUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(QRReaderUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigateToBack = MutableSharedFlow<Unit>()
    val navigateToBack = _navigateToBack.asSharedFlow()

    fun updateDecodedText(text: String?) {
        if (text != _uiState.value.decodedText) {
            _uiState.update { it.copy(decodedText = text, isUrl = validateUrlUseCase(text ?: "")) }
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

    fun saveResult() {
        _uiState.value.decodedText?.let {
            viewModelScope.launch {
                val title = if (!_uiState.value.isUrl) "" else getPageTitleUseCase(it)
                val result = ScannedResult(text = it, title = title)
                repository.saveResult(result)

                _navigateToBack.emit(Unit)
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as QRReaderApplication
                val repository = DefaultScannedResultRepository(app.source)
                val validateUrlUseCase = ValidateUrlUseCase()
                val getPageTitleUseCase = DefaultGetPageTitleUseCase()
                QRReaderViewModel(repository, validateUrlUseCase, getPageTitleUseCase)
            }
        }
    }
}
