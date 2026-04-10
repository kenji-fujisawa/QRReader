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
import org.junit.Assert.assertNotNull
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
                scannedDate = Date(),
                deletedDate = null
            ),
            LocalScannedResult(
                id = "3",
                text = "ccc",
                scannedDate = Date(),
                deletedDate = Date()
            )
        )
        source.flow.emit(records2)
        assertEquals(2, values.count())
        assertEquals(1, values[0].count())
        assertEquals(records1[0].id, values[0][0].id)
        assertEquals(records1[0].text, values[0][0].text)
        assertEquals(records1[0].scannedDate, values[0][0].scannedDate)
        assertEquals(records1[0].deletedDate, values[0][0].deletedDate)
        assertEquals(1, values[1].count())
        assertEquals(records2[0].id, values[1][0].id)
        assertEquals(records2[0].text, values[1][0].text)
        assertEquals(records2[0].scannedDate, values[1][0].scannedDate)
        assertEquals(records2[0].deletedDate, values[1][0].deletedDate)
    }

    @Test
    fun testSaveResult() = runTest {
        val source = FakeLocalDataSource()
        val repository = DefaultScannedResultRepository(source)

        val result = ScannedResult(
            id = "1",
            text = "aaa",
            scannedDate = Date(),
            deletedDate = Date()
        )
        repository.saveResult(result)
        assertEquals(result.id, source.upserted?.id)
        assertEquals(result.text, source.upserted?.text)
        assertEquals(result.scannedDate, source.upserted?.scannedDate)
        assertEquals(result.deletedDate, source.upserted?.deletedDate)

        repository.saveResult(result.copy(text = "bbb"))
        assertEquals(result.id, source.upserted?.id)
        assertEquals("bbb", source.upserted?.text)
        assertEquals(result.scannedDate, source.upserted?.scannedDate)
        assertEquals(result.deletedDate, source.upserted?.deletedDate)
    }

    @Test
    fun testMarkAsDelete() = runTest {
        val source = FakeLocalDataSource()
        val repository = DefaultScannedResultRepository(source)

        val id = "1"
        repository.markAsDelete(id)
        assertEquals(id, source.updated?.id)
        assertEquals(source.result.text, source.updated?.text)
        assertEquals(source.result.scannedDate, source.updated?.scannedDate)
        assertNotNull(source.updated?.deletedDate)
        assertTrue(Date().time - (source.updated?.deletedDate?.time ?: 0) < 1000)
    }

    class FakeLocalDataSource : LocalDataSource {
        val flow = MutableSharedFlow<List<LocalScannedResult>>()
        override fun observeResults(): Flow<List<LocalScannedResult>> {
            return flow
        }
        override suspend fun getResults(): List<LocalScannedResult> { return listOf() }

        val result = LocalScannedResult(
            id = "",
            text = "aaa",
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

        var deleted: LocalScannedResult? = null
        override suspend fun delete(result: LocalScannedResult) {
            deleted = result
        }
    }
}
