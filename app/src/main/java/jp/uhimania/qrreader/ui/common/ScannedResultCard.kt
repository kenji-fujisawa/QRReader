package jp.uhimania.qrreader.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import jp.uhimania.qrreader.R
import jp.uhimania.qrreader.domain.DateFormat

@Composable
fun ScannedResultCard(
    uiState: ScannedResultUiState,
    modifier: Modifier = Modifier,
    menu: @Composable () -> Unit
) {
    ElevatedCard(modifier = modifier.padding(4.dp)) {
        Row(modifier = Modifier.padding(8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                if (!uiState.title.isEmpty()) {
                    Text(uiState.title)
                }

                if (uiState.isUrl) {
                    val uriHandler = LocalUriHandler.current
                    TextButton({ uriHandler.openUri(uiState.text) }) {
                        Text(
                            text = uiState.text,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                } else {
                    Text(
                        text = uiState.text,
                        style = if (uiState.title.isEmpty()) {
                            TextStyle.Default
                        } else {
                            MaterialTheme.typography.bodyMedium
                        }
                    )
                }

                Text(
                    text = when (uiState.date) {
                        is DateFormat.Today -> stringResource(R.string.date_format_today)
                        is DateFormat.DaysAgo -> stringResource(R.string.date_format_days_ago, uiState.date.day)
                        is DateFormat.MonthsAgo -> stringResource(R.string.date_format_months_ago, uiState.date.month)
                        is DateFormat.Date -> uiState.date.date
                    },
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                )
            }

            menu()
        }
    }
}
