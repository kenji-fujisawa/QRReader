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
        assertEquals(0, viewModel.uiState.value.codes.count())
        assertEquals(0, viewModel.uiState.value.bounds.count())
        assertEquals(Size.Zero, viewModel.uiState.value.imageSize)

        var texts = listOf("aaa", "bbb")
        viewModel.updateCodeTexts(texts)
        assertEquals(2, viewModel.uiState.value.codes.count())
        assertEquals(texts[0], viewModel.uiState.value.codes[0].text)
        assertEquals(texts[1], viewModel.uiState.value.codes[1].text)
        assertFalse(viewModel.uiState.value.codes[0].isUrl)
        assertFalse(viewModel.uiState.value.codes[1].isUrl)
        assertEquals(0, viewModel.uiState.value.bounds.count())
        assertEquals(Size.Zero, viewModel.uiState.value.imageSize)

        texts = listOf("https://google.com/")
        viewModel.updateCodeTexts(texts)
        assertEquals(1, viewModel.uiState.value.codes.count())
        assertEquals(texts[0], viewModel.uiState.value.codes[0].text)
        assertTrue(viewModel.uiState.value.codes[0].isUrl)
        assertEquals(0, viewModel.uiState.value.bounds.count())
        assertEquals(Size.Zero, viewModel.uiState.value.imageSize)

        val bounds = listOf(Rect(10f, 20f, 30f, 40f))
        viewModel.updateCodeBounds(bounds)
        assertEquals(1, viewModel.uiState.value.codes.count())
        assertEquals(texts[0], viewModel.uiState.value.codes[0].text)
        assertTrue(viewModel.uiState.value.codes[0].isUrl)
        assertEquals(1, viewModel.uiState.value.bounds.count())
        assertEquals(bounds[0], viewModel.uiState.value.bounds[0])
        assertEquals(Size.Zero, viewModel.uiState.value.imageSize)

        val size = Size(100f, 200f)
        viewModel.updateImageSize(size)
        assertEquals(1, viewModel.uiState.value.codes.count())
        assertEquals(texts[0], viewModel.uiState.value.codes[0].text)
        assertTrue(viewModel.uiState.value.codes[0].isUrl)
        assertEquals(1, viewModel.uiState.value.bounds.count())
        assertEquals(bounds[0], viewModel.uiState.value.bounds[0])
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
        assertEquals(0, repository.savedResults.count())

        var texts = listOf("aaa", "bbb")
        viewModel.updateCodeTexts(texts)
        viewModel.saveResult()
        assertEquals(2, repository.savedResults.count())
        assertEquals(texts[0], repository.savedResults[0].text)
        assertEquals("", repository.savedResults[0].title)
        assertEquals("", repository.savedResults[0].description)
        assertEquals("", repository.savedResults[0].image)
        assertEquals(texts[1], repository.savedResults[1].text)
        assertEquals("", repository.savedResults[1].title)
        assertEquals("", repository.savedResults[1].description)
        assertEquals("", repository.savedResults[1].image)

        repository.savedResults.clear()

        texts = listOf("https://google.com")
        viewModel.updateCodeTexts(texts)
        viewModel.saveResult()
        assertEquals(1, repository.savedResults.count())
        assertEquals(texts[0], repository.savedResults[0].text)
        assertEquals(getPagePreviewUseCase(texts[0]).title, repository.savedResults[0].title)
        assertEquals(getPagePreviewUseCase(texts[0]).description, repository.savedResults[0].description)
        assertEquals(getPagePreviewUseCase(texts[0]).image, repository.savedResults[0].image)
    }

    class FakeScannedResultRepository : ScannedResultRepository {
        override fun getResultsStream(): Flow<List<ScannedResult>> { return flowOf() }
        override fun getDeletedResultsStream(): Flow<List<ScannedResult>> { return flowOf() }

        var savedResults: MutableList<ScannedResult> = mutableListOf()
        override suspend fun saveResult(result: ScannedResult) {
            savedResults.add(result)
        }

        override suspend fun markAsDelete(id: String) {}
        override suspend fun unmarkAsDelete(id: String) {}
        override suspend fun forceDelete(id: String) {}
        override suspend fun purgeExpired() {}
        override suspend fun updateTitle(id: String, title: String) {}
        override suspend fun updateDescription(id: String, description: String) {}
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