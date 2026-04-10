package jp.uhimania.qrreader.data

import java.util.Date
import java.util.UUID

data class ScannedResult(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val scannedDate: Date = Date(),
    val deletedDate: Date? = null
)
