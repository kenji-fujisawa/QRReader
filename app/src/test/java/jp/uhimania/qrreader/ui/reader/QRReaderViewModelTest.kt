package jp.uhimania.qrreader.ui.reader

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import jp.uhimania.qrreader.data.ScannedResult
import jp.uhimania.qrreader.data.ScannedResultRepository
import jp.uhimania.qrreader.domain.DefaultGetPagePreviewUseCase
import jp.uhimania.qrreader.domain.GetPagePreviewUseCase
import jp.uhimania.qrreader.domain.PagePreview
import jp.uhimania.qrreader.domain.ValidateUrlUseCase
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
        val validateUrlUseCase = ValidateUrlUseCase()
        val getPagePreviewUseCase = DefaultGetPagePreviewUseCase()
        val viewModel = QRReaderViewModel(repository, validateUrlUseCase, getPagePreviewUseCase)
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
        val validateUrlUseCase = ValidateUrlUseCase()
        val getPagePreviewUseCase = FakeGetPagePreviewUseCase()
        val viewModel = QRReaderViewModel(repository, validateUrlUseCase, getPagePreviewUseCase)

        viewModel.saveResult()
        assertNull(repository.savedResult)

        var text = "aaa"
        viewModel.updateDecodedText(text)
        viewModel.saveResult()
        assertEquals(text, repository.savedResult?.text)
        assertEquals("", repository.savedResult?.title)
        assertEquals("", repository.savedResult?.description)
        assertEquals("", repository.savedResult?.image)

        text = "https://google.com"
        viewModel.updateDecodedText(text)
        viewModel.saveResult()
        assertEquals(text, repository.savedResult?.text)
        assertEquals(getPagePreviewUseCase(text).title, repository.savedResult?.title)
        assertEquals(getPagePreviewUseCase(text).description, repository.savedResult?.description)
        assertEquals(getPagePreviewUseCase(text).image, repository.savedResult?.image)
    }

    class FakeScannedResultRepository : ScannedResultRepository {
        override fun getResultsStream(): Flow<List<ScannedResult>> { return flowOf() }
        override fun getDeletedResultsStream(): Flow<List<ScannedResult>> { return flowOf() }

        var savedResult: ScannedResult? = null
        override suspend fun saveResult(result: ScannedResult) {
            savedResult = result
        }

        override suspend fun markAsDelete(id: String) {}
        override suspend fun unmarkAsDelete(id: String) {}
        override suspend fun forceDelete(id: String) {}
        override suspend fun purgeExpired() {}
        override suspend fun updateTitle(id: String, title: String) {}
    }

    class FakeGetPagePreviewUseCase : GetPagePreviewUseCase {
        override suspend operator fun invoke(url: String): PagePreview {
            return PagePreview(
                title = "title",
                description = "description",
                image = "image"
            )
        }
    }
}