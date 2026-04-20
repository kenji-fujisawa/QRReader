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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
                title = "title1",
                scannedDate = Date(),
                deletedDate = null
            )
        )
        source.flow.emit(records1)
        assertEquals(1, values.count())
        assertEquals(1, values[0].count())
        assertEquals(records1[0].id, values[0][0].id)
        assertEquals(records1[0].text, values[0][0].text)
        assertEquals(records1[0].scannedDate, values[0][0].scannedDate)
        assertEquals(records1[0].deletedDate, values[0][0].deletedDate)

        val records2 = listOf(
            LocalScannedResult(
                id = "2",
                text = "bbb",
                title = "title2",
                scannedDate = Date(),
                deletedDate = null
            ),
            LocalScannedResult(
                id = "3",
                text = "ccc",
                title = "title3",
                scannedDate = Date(),
                deletedDate = Date()
            )
        )
        source.flow.emit(records2)
        assertEquals(2, values.count())
        assertEquals(1, values[0].count())
        assertEquals(records1[0].id, values[0][0].id)
        assertEquals(records1[0].text, values[0][0].text)
        assertEquals(records1[0].title, values[0][0].title)
        assertEquals(records1[0].scannedDate, values[0][0].scannedDate)
        assertEquals(records1[0].deletedDate, values[0][0].deletedDate)
        assertEquals(1, values[1].count())
        assertEquals(records2[0].id, values[1][0].id)
        assertEquals(records2[0].text, values[1][0].text)
        assertEquals(records2[0].title, values[1][0].title)
        assertEquals(records2[0].scannedDate, values[1][0].scannedDate)
        assertEquals(records2[0].deletedDate, values[1][0].deletedDate)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testGetDeletedResultsStream() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        val source = FakeLocalDataSource()
        val repository = DefaultScannedResultRepository(source, Dispatchers.Main)

        val values = mutableListOf<List<ScannedResult>>()
        backgroundScope.launch(UnconfinedTestDispatcher()) {
            repository.getDeletedResultsStream().toList(values)
        }

        val records1 = listOf(
            LocalScannedResult(
                id = "1",
                text = "aaa",
                title = "title1",
                scannedDate = Date(),
                deletedDate = null
            )
        )
        source.flow.emit(records1)
        assertEquals(1, values.count())
        assertEquals(0, values[0].count())

        val records2 = listOf(
            LocalScannedResult(
                id = "2",
                text = "bbb",
                title = "title2",
                scannedDate = Date(),
                deletedDate = null
            ),
            LocalScannedResult(
                id = "3",
                text = "ccc",
                title = "title3",
                scannedDate = Date(),
                deletedDate = Date()
            )
        )
        source.flow.emit(records2)
        assertEquals(2, values.count())
        assertEquals(0, values[0].count())
        assertEquals(1, values[1].count())
        assertEquals(records2[1].id, values[1][0].id)
        assertEquals(records2[1].text, values[1][0].text)
        assertEquals(records2[1].title, values[1][0].title)
        assertEquals(records2[1].scannedDate, values[1][0].scannedDate)
        assertEquals(records2[1].deletedDate, values[1][0].deletedDate)
    }

    @Test
    fun testSaveResult() = runTest {
        val source = FakeLocalDataSource()
        val repository = DefaultScannedResultRepository(source)

        val result = ScannedResult(
            id = "1",
            text = "aaa",
            title = "title1",
            scannedDate = Date(),
            deletedDate = Date()
        )
        repository.saveResult(result)
        assertEquals(result.id, source.upserted?.id)
        assertEquals(result.text, source.upserted?.text)
        assertEquals(result.title, source.upserted?.title)
        assertEquals(result.scannedDate, source.upserted?.scannedDate)
        assertEquals(result.deletedDate, source.upserted?.deletedDate)

        repository.saveResult(result.copy(text = "bbb"))
        assertEquals(result.id, source.upserted?.id)
        assertEquals("bbb", source.upserted?.text)
        assertEquals(result.title, source.upserted?.title)
        assertEquals(result.scannedDate, source.upserted?.scannedDate)
        assertEquals(result.deletedDate, source.upserted?.deletedDate)
    }

    @Test
    fun testMark_UnmarkAsDelete() = runTest {
        val source = FakeLocalDataSource()
        val repository = DefaultScannedResultRepository(source)

        val id = "1"
        repository.markAsDelete(id)
        assertEquals(id, source.updated?.id)
        assertEquals(source.result.text, source.updated?.text)
        assertEquals(source.result.title, source.updated?.title)
        assertEquals(source.result.scannedDate, source.updated?.scannedDate)
        assertNotNull(source.updated?.deletedDate)
        assertTrue(Date().time - (source.updated?.deletedDate?.time ?: 0) < 1000)

        repository.unmarkAsDelete(id)
        assertEquals(id, source.updated?.id)
        assertEquals(source.result.text, source.updated?.text)
        assertEquals(source.result.title, source.updated?.title)
        assertEquals(source.result.scannedDate, source.updated?.scannedDate)
        assertNull(source.updated?.deletedDate)
    }

    @Test
    fun testForceDelete() = runTest {
        val source = FakeLocalDataSource()
        val repository = DefaultScannedResultRepository(source)

        val id = "1"
        repository.forceDelete(id)
        assertEquals(1, source.deleted.count())
        assertEquals(id, source.deleted[0].id)
    }

    @Test
    fun testPurgeExpired() = runTest {
        val source = FakeLocalDataSource()
        val repository = DefaultScannedResultRepository(source)

        repository.purgeExpired()
        assertEquals(1, source.deleted.count())
        assertEquals("3", source.deleted[0].id)
    }

    @Test
    fun testUpdateTitle() = runTest {
        val source = FakeLocalDataSource()
        val repository = DefaultScannedResultRepository(source)

        val id = "1"
        val title = "title"
        repository.updateTitle(id, title)
        assertEquals(id, source.updated?.id)
        assertEquals(title, source.updated?.title)
    }

    class FakeLocalDataSource : LocalDataSource {
        val flow = MutableSharedFlow<List<LocalScannedResult>>()
        override fun observeResults(): Flow<List<LocalScannedResult>> {
            return flow
        }

        val results = listOf(
            LocalScannedResult(
                id = "1",
                text = "",
                title = "",
                scannedDate = Date(),
                deletedDate = null
            ),
            LocalScannedResult(
                id = "2",
                text = "",
                title = "",
                scannedDate = Date(),
                deletedDate = Date(Date().time - 30L * 24 * 60 * 60 * 1000 + 1000)
            ),
            LocalScannedResult(
                id = "3",
                text = "",
                title = "",
                scannedDate = Date(),
                deletedDate = Date(Date().time - 30L * 24 * 60 * 60 * 1000 - 1000)
            )
        )
        override suspend fun getResults(): List<LocalScannedResult> {
            return results
        }

        val result = LocalScannedResult(
            id = "",
            text = "aaa",
            title = "title1",
            scannedDate = Date(),
            deletedDate = null
        )
        override suspend fun getResult(id: String): LocalScannedResult {
            return result.copy(id = id)
        }

        override suspend fun insert(result: LocalScannedResult) {}

        var updated: LocalScannedResult? = null
        override suspend fun update(result: LocalScannedResult) {
            updated = result
        }

        var upserted: LocalScannedResult? = null
        override suspend fun upsert(result: LocalScannedResult) {
            upserted = result
        }

        var deleted: MutableList<LocalScannedResult> = mutableListOf()
        override suspend fun delete(result: LocalScannedResult) {
            deleted.add(result)
        }

        override fun observeQueryHistory(): Flow<List<LocalQueryHistory>> { return flowOf() }
        override suspend fun getQueryHistory(): List<LocalQueryHistory> { return listOf() }
        override suspend fun insert(query: LocalQueryHistory) {}
        override suspend fun delete(query: LocalQueryHistory) {}
    }
}
