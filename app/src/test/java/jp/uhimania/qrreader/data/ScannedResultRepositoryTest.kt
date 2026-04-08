package jp.uhimania.qrreader.data

import jp.uhimania.qrreader.data.source.LocalDataSource
import jp.uhimania.qrreader.data.source.LocalScannedResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class ScannedResultRepositoryTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testGetResultsStream() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        val source = FakeLocalDataSource()
        val repository = DefaultScannedResultRepository(source, Dispatchers.Main)

        val values = mutableListOf<List<ScannedResult>>()
        backgroundScope.launch(UnconfinedTestDispatcher()) {
            repository.getResultsStream().toList(values)
        }

        val records1 = listOf(
            LocalScannedResult(
                id = "1",
                text = "aaa",
                date = Date()
            )
        )
        source.flow.emit(records1)
        assertEquals(1, values.count())
        assertEquals(1, values[0].count())
        assertEquals(records1[0].id, values[0][0].id)
        assertEquals(records1[0].text, values[0][0].text)
        assertEquals(records1[0].date, values[0][0].date)

        val records2 = listOf(
            LocalScannedResult(
                id = "2",
                text = "bbb",
                date = Date()
            ),
            LocalScannedResult(
                id = "3",
                text = "ccc",
                date = Date()
            )
        )
        source.flow.emit(records2)
        assertEquals(2, values.count())
        assertEquals(1, values[0].count())
        assertEquals(records1[0].id, values[0][0].id)
        assertEquals(records1[0].text, values[0][0].text)
        assertEquals(records1[0].date, values[0][0].date)
        assertEquals(2, values[1].count())
        assertEquals(records2[0].id, values[1][0].id)
        assertEquals(records2[0].text, values[1][0].text)
        assertEquals(records2[0].date, values[1][0].date)
        assertEquals(records2[1].id, values[1][1].id)
        assertEquals(records2[1].text, values[1][1].text)
        assertEquals(records2[1].date, values[1][1].date)
    }

    @Test
    fun testSaveResult() = runTest {
        val source = FakeLocalDataSource()
        val repository = DefaultScannedResultRepository(source)

        val result = ScannedResult(
            id = "1",
            text = "aaa",
            date = Date()
        )
        repository.saveResult(result)
        assertEquals(result.id, source.upserted?.id)
        assertEquals(result.text, source.upserted?.text)
        assertEquals(result.date, source.upserted?.date)

        repository.saveResult(result.copy(text = "bbb"))
        assertEquals(result.id, source.upserted?.id)
        assertEquals("bbb", source.upserted?.text)
        assertEquals(result.date, source.upserted?.date)
    }

    @Test
    fun testRemoveResult() = runTest {
        val source = FakeLocalDataSource()
        val repository = DefaultScannedResultRepository(source)

        val result = ScannedResult(id = "1")
        repository.removeResult(result)
        assertEquals(result.id, source.deleted?.id)
    }

    class FakeLocalDataSource : LocalDataSource {
        val flow = MutableSharedFlow<List<LocalScannedResult>>()
        override fun observeResults(): Flow<List<LocalScannedResult>> {
            return flow
        }
        override suspend fun getResults(): List<LocalScannedResult> { return listOf() }
        override suspend fun insert(result: LocalScannedResult) {}
        override suspend fun update(result: LocalScannedResult) {}

        var upserted: LocalScannedResult? = null
        override suspend fun upsert(result: LocalScannedResult) {
            upserted = result
        }

        var deleted: LocalScannedResult? = null
        override suspend fun delete(result: LocalScannedResult) {
            deleted = result
        }
    }
}
