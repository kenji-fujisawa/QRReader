package jp.uhimania.qrreader.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import jp.uhimania.qrreader.data.ScannedResult
import jp.uhimania.qrreader.data.ScannedResultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QRReaderViewModelTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testUiState() {
        val repository = FakeScannedResultRepository()
        val viewModel = QRReaderViewModel(repository)
        assertNull(viewModel.uiState.value.decodedText)
        assertFalse(viewModel.uiState.value.isUrl)
        assertNull(viewModel.uiState.value.barCodeRect)
        assertEquals(Size.Zero, viewModel.uiState.value.imageSize)

        var text = "aaa"
        viewModel.updateDecodedText(text)
        assertEquals(text, viewModel.uiState.value.decodedText)
        assertFalse(viewModel.uiState.value.isUrl)
        assertNull(viewModel.uiState.value.barCodeRect)
        assertEquals(Size.Zero, viewModel.uiState.value.imageSize)

        text = "https://google.com/"
        viewModel.updateDecodedText(text)
        assertEquals(text, viewModel.uiState.value.decodedText)
        assertTrue(viewModel.uiState.value.isUrl)
        assertNull(viewModel.uiState.value.barCodeRect)
        assertEquals(Size.Zero, viewModel.uiState.value.imageSize)

        val rect = Rect(10f, 20f, 30f, 40f)
        viewModel.updateBarcodeRect(rect)
        assertEquals(text, viewModel.uiState.value.decodedText)
        assertTrue(viewModel.uiState.value.isUrl)
        assertEquals(rect, viewModel.uiState.value.barCodeRect)
        assertEquals(Size.Zero, viewModel.uiState.value.imageSize)

        val size = Size(100f, 200f)
        viewModel.updateImageSize(size)
        assertEquals(text, viewModel.uiState.value.decodedText)
        assertTrue(viewModel.uiState.value.isUrl)
        assertEquals(rect, viewModel.uiState.value.barCodeRect)
        assertEquals(size, viewModel.uiState.value.imageSize)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testSaveResult() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        val repository = FakeScannedResultRepository()
        val viewModel = QRReaderViewModel(repository)

        viewModel.saveResult()
        assertNull(repository.savedResult)

        val text = "aaa"
        viewModel.updateDecodedText(text)
        viewModel.saveResult()
        assertEquals(text, repository.savedResult?.text)
    }

    class FakeScannedResultRepository : ScannedResultRepository {
        override fun getResultsStream(): Flow<List<ScannedResult>> { return flowOf() }

        var savedResult: ScannedResult? = null
        override suspend fun saveResult(result: ScannedResult) {
            savedResult = result
        }

        override suspend fun markAsDelete(id: String) {}
    }
}