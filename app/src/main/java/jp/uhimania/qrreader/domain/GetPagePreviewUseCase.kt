package jp.uhimania.qrreader.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

data class PagePreview(
    val title: String = "",
    val description: String = "",
    val image: String = ""
)

interface GetPagePreviewUseCase {
    suspend operator fun invoke(url: String): PagePreview
}

class DefaultGetPagePreviewUseCase(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : GetPagePreviewUseCase {
    override suspend operator fun invoke(url: String): PagePreview {
        return withContext(dispatcher) {
            val doc = Jsoup.connect(url).get()
            val title = doc.select("meta[property=og:title]").attr("content").ifEmpty { doc.title() }
            val description = doc.select("meta[property=og:description]").attr("content")
            val image = doc.select("meta[property=og:image]").attr("content")
            PagePreview(title, description, image)
        }
    }
}
