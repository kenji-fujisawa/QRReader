package jp.uhimania.qrreader.ui.scannedlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import jp.uhimania.qrreader.QRReaderApplication
import jp.uhimania.qrreader.data.DefaultQueryHistoryRepository
import jp.uhimania.qrreader.data.DefaultScannedResultRepository
import jp.uhimania.qrreader.data.QueryHistoryRepository
import jp.uhimania.qrreader.data.ScannedResult
import jp.uhimania.qrreader.data.ScannedResultRepository
import jp.uhimania.qrreader.domain.FormatDateUseCase
import jp.uhimania.qrreader.domain.ValidateUrlUseCase
import jp.uhimania.qrreader.ui.common.ScannedResultUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ScannedListScreenState {
    Normal,
    RemoveMode,
    SearchMode
}

data class ScannedListUiState(
    val results: List<ScannedResultUiState> = listOf(),
    val isLoading: Boolean = false,
    val state: ScannedListScreenState = ScannedListScreenState.Normal,
    val query: String = "",
    val queryHistory: List<String> = listOf()
)

class ScannedListViewModel(
    private val resultRepository: ScannedResultRepository,
    private val queryRepository: QueryHistoryRepository,
    private val formatDateUseCase: FormatDateUseCase,
    private val validateUrlUseCase: ValidateUrlUseCase
) : ViewModel() {
    private val _results = resultRepository.getResultsStream()
        .map { results -> results.sortedByDescending { it.scannedDate } }

    private val _state = MutableStateFlow(ScannedListScreenState.Normal)
    private val _selected = MutableStateFlow<Set<String>>(setOf())
    private val _query = MutableStateFlow("")
    private val _queryHistory = queryRepository.getQueryHistoryStream()

    val uiState: StateFlow<ScannedListUiState> =
        combine(_results, _state, _selected, _query, _queryHistory) { results, state, _, query, history ->
            ScannedListUiState(
                results = results
                    .filter { it.title.contains(query) || it.text.contains(query) }
                    .map { toUiState(it) },
                isLoading = false,
                state = state,
                query = query,
                queryHistory = history.reversed()
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = ScannedListUiState(isLoading = true)
            )

    init {
        viewModelScope.launch {
            resultRepository.purgeExpired()
        }
    }

    private fun toUiState(result: ScannedResult): ScannedResultUiState {
        return ScannedResultUiState(
            id = result.id,
            text = result.text,
            title = result.title,
            isUrl = validateUrlUseCase(result.text),
            date = formatDateUseCase(result.scannedDate),
            selected = _selected.value.contains(result.id)
        )
    }

    fun remove(id: String) {
        viewModelScope.launch {
            resultRepository.markAsDelete(id)
        }
    }

    fun updateTitle(id: String, title: String) {
        viewModelScope.launch {
            resultRepository.updateTitle(id, title)
        }
    }

    fun setScreenState(state: ScannedListScreenState) {
        _state.update { state }
    }

    fun select(id: String) {
        _selected.update { it + id }
    }

    fun unselect(id: String) {
        _selected.update { it - id }
    }

    fun removeSelected() {
        viewModelScope.launch {
            _selected.value.forEach {
                resultRepository.markAsDelete(it)
            }
            clearSelection()
        }
    }

    fun clearSelection() {
        _selected.update { setOf() }
    }

    fun updateQuery(query: String) {
        _query.update { query }

        if (!query.isEmpty()) {
            viewModelScope.launch {
                queryRepository.addQuery(query)
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as QRReaderApplication
                val resultRepository = DefaultScannedResultRepository(app.source)
                val queryRepository = DefaultQueryHistoryRepository(app.source)
                val formatDateUseCase = FormatDateUseCase()
                val validateUrlUseCase = ValidateUrlUseCase()
                ScannedListViewModel(resultRepository, queryRepository, formatDateUseCase, validateUrlUseCase)
            }
        }
    }
}
