package jp.uhimania.qrreader.data

import jp.uhimania.qrreader.data.source.LocalDataSource
import jp.uhimania.qrreader.data.source.LocalQueryHistory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.Date

interface QueryHistoryRepository {
    fun getQueryHistoryStream(): Flow<List<String>>
    suspend fun addQuery(query: String)
}

class DefaultQueryHistoryRepository(
    private val source: LocalDataSource,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
): QueryHistoryRepository {
    override fun getQueryHistoryStream(): Flow<List<String>> {
        return source.observeQueryHistory()
            .map { it.map { item -> item.query } }
            .flowOn(dispatcher)
    }

    override suspend fun addQuery(query: String) {
        val history = source.getQueryHistory()
        if (query.isEmpty() || history.lastOrNull()?.query == query) {
            return
        }

        val historyToRemove = history.filter { it.query == query }
        historyToRemove.forEach { source.delete(it) }

        if (history.count() - historyToRemove.count() >= 5) {
            source.delete(history.first())
        }

        source.insert(LocalQueryHistory(query, Date()))
    }
}
