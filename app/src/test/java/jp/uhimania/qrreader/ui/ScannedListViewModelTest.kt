package jp.uhimania.qrreader.ui

import jp.uhimania.qrreader.data.ScannedResult
import jp.uhimania.qrreader.data.ScannedResultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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

        val repository = FakeScannedResultRepository()
        val viewModel = ScannedListViewModel(repository)

        backgroundScope.launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect {}
        }

        assertTrue(viewModel.uiState.value.results.isEmpty())
        assertTrue(viewModel.uiState.value.isLoading)

        val results = listOf(
            ScannedResult(text = "aaa"),
            ScannedResult(text = "https://google.com/", date = Date(Date().time - 24 * 60 * 60 * 1000))
        )
        repository.flow.emit(results)
        assertEquals(results.count(), viewModel.uiState.value.results.count())

        assertEquals(results[0].text, viewModel.uiState.value.results[0].text)
        assertFalse(viewModel.uiState.value.results[0].isUrl)
        assertEquals(DateFormat.Today, viewModel.uiState.value.results[0].date)

        assertEquals(results[1].text, viewModel.uiState.value.results[1].text)
        assertTrue(viewModel.uiState.value.results[1].isUrl)
        assertEquals(DateFormat.DaysAgo(1), viewModel.uiState.value.results[1].date)

        assertFalse(viewModel.uiState.value.isLoading)
    }

    class FakeScannedResultRepository : ScannedResultRepository {
        val flow = MutableSharedFlow<List<ScannedResult>>()
        override fun getResultsStream(): Flow<List<ScannedResult>> {
            return flow
        }
        override suspend fun saveResult(result: ScannedResult) {}
        override suspend fun removeResult(result: ScannedResult) {}
    }
}
