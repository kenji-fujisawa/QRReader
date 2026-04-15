package jp.uhimania.qrreader.ui.scannedlist

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
import jp.uhimania.qrreader.domain.FormatDateUseCase
import jp.uhimania.qrreader.domain.ValidateUrlUseCase
import jp.uhimania.qrreader.ui.common.ScannedResultUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ScannedListUiState(
    val results: List<ScannedResultUiState> = listOf(),
    val isLoading: Boolean = false
)

class ScannedListViewModel(
    private val repository: ScannedResultRepository,
    private val formatDateUseCase: FormatDateUseCase,
    private val validateUrlUseCase: ValidateUrlUseCase
) : ViewModel() {
    val uiState: StateFlow<ScannedListUiState> =
        repository.getResultsStream()
            .map { results ->
                ScannedListUiState(
                    results = results
                        .sortedByDescending { it.scannedDate }
                        .map { toUiState(it) },
                    isLoading = false
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = ScannedListUiState(isLoading = true)
            )

    init {
        viewModelScope.launch {
            repository.purgeExpired()
        }
    }

    private fun toUiState(result: ScannedResult): ScannedResultUiState {
        return ScannedResultUiState(
            id = result.id,
            text = result.text,
            title = result.title,
            isUrl = validateUrlUseCase(result.text),
            date = formatDateUseCase(result.scannedDate)
        )
    }

    fun remove(id: String) {
        viewModelScope.launch {
            repository.markAsDelete(id)
        }
    }

    fun updateTitle(id: String, title: String) {
        viewModelScope.launch {
            repository.updateTitle(id, title)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as QRReaderApplication
                val repository = DefaultScannedResultRepository(app.source)
                val formatDateUseCase = FormatDateUseCase()
                val validateUrlUseCase = ValidateUrlUseCase()
                ScannedListViewModel(repository, formatDateUseCase, validateUrlUseCase)
            }
        }
    }
}
