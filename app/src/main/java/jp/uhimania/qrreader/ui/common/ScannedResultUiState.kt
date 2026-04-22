package jp.uhimania.qrreader.ui.common

import jp.uhimania.qrreader.domain.DateFormat

data class ScannedResultUiState(
    val id: String = "",
    val text: String = "",
    val title: String = "",
    val description: String = "",
    val image: String = "",
    val isUrl: Boolean = false,
    val date: DateFormat = DateFormat.Today,
    val selected: Boolean = false
)
