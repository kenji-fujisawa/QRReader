package jp.uhimania.qrreader.data

import jp.uhimania.qrreader.data.source.LocalDataSource
import jp.uhimania.qrreader.data.source.LocalScannedResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

interface ScannedResultRepository {
    fun getResultsStream(): Flow<List<ScannedResult>>
    suspend fun saveResult(result: ScannedResult)
}

class DefaultScannedResultRepository(
    private val source: LocalDataSource,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ScannedResultRepository {
    override fun getResultsStream(): Flow<List<ScannedResult>> {
        return source.observeResults()
            .map { it.map { item -> item.asResult() } }
            .flowOn(dispatcher)
    }

    override suspend fun saveResult(result: ScannedResult) {
        source.upsert(result.asLocal())
    }
}

fun ScannedResult.asLocal(): LocalScannedResult {
    return LocalScannedResult(
        id = this.id,
        text = this.text,
        date = this.date
    )
}

fun LocalScannedResult.asResult(): ScannedResult {
    return ScannedResult(
        id = this.id,
        text = this.text,
        date = this.date
    )
}
