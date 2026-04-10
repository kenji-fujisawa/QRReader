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
import jp.uhimania.qrreader.domain.DateFormat
import jp.uhimania.qrreader.domain.FormatDateUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date

data class TrashBoxUiState(
    val results: List<ScannedResult> = listOf(),
    val isLoading: Boolean = false
) {
    data class ScannedResult(
        val id: String = "",
        val text: String = "",
        val date: DateFormat = DateFormat.Today
    )
}

class TrashBoxViewModel(
    private val repository: ScannedResultRepository,
    private val formatDateUseCase: FormatDateUseCase
): ViewModel() {
    val uiState: StateFlow<TrashBoxUiState> =
        repository.getDeletedResultsStream()
            .map { results ->
                TrashBoxUiState(
                    results = results
                        .sortedByDescending { it.deletedDate }
                        .map { toUiState(it) },
                    isLoading = false
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = TrashBoxUiState(isLoading = true)
            )

    private fun toUiState(result: ScannedResult): TrashBoxUiState.ScannedResult {
        return TrashBoxUiState.ScannedResult(
            id = result.id,
            text = result.text,
            date = formatDateUseCase(result.deletedDate ?: Date())
        )
    }

    fun restore(result: TrashBoxUiState.ScannedResult) {
        viewModelScope.launch {
            repository.unmarkAsDelete(result.id)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as QRReaderApplication
                val repository = DefaultScannedResultRepository(app.source)
                val useCase = FormatDateUseCase()
                TrashBoxViewModel(repository, useCase)
            }
        }
    }
}
