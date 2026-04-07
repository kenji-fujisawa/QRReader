package jp.uhimania.qrreader.data.source

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
            date = Date()
        ),
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
    fun testInsert() = runBlocking {
        results.forEach { source.insert(it) }

        val values = source.getResults()
        assertEquals(results.count(), values.count())
        assertEquals(results[0], values[0])
        assertEquals(results[1], values[1])
        assertEquals(results[2], values[2])
    }

    @Test
    fun testUpdate() = runBlocking {
        results.forEach { source.insert(it) }

        val result0 = results[0].copy(text = "test")
        source.update(result0)

        val result2 = results[2].copy(date = Date())
        source.update(result2)

        val values = source.getResults()
        assertEquals(results.count(), values.count())
        assertEquals(result0, values[0])
        assertEquals(results[1], values[1])
        assertEquals(result2, values[2])
    }

    @Test
    fun testUpsert() = runBlocking {
        results.forEach { source.upsert(it) }

        var values = source.getResults()
        assertEquals(results.count(), values.count())
        assertEquals(results[0], values[0])
        assertEquals(results[1], values[1])
        assertEquals(results[2], values[2])

        val result0 = results[0].copy(text = "test")
        source.upsert(result0)

        val result2 = results[2].copy(date = Date())
        source.upsert(result2)

        values = source.getResults()
        assertEquals(results.count(), values.count())
        assertEquals(result0, values[0])
        assertEquals(results[1], values[1])
        assertEquals(result2, values[2])
    }

    @Test
    fun testDelete() = runBlocking {
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
}
