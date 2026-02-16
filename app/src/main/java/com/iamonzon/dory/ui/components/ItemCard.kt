package com.iamonzon.dory.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.iamonzon.dory.data.model.ReviewUrgency
import com.iamonzon.dory.ui.theme.DoryTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemCard(
    title: String,
    urgency: ReviewUrgency,
    categoryName: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UrgencyIndicator(urgency = urgency)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                if (categoryName != null) {
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                text = categoryName,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemCardPreview() {
    DoryTheme {
        ItemCard(
            title = "Kotlin Coroutines",
            urgency = ReviewUrgency.Overdue,
            categoryName = "Programming",
            onClick = {},
            onLongClick = {}
        )
    }
}
