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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    private val repository: ScannedResultRepository
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
            date = toDateFormat(result.deletedDate ?: Date())
        )
    }

    private fun toDateFormat(date: Date): DateFormat {
        val now = Date()
        if (date.year() == now.year() && date.month() == now.month() && date.day() == now.day()) {
            return DateFormat.Today
        } else if (date.year() == now.year() && date.month() == now.month()) {
            return DateFormat.DaysAgo(now.day() - date.day())
        } else if (date.year() == now.year()) {
            return DateFormat.MonthsAgo(now.month() - date.month())
        } else {
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            return DateFormat.Date(formatter.format(date))
        }
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
                TrashBoxViewModel(repository)
            }
        }
    }
}
