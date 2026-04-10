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
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed class DateFormat {
    object Today : DateFormat()
    data class DaysAgo(val day: Int) : DateFormat()
    data class MonthsAgo(val month: Int) : DateFormat()
    data class Date(val date: String) : DateFormat()
}

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
    private val repository: ScannedResultRepository
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
            isUrl = isUrl(result.text),
            date = toDateFormat(result.scannedDate)
        )
    }

    private fun isUrl(text: String?): Boolean {
        return try {
            URL(text)
            true
        } catch (_: Exception) {
            false
        }
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
                ScannedListViewModel(repository)
            }
        }
    }
}

fun Date.year(): Int {
    val calendar = Calendar.getInstance()
    calendar.time = this
    return calendar.get(Calendar.YEAR)
}

fun Date.month(): Int {
    val calendar = Calendar.getInstance()
    calendar.time = this
    return calendar.get(Calendar.MONTH) + 1
}

fun Date.day(): Int {
    val calendar = Calendar.getInstance()
    calendar.time = this
    return calendar.get(Calendar.DAY_OF_MONTH)
}
