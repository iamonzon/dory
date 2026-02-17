package com.iamonzon.dory.ui.dashboard

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
fun ItemEditScreen(
    viewModel: ItemEditViewModel,
    onBackClick: () -> Unit
) {
    val item by viewModel.item.collectAsStateWithLifecycle()
    val editState by viewModel.editState.collectAsStateWithLifecycle()
    val recentReviews by viewModel.recentReviews.collectAsStateWithLifecycle()

    LaunchedEffect(editState.savedSuccessfully) {
        if (editState.savedSuccessfully) {
            onBackClick()
        }
    }

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
                value = editState.title,
                onValueChange = { viewModel.updateTitle(it) },
                label = { Text(stringResource(R.string.edit_label_title)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = editState.source,
                onValueChange = { viewModel.updateSource(it) },
                label = { Text(stringResource(R.string.edit_label_source)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = editState.notes,
                onValueChange = { viewModel.updateNotes(it) },
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
                                    text = ratingDisplayName(review.rating),
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
                onClick = { viewModel.saveChanges() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.edit_save_changes))
            }
        }
    }
}
