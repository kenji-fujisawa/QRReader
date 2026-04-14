package jp.uhimania.qrreader.ui.trashbox

import jp.uhimania.qrreader.data.ScannedResult
import jp.uhimania.qrreader.data.ScannedResultRepository
import jp.uhimania.qrreader.domain.DateFormat
import jp.uhimania.qrreader.domain.FormatDateUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class TrashBoxViewModelTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testUiState() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        val repository = FakeScannedResultRepository()
        val useCase = FormatDateUseCase()
        val viewModel = TrashBoxViewModel(repository, useCase)

        backgroundScope.launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect {}
        }

        assertTrue(viewModel.uiState.value.results.isEmpty())
        assertTrue(viewModel.uiState.value.isLoading)

        val results = listOf(
            ScannedResult(text = "aaa", deletedDate = Date()),
            ScannedResult(text = "https://google.com/", deletedDate = Date(Date().time - 24 * 60 * 60 * 1000))
        )
        repository.flow.emit(results)
        assertEquals(results.count(), viewModel.uiState.value.results.count())

        assertEquals(results[0].text, viewModel.uiState.value.results[0].text)
        assertEquals(DateFormat.Today, viewModel.uiState.value.results[0].date)

        assertEquals(results[1].text, viewModel.uiState.value.results[1].text)
        assertEquals(DateFormat.DaysAgo(1), viewModel.uiState.value.results[1].date)

        assertFalse(viewModel.uiState.value.isLoading)
    }

    class FakeScannedResultRepository : ScannedResultRepository {
        val flow = MutableSharedFlow<List<ScannedResult>>()
        override fun getResultsStream(): Flow<List<ScannedResult>> { return flowOf() }
        override fun getDeletedResultsStream(): Flow<List<ScannedResult>> {
            return flow
        }
        override suspend fun saveResult(result: ScannedResult) {}
        override suspend fun markAsDelete(id: String) {}
        override suspend fun unmarkAsDelete(id: String) {}
        override suspend fun forceDelete(id: String) {}
        override suspend fun purgeExpired() {}
        override suspend fun updateTitle(id: String, title: String) {}
    }
}