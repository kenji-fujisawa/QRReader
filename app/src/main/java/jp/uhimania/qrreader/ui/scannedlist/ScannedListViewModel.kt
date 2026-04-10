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
import jp.uhimania.qrreader.domain.DateFormat
import jp.uhimania.qrreader.domain.FormatDateUseCase
import jp.uhimania.qrreader.domain.ValidateUrlUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ScannedListUiState(
    val results: List<ScannedResult> = listOf(),
    val isLoading: Boolean = false
) {
    data class ScannedResult(
        val id: String = "",
        val text: String = "",
        val isUrl: Boolean = false,
        val date: DateFormat = DateFormat.Today
    )
}

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

    private fun toUiState(result: ScannedResult): ScannedListUiState.ScannedResult {
        return ScannedListUiState.ScannedResult(
            id = result.id,
            text = result.text,
            isUrl = validateUrlUseCase(result.text),
            date = formatDateUseCase(result.scannedDate)
        )
    }

    fun remove(result: ScannedListUiState.ScannedResult) {
        viewModelScope.launch {
            repository.markAsDelete(result.id)
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
