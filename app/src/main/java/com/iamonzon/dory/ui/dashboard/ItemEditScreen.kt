package com.iamonzon.dory.ui.dashboard

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.iamonzon.dory.R
import com.iamonzon.dory.data.mock.MockData
import com.iamonzon.dory.ui.theme.DoryTheme
import com.iamonzon.dory.ui.components.DoryTopAppBar
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ItemEditScreen(
    itemId: Long,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val item = remember { MockData.itemById(itemId) }
    val recentReviews = remember { MockData.reviewsForItem(itemId).takeLast(3) }

    var title by remember { mutableStateOf(item?.title ?: "") }
    var source by remember { mutableStateOf(item?.source ?: "") }
    var notes by remember { mutableStateOf(item?.notes ?: "") }

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("MMM d, yyyy")
            .withZone(ZoneId.systemDefault())
    }

    Scaffold(
        topBar = {
            DoryTopAppBar(
                title = stringResource(R.string.edit_title),
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        if (item == null) {
            Text(
                stringResource(R.string.edit_item_not_found),
                modifier = Modifier.padding(padding).padding(16.dp)
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.edit_label_title)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = source,
                onValueChange = { source = it },
                label = { Text(stringResource(R.string.edit_label_source)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.edit_label_notes)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            HorizontalDivider()

            Text(text = stringResource(R.string.edit_recent_reviews), style = MaterialTheme.typography.titleMedium)
            if (recentReviews.isEmpty()) {
                Text(
                    text = stringResource(R.string.edit_no_reviews),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                recentReviews.forEach { review ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = review.rating.name,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = dateFormatter.format(review.reviewedAt),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            if (!review.notes.isNullOrBlank()) {
                                Text(
                                    text = review.notes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    Toast.makeText(context, context.getString(R.string.toast_changes_saved), Toast.LENGTH_SHORT).show()
                    onBackClick()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.edit_save_changes))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemEditScreenPreview() {
    DoryTheme {
        ItemEditScreen(itemId = 1L, onBackClick = {})
    }
}
