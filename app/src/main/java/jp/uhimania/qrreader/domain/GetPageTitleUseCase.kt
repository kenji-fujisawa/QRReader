package jp.uhimania.qrreader.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

interface GetPageTitleUseCase {
    suspend operator fun invoke(url: String): String
}

class DefaultGetPageTitleUseCase(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : GetPageTitleUseCase {
    override suspend operator fun invoke(url: String): String {
        return withContext(dispatcher) {
            val doc = Jsoup.connect(url).get()
            doc.title()
        }
    }
}
