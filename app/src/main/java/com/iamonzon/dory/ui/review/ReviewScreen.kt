package com.iamonzon.dory.ui.review

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.iamonzon.dory.ui.components.DoryTopAppBar
import com.iamonzon.dory.ui.theme.DoryTheme
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ReviewScreen(
    itemId: Long,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val item = remember { MockData.itemById(itemId) }
    val reviews = remember { MockData.reviewsForItem(itemId) }
    val category = remember { MockData.categories.find { it.id == item?.categoryId } }
    var newNotes by remember { mutableStateOf("") }

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("MMM d, yyyy")
            .withZone(ZoneId.systemDefault())
    }

    val ratingLabels = listOf(
        stringResource(R.string.review_rating_again),
        stringResource(R.string.review_rating_hard),
        stringResource(R.string.review_rating_good),
        stringResource(R.string.review_rating_easy)
    )

    Scaffold(
        topBar = {
            DoryTopAppBar(
                title = stringResource(R.string.review_title),
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        if (item == null) {
            Text(
                stringResource(R.string.review_item_not_found),
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
            // Item details
            Text(text = item.title, style = MaterialTheme.typography.titleLarge)
            Text(
                text = item.source,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (category != null) {
                Text(
                    text = stringResource(R.string.review_category_format, category.name),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            if (!item.notes.isNullOrBlank()) {
                Text(
                    text = item.notes,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            HorizontalDivider()

            // Review history
            Text(text = stringResource(R.string.review_history_title), style = MaterialTheme.typography.titleMedium)
            if (reviews.isEmpty()) {
                Text(
                    text = stringResource(R.string.review_no_reviews),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                reviews.forEach { review ->
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
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = review.notes,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // New review
            Text(text = stringResource(R.string.review_new_review_title), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = newNotes,
                onValueChange = { newNotes = it },
                label = { Text(stringResource(R.string.review_notes_label)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ratingLabels.forEach { label ->
                    if (label == stringResource(R.string.review_rating_good) || label == stringResource(R.string.review_rating_easy)) {
                        Button(
                            onClick = {
                                Toast.makeText(context, context.getString(R.string.toast_reviewed_as, label), Toast.LENGTH_SHORT).show()
                                onBackClick()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(label)
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                Toast.makeText(context, context.getString(R.string.toast_reviewed_as, label), Toast.LENGTH_SHORT).show()
                                onBackClick()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(label)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReviewScreenPreview() {
    DoryTheme {
        ReviewScreen(itemId = 1L, onBackClick = {})
    }
}
