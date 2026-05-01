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
import jp.uhimania.qrreader.domain.DefaultGetPagePreviewUseCase
import jp.uhimania.qrreader.domain.GetPagePreviewUseCase
import jp.uhimania.qrreader.domain.ValidateUrlUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QRReaderUiState(
    val codes: List<Code> = listOf(),
    val bounds: List<Rect> = listOf(),
    val imageSize: Size = Size.Zero,
    val isLoading: Boolean = false
) {
    data class Code(
        val text: String = "",
        val isUrl: Boolean = false
    )
}

class QRReaderViewModel(
    private val repository: ScannedResultRepository,
    private val validateUrlUseCase: ValidateUrlUseCase,
    private val getPagePreviewUseCase: GetPagePreviewUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(QRReaderUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigateToBack = MutableSharedFlow<Unit>()
    val navigateToBack = _navigateToBack.asSharedFlow()

    fun updateCodeTexts(texts: List<String>) {
        val old = _uiState.value.codes.map { it.text }
        if (old != texts) {
            _uiState.update {
                it.copy(
                    codes = texts.map { text ->
                        QRReaderUiState.Code(
                            text = text,
                            isUrl = validateUrlUseCase(text)
                        )
                    }
                )
            }
        }
    }

    fun updateCodeBounds(bounds: List<Rect>) {
        _uiState.update { it.copy(bounds = bounds) }
    }

    fun updateImageSize(size: Size) {
        if (size != _uiState.value.imageSize) {
            _uiState.update { it.copy(imageSize = size) }
        }
    }

    fun saveResult() {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            _uiState.value.codes.forEach { code ->
                val result = if (!code.isUrl) {
                    ScannedResult(text = code.text)
                } else {
                    try {
                        val preview = getPagePreviewUseCase(code.text)
                        ScannedResult(
                            text = code.text,
                            title = preview.title,
                            description = preview.description,
                            image = preview.image
                        )
                    } catch (_: Exception) {
                        ScannedResult(text = code.text)
                    }
                }

                repository.saveResult(result)
            }

            _navigateToBack.emit(Unit)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as QRReaderApplication
                val repository = DefaultScannedResultRepository(app.source)
                val validateUrlUseCase = ValidateUrlUseCase()
                val getPagePreviewUseCase = DefaultGetPagePreviewUseCase()
                QRReaderViewModel(repository, validateUrlUseCase, getPagePreviewUseCase)
            }
        }
    }
}
