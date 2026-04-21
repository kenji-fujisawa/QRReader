package jp.uhimania.qrreader.data

import jp.uhimania.qrreader.data.source.LocalDataSource
import jp.uhimania.qrreader.data.source.LocalScannedResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.Date

interface ScannedResultRepository {
    fun getResultsStream(): Flow<List<ScannedResult>>
    fun getDeletedResultsStream(): Flow<List<ScannedResult>>
    suspend fun saveResult(result: ScannedResult)
    suspend fun markAsDelete(id: String)
    suspend fun unmarkAsDelete(id: String)
    suspend fun forceDelete(id: String)
    suspend fun purgeExpired()
    suspend fun updateTitle(id: String, title: String)
}

class DefaultScannedResultRepository(
    private val source: LocalDataSource,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ScannedResultRepository {
    override fun getResultsStream(): Flow<List<ScannedResult>> {
        return source.observeResults()
            .map {
                it.filter { item -> item.deletedDate == null }
                    .map { item -> item.asResult() }
            }
            .flowOn(dispatcher)
    }

    override fun getDeletedResultsStream(): Flow<List<ScannedResult>> {
        return source.observeResults()
            .map {
                it.filter { item -> item.deletedDate != null }
                    .map { item -> item.asResult() }
            }
            .flowOn(dispatcher)
    }

    override suspend fun saveResult(result: ScannedResult) {
        source.upsert(result.asLocal())
    }

    override suspend fun markAsDelete(id: String) {
        source.getResult(id)?.let {
            val deleted = it.copy(deletedDate = Date())
            source.update(deleted)
        }
    }

    override suspend fun unmarkAsDelete(id: String) {
        source.getResult(id)?.let {
            val restored = it.copy(deletedDate = null)
            source.update(restored)
        }
    }

    override suspend fun forceDelete(id: String) {
        source.getResult(id)?.let {
            source.delete(it)
        }
    }

    override suspend fun purgeExpired() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -30)
        source.getResults()
            .filter { it.deletedDate != null }
            .filter { it.deletedDate!! < calendar.time }
            .forEach { source.delete(it) }
    }

    override suspend fun updateTitle(id: String, title: String) {
        source.getResult(id)?.let {
            val result = it.copy(title = title)
            source.update(result)
        }
    }
}

fun ScannedResult.asLocal(): LocalScannedResult {
    return LocalScannedResult(
        id = this.id,
        text = this.text,
        title = this.title,
        description = this.description,
        image = this.image,
        scannedDate = this.scannedDate,
        deletedDate = this.deletedDate
    )
}

fun LocalScannedResult.asResult(): ScannedResult {
    return ScannedResult(
        id = this.id,
        text = this.text,
        title = this.title,
        description = this.description,
        image = this.image,
        scannedDate = this.scannedDate,
        deletedDate = this.deletedDate
    )
}
