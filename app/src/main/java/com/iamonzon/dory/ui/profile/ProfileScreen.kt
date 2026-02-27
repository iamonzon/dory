package com.iamonzon.dory.ui.profile

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iamonzon.dory.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onCategoryManagement: () -> Unit,
    onArchivedItems: () -> Unit,
    onAdvancedSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stats = uiState.stats

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.profile_title)) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mastery stats
            Text(text = stringResource(R.string.profile_mastery_overview), style = MaterialTheme.typography.titleLarge)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard(label = stringResource(R.string.profile_stat_total), value = stats.totalItems.toString())
                StatCard(label = stringResource(R.string.profile_stat_mastered), value = stats.masteredCount.toString())
                StatCard(label = stringResource(R.string.profile_stat_struggling), value = stats.strugglingCount.toString())
            }

            HorizontalDivider()

            // By category
            Text(text = stringResource(R.string.profile_by_category), style = MaterialTheme.typography.titleMedium)
            stats.byCategory.forEach { (name, count) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = pluralStringResource(R.plurals.category_item_count, count, count),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            // Notification time
            Text(text = stringResource(R.string.profile_daily_reminder), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.profile_notification_time_format, uiState.notificationHour, uiState.notificationMinute),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            // Navigation links
            Text(text = stringResource(R.string.profile_settings), style = MaterialTheme.typography.titleMedium)

            NavigationCard(
                title = stringResource(R.string.profile_category_management),
                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.profile_icon_category_management)) },
                onClick = onCategoryManagement
            )
            NavigationCard(
                title = stringResource(R.string.profile_archived_items),
                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.profile_icon_archived_items)) },
                onClick = onArchivedItems
            )
            NavigationCard(
                title = stringResource(R.string.profile_advanced_settings),
                icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.profile_icon_advanced_settings)) },
                onClick = onAdvancedSettings
            )
        }
    }
}

@Composable
private fun StatCard(label: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NavigationCard(
    title: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            icon()
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
