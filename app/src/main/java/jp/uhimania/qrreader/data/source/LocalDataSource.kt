package jp.uhimania.qrreader.data.source

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface LocalDataSource {
    @Query("SELECT * FROM scanned_results ORDER BY scanned_date")
    fun observeResults(): Flow<List<LocalScannedResult>>

    @Query("SELECT * FROM scanned_results ORDER BY scanned_date")
    suspend fun getResults(): List<LocalScannedResult>

    @Query("SELECT * FROM scanned_results WHERE id = :id")
    suspend fun getResult(id: String): LocalScannedResult?

    @Insert suspend fun insert(result: LocalScannedResult)
    @Update suspend fun update(result: LocalScannedResult)
    @Upsert suspend fun upsert(result: LocalScannedResult)
    @Delete suspend fun delete(result: LocalScannedResult)
}

@Database(entities = [LocalScannedResult::class], version = 1)
@TypeConverters(DateConverter::class)
abstract class LocalDatabase : RoomDatabase() {
    abstract fun dataSource(): LocalDataSource

    companion object {
        @Volatile private var instance: LocalDatabase? = null

        fun getDatabase(context: Context): LocalDatabase {
            return instance ?: synchronized(this) {
                Room.databaseBuilder(context, LocalDatabase::class.java, "QRReader.db")
                    .build()
                    .also { instance = it }
            }
        }
    }
}

class DateConverter {
    @TypeConverter
    fun from(value: Long): Date {
        return Date(value)
    }

    @TypeConverter
    fun to(value: Date): Long {
        return value.time
    }
}
