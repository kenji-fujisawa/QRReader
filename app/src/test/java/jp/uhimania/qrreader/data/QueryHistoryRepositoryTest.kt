package jp.uhimania.qrreader.data

import jp.uhimania.qrreader.data.source.LocalDataSource
import jp.uhimania.qrreader.data.source.LocalQueryHistory
import jp.uhimania.qrreader.data.source.LocalScannedResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
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

class QueryHistoryRepositoryTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testGetQueryHistoryStream() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        val source = FakeLocalDataSource()
        val repository = DefaultQueryHistoryRepository(source, Dispatchers.Main)

        val values = mutableListOf<List<String>>()
        backgroundScope.launch(UnconfinedTestDispatcher()) {
            repository.getQueryHistoryStream().toList(values)
        }

        val history1 = listOf(
            LocalQueryHistory(
                query = "aaa",
                date = Date()
            )
        )
        source.flow.emit(history1)
        assertEquals(1, values.count())
        assertEquals(1, values[0].count())
        assertEquals(history1[0].query, values[0][0])

        val history2 = listOf(
            LocalQueryHistory(
                query = "bbb",
                date = Date()
            ),
            LocalQueryHistory(
                query = "ccc",
                date = Date()
            )
        )
        source.flow.emit(history2)
        assertEquals(2, values.count())
        assertEquals(1, values[0].count())
        assertEquals(history1[0].query, values[0][0])
        assertEquals(2, values[1].count())
        assertEquals(history2[0].query, values[1][0])
        assertEquals(history2[1].query, values[1][1])
    }

    @Test
    fun testAddQuery() = runTest {
        val source = FakeLocalDataSource()
        val repository = DefaultQueryHistoryRepository(source)

        repository.addQuery("")
        assertEquals(0, source.history.count())

        repository.addQuery("aaa")
        assertEquals(1, source.history.count())
        assertEquals("aaa", source.history[0].query)

        repository.addQuery("bbb")
        repository.addQuery("ccc")
        repository.addQuery("ddd")
        repository.addQuery("eee")
        assertEquals(5, source.history.count())
        assertEquals("aaa", source.history[0].query)
        assertEquals("bbb", source.history[1].query)
        assertEquals("ccc", source.history[2].query)
        assertEquals("ddd", source.history[3].query)
        assertEquals("eee", source.history[4].query)

        repository.addQuery("fff")
        assertEquals(5, source.history.count())
        assertEquals("bbb", source.history[0].query)
        assertEquals("ccc", source.history[1].query)
        assertEquals("ddd", source.history[2].query)
        assertEquals("eee", source.history[3].query)
        assertEquals("fff", source.history[4].query)

        repository.addQuery("ccc")
        assertEquals(5, source.history.count())
        assertEquals("bbb", source.history[0].query)
        assertEquals("ddd", source.history[1].query)
        assertEquals("eee", source.history[2].query)
        assertEquals("fff", source.history[3].query)
        assertEquals("ccc", source.history[4].query)
    }

    class FakeLocalDataSource: LocalDataSource {
        override fun observeResults(): Flow<List<LocalScannedResult>> { return flowOf() }
        override suspend fun getResults(): List<LocalScannedResult> { return listOf() }
        override suspend fun getResult(id: String): LocalScannedResult? { return null }
        override suspend fun insert(result: LocalScannedResult) {}
        override suspend fun update(result: LocalScannedResult) {}
        override suspend fun upsert(result: LocalScannedResult) {}
        override suspend fun delete(result: LocalScannedResult) {}

        val flow = MutableSharedFlow<List<LocalQueryHistory>>()
        override fun observeQueryHistory(): Flow<List<LocalQueryHistory>> {
            return flow
        }

        val history: MutableList<LocalQueryHistory> = mutableListOf()
        override suspend fun getQueryHistory(): List<LocalQueryHistory> {
            return history
        }

        override suspend fun insert(query: LocalQueryHistory) {
            history.add(query)
        }

        override suspend fun delete(query: LocalQueryHistory) {
            history.remove(query)
        }
    }
}
