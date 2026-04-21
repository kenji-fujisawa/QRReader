package jp.uhimania.qrreader.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import jp.uhimania.qrreader.R
import jp.uhimania.qrreader.domain.DateFormat
import jp.uhimania.qrreader.ui.theme.QRReaderTheme

@Composable
fun ScannedResultCard(
    uiState: ScannedResultUiState,
    showCheckBox: Boolean,
    onCheckChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    menu: @Composable () -> Unit
) {
    ElevatedCard(modifier = modifier.padding(4.dp)) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(8.dp)
        ) {
            AnimatedVisibility(
                visible = showCheckBox,
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Checkbox(
                    checked = uiState.selected,
                    onCheckedChange = onCheckChange
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                if (!uiState.title.isEmpty()) {
                    Text(uiState.title)
                }

                if (uiState.isUrl) {
                    val uriHandler = LocalUriHandler.current
                    TextButton(
                        onClick = { uriHandler.openUri(uiState.text) },
                        enabled = !showCheckBox
                    ) {
                        Text(
                            text = uiState.text,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }

                    if (!uiState.description.isEmpty()) {
                        Text(
                            text = uiState.description,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (!uiState.image.isEmpty()) {
                        AsyncImage(
                            model = uiState.image,
                            contentDescription = uiState.image,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .padding(4.dp)
                                .clickable {
                                    if (!showCheckBox) {
                                        uriHandler.openUri(uiState.text)
                                    }
                                },
                            contentScale = ContentScale.Crop
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

                    if (!uiState.description.isEmpty()) {
                        Text(
                            text = uiState.description,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

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

            AnimatedVisibility(!showCheckBox) {
                menu()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScannedResultCardPreview() {
    QRReaderTheme {
        val uiState = ScannedResultUiState(text = "aaa")
        ScannedResultCard(
            uiState = uiState,
            showCheckBox = true,
            onCheckChange = {}
        ) {
            IconButton({}) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = Icons.Default.MoreVert.name
                )
            }
        }
    }
}
