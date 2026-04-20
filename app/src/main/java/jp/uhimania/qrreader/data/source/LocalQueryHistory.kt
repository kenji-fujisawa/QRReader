package jp.uhimania.qrreader.data.source

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "query_history")
data class LocalQueryHistory(
    @PrimaryKey val query: String,
    val date: Date
)
