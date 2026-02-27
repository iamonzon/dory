package com.iamonzon.dory.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iamonzon.dory.R
import com.iamonzon.dory.ui.components.DoryTopAppBar
import com.iamonzon.dory.ui.components.ItemCard

@Composable
fun ReviewSessionScreen(
    viewModel: ReviewSessionViewModel,
    onItemClick: (Long) -> Unit,
    onBackClick: () -> Unit
) {
    val dueItems by viewModel.dueItems.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            DoryTopAppBar(
                title = stringResource(R.string.review_session_title),
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        if (dueItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.review_session_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = pluralStringResource(R.plurals.review_session_count, dueItems.size, dueItems.size),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(dueItems, key = { it.item.id }) { dashboardItem ->
                    ItemCard(
                        title = dashboardItem.item.title,
                        urgency = dashboardItem.urgency,
                        categoryName = dashboardItem.categoryName,
                        onClick = { onItemClick(dashboardItem.item.id) },
                        onLongClick = {}
                    )
                }
            }
        }
    }
}
