package jp.uhimania.qrreader.ui.trashbox

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date

enum class TrashBoxScreenState {
    Normal,
    RestoreMode,
    ForceRemoveMode
}

data class TrashBoxUiState(
    val results: List<ScannedResultUiState> = listOf(),
    val isLoading: Boolean = false,
    val state: TrashBoxScreenState = TrashBoxScreenState.Normal
)

class TrashBoxViewModel(
    private val repository: ScannedResultRepository,
    private val formatDateUseCase: FormatDateUseCase,
    private val validateUrlUseCase: ValidateUrlUseCase
): ViewModel() {
    private val _results = repository.getDeletedResultsStream()
        .map { results -> results.sortedByDescending { it.deletedDate } }

    private val _state = MutableStateFlow(TrashBoxScreenState.Normal)
    private val _selected = MutableStateFlow<Set<String>>(setOf())

    val uiState: StateFlow<TrashBoxUiState> =
        combine(_results, _state, _selected) { results, state, _ ->
            TrashBoxUiState(
                results = results.map { toUiState(it) },
                isLoading = false,
                state = state
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = TrashBoxUiState(isLoading = true)
            )

    private fun toUiState(result: ScannedResult): ScannedResultUiState {
        return ScannedResultUiState(
            id = result.id,
            text = result.text,
            title = result.title,
            isUrl = validateUrlUseCase(result.text),
            date = formatDateUseCase(result.deletedDate ?: Date()),
            selected = _selected.value.contains(result.id)
        )
    }

    fun restore(id: String) {
        viewModelScope.launch {
            repository.unmarkAsDelete(id)
        }
    }

    fun forceRemove(id: String) {
        viewModelScope.launch {
            repository.forceDelete(id)
        }
    }

    fun setScreenState(state: TrashBoxScreenState) {
        _state.update { state }
    }

    fun select(id: String) {
        _selected.update { it + id }
    }

    fun unselect(id: String) {
        _selected.update { it - id }
    }

    fun restoreSelected() {
        viewModelScope.launch {
            _selected.value.forEach {
                repository.unmarkAsDelete(it)
            }
            clearSelection()
        }
    }

    fun forceRemoveSelected() {
        viewModelScope.launch {
            _selected.value.forEach {
                repository.forceDelete(it)
            }
            clearSelection()
        }
    }

    fun clearSelection() {
        _selected.update { setOf() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as QRReaderApplication
                val repository = DefaultScannedResultRepository(app.source)
                val formatDateUseCase = FormatDateUseCase()
                val validateUrlUseCase = ValidateUrlUseCase()
                TrashBoxViewModel(repository, formatDateUseCase, validateUrlUseCase)
            }
        }
    }
}
