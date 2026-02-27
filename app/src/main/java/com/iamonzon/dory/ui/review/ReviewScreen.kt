package com.iamonzon.dory.ui.review

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iamonzon.dory.R
import com.iamonzon.dory.algorithm.Rating
import com.iamonzon.dory.ui.components.DoryTopAppBar
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
private fun ratingDisplayName(rating: Rating): String = when (rating) {
    Rating.Again -> stringResource(R.string.review_rating_again)
    Rating.Hard -> stringResource(R.string.review_rating_hard)
    Rating.Good -> stringResource(R.string.review_rating_good)
    Rating.Easy -> stringResource(R.string.review_rating_easy)
}

@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel,
    onBackClick: () -> Unit
) {
    val item by viewModel.item.collectAsStateWithLifecycle()
    val reviews by viewModel.reviews.collectAsStateWithLifecycle()
    val category by viewModel.category.collectAsStateWithLifecycle()
    var newNotes by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.reviewSubmitted.collect {
            onBackClick()
        }
    }

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("MMM d, yyyy")
            .withZone(ZoneId.systemDefault())
    }

    val ratingOptions = listOf(
        Rating.Again to stringResource(R.string.review_rating_again),
        Rating.Hard to stringResource(R.string.review_rating_hard),
        Rating.Good to stringResource(R.string.review_rating_good),
        Rating.Easy to stringResource(R.string.review_rating_easy)
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

        val loadedItem = item!!

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Item details
            Text(text = loadedItem.title, style = MaterialTheme.typography.titleLarge)
            Text(
                text = loadedItem.source,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (category != null) {
                Text(
                    text = stringResource(R.string.review_category_format, category!!.name),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            if (!loadedItem.notes.isNullOrBlank()) {
                Text(
                    text = loadedItem.notes,
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
                                    text = ratingDisplayName(review.rating),
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
                ratingOptions.forEach { (rating, label) ->
                    if (rating == Rating.Good || rating == Rating.Easy) {
                        Button(
                            onClick = {
                                viewModel.submitReview(rating, newNotes.ifBlank { null })
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(label)
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                viewModel.submitReview(rating, newNotes.ifBlank { null })
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
