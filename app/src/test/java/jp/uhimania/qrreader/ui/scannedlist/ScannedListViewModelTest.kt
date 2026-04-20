package jp.uhimania.qrreader.ui.scannedlist

import jp.uhimania.qrreader.data.QueryHistoryRepository
import jp.uhimania.qrreader.data.ScannedResult
import jp.uhimania.qrreader.data.ScannedResultRepository
import jp.uhimania.qrreader.domain.DateFormat
import jp.uhimania.qrreader.domain.FormatDateUseCase
import jp.uhimania.qrreader.domain.ValidateUrlUseCase
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

class ScannedListViewModelTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testUiState() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        val resultRepository = FakeScannedResultRepository()
        val queryRepository = FakeQueryHistoryRepository()
        val formatDateUseCase = FormatDateUseCase()
        val validateUrlUseCase = ValidateUrlUseCase()
        val viewModel = ScannedListViewModel(resultRepository, queryRepository, formatDateUseCase, validateUrlUseCase)

        backgroundScope.launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect {}
        }

        assertTrue(viewModel.uiState.value.results.isEmpty())
        assertTrue(viewModel.uiState.value.isLoading)

        val results = listOf(
            ScannedResult(text = "aaa"),
            ScannedResult(text = "https://google.com/", scannedDate = Date(Date().time - 24 * 60 * 60 * 1000))
        )
        val history = listOf(
            "bbb",
            "ccc"
        )
        resultRepository.flow.emit(results)
        queryRepository.flow.emit(history)
        assertEquals(results.count(), viewModel.uiState.value.results.count())

        assertEquals(results[0].text, viewModel.uiState.value.results[0].text)
        assertFalse(viewModel.uiState.value.results[0].isUrl)
        assertEquals(DateFormat.Today, viewModel.uiState.value.results[0].date)

        assertEquals(results[1].text, viewModel.uiState.value.results[1].text)
        assertTrue(viewModel.uiState.value.results[1].isUrl)
        assertEquals(DateFormat.DaysAgo(1), viewModel.uiState.value.results[1].date)

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(ScannedListScreenState.Normal, viewModel.uiState.value.state)
        assertTrue(viewModel.uiState.value.query.isEmpty())

        assertEquals(history[1], viewModel.uiState.value.queryHistory[0])
        assertEquals(history[0], viewModel.uiState.value.queryHistory[1])
    }

    class FakeScannedResultRepository : ScannedResultRepository {
        val flow = MutableSharedFlow<List<ScannedResult>>()
        override fun getResultsStream(): Flow<List<ScannedResult>> {
            return flow
        }
        override fun getDeletedResultsStream(): Flow<List<ScannedResult>> { return flowOf() }
        override suspend fun saveResult(result: ScannedResult) {}
        override suspend fun markAsDelete(id: String) {}
        override suspend fun unmarkAsDelete(id: String) {}
        override suspend fun forceDelete(id: String) {}
        override suspend fun purgeExpired() {}
        override suspend fun updateTitle(id: String, title: String) {}
    }

    class FakeQueryHistoryRepository: QueryHistoryRepository {
        val flow = MutableSharedFlow<List<String>>()
        override fun getQueryHistoryStream(): Flow<List<String>> {
            return flow
        }
        override suspend fun addQuery(query: String) {}
    }
}
