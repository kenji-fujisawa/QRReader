package jp.uhimania.qrreader.ui

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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ScannedListUiState(
    val results: List<ScannedResult> = listOf()
)

class ScannedListViewModel(
    repository: ScannedResultRepository
) : ViewModel() {
    val uiState: StateFlow<ScannedListUiState> =
        repository.getResultsStream()
            .map { ScannedListUiState(results = it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = ScannedListUiState()
            )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as QRReaderApplication
                val repository = DefaultScannedResultRepository(app.source)
                ScannedListViewModel(repository)
            }
        }
    }
}
