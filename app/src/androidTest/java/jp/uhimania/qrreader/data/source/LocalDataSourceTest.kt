package jp.uhimania.qrreader.data.source

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Date

class LocalDataSourceTest {
    private lateinit var source: LocalDataSource
    private lateinit var database: LocalDatabase

    private val results = listOf(
        LocalScannedResult(
            id = "1",
            text = "aaa",
            title = "title1",
            description = "desc1",
            image = "image1",
            scannedDate = Date(),
            deletedDate = null
        ),
        LocalScannedResult(
            id = "2",
            text = "bbb",
            title = "title2",
            description = "desc2",
            image = "image2",
            scannedDate = Date(),
            deletedDate = Date()
        ),
        LocalScannedResult(
            id = "3",
            text = "ccc",
            title = "title3",
            description = "desc3",
            image = "image3",
            scannedDate = Date(),
            deletedDate = null
        )
    )

    private val queries = listOf(
        LocalQueryHistory(
            query = "aaa",
            date = Date()
        ),
        LocalQueryHistory(
            query = "bbb",
            date = Date()
        ),
        LocalQueryHistory(
            query = "ccc",
            date = Date()
        )
    )

    @Before
    fun setup() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, LocalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        source = database.dataSource()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testInsertResults() = runBlocking {
        results.forEach { source.insert(it) }

        val values = source.getResults()
        assertEquals(results.count(), values.count())
        assertEquals(results[0], values[0])
        assertEquals(results[1], values[1])
        assertEquals(results[2], values[2])
    }

    @Test
    fun testUpdateResults() = runBlocking {
        results.forEach { source.insert(it) }

        val result0 = results[0].copy(text = "test")
        source.update(result0)

        val result2 = results[2].copy(scannedDate = Date())
        source.update(result2)

        val values = source.getResults()
        assertEquals(results.count(), values.count())
        assertEquals(result0, values[0])
        assertEquals(results[1], values[1])
        assertEquals(result2, values[2])
    }

    @Test
    fun testUpsertResults() = runBlocking {
        results.forEach { source.upsert(it) }

        var values = source.getResults()
        assertEquals(results.count(), values.count())
        assertEquals(results[0], values[0])
        assertEquals(results[1], values[1])
        assertEquals(results[2], values[2])

        val result0 = results[0].copy(text = "test")
        source.upsert(result0)

        val result2 = results[2].copy(scannedDate = Date())
        source.upsert(result2)

        values = source.getResults()
        assertEquals(results.count(), values.count())
        assertEquals(result0, values[0])
        assertEquals(results[1], values[1])
        assertEquals(result2, values[2])
    }

    @Test
    fun testDeleteResults() = runBlocking {
        results.forEach { source.insert(it) }

        source.delete(results[0])
        source.delete(results[1])

        val values = source.getResults()
        assertEquals(1, values.count())
        assertEquals(results[2], values[0])
    }

    @Test
    fun testObserveResults() = runBlocking {
        results.forEach { source.insert(it) }

        val values = source.observeResults().first()
        assertEquals(results.count(), values.count())
        assertEquals(results[0], values[0])
        assertEquals(results[1], values[1])
        assertEquals(results[2], values[2])
    }

    @Test
    fun testGetResult() = runBlocking {
        results.forEach { source.insert(it) }

        var value = source.getResult(results[0].id)
        assertEquals(results[0], value)

        value = source.getResult(results[2].id)
        assertEquals(results[2], value)

        value = source.getResult("")
        assertNull(value)
    }

    @Test
    fun testInsertQuery() = runBlocking {
        queries.forEach { source.insert(it) }

        val values = source.getQueryHistory()
        assertEquals(queries.count(), values.count())
        assertEquals(queries[0], values[0])
        assertEquals(queries[1], values[1])
        assertEquals(queries[2], values[2])
    }

    @Test
    fun testDeleteQuery() = runBlocking {
        queries.forEach { source.insert(it) }

        source.delete(queries[0])
        source.delete(queries[1])

        val values = source.getQueryHistory()
        assertEquals(1, values.count())
        assertEquals(queries[2], values[0])
    }

    @Test
    fun testObserveQueryHistory() = runBlocking {
        queries.forEach { source.insert(it) }

        val values = source.observeQueryHistory().first()
        assertEquals(queries.count(), values.count())
        assertEquals(queries[0], values[0])
        assertEquals(queries[1], values[1])
        assertEquals(queries[2], values[2])
    }
}
