package jp.uhimania.qrreader.data.source

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "scanned_results")
data class LocalScannedResult(
    @PrimaryKey val id: String,
    val text: String,
    @ColumnInfo(name = "scanned_date") val scannedDate: Date,
    @ColumnInfo(name = "deleted_date") val deletedDate: Date?
)
