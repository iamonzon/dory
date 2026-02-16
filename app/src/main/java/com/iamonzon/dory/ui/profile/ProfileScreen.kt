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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.iamonzon.dory.data.mock.MockData
import com.iamonzon.dory.ui.theme.DoryTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onCategoryManagement: () -> Unit,
    onArchivedItems: () -> Unit,
    onAdvancedSettings: () -> Unit
) {
    val stats = remember { MockData.profileStats }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Profile") })
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
            Text(text = "Mastery Overview", style = MaterialTheme.typography.titleLarge)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard(label = "Total", value = stats.totalItems.toString())
                StatCard(label = "Mastered", value = stats.masteredCount.toString())
                StatCard(label = "Struggling", value = stats.strugglingCount.toString())
            }

            HorizontalDivider()

            // By category
            Text(text = "By Category", style = MaterialTheme.typography.titleMedium)
            stats.byCategory.forEach { (name, count) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "$count items",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            // Notification time (mock)
            Text(text = "Daily Reminder", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "9:00 AM",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            // Navigation links
            Text(text = "Settings", style = MaterialTheme.typography.titleMedium)

            NavigationCard(
                title = "Category Management",
                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                onClick = onCategoryManagement
            )
            NavigationCard(
                title = "Archived Items",
                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                onClick = onArchivedItems
            )
            NavigationCard(
                title = "Advanced Settings",
                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
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

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    DoryTheme {
        ProfileScreen(
            onCategoryManagement = {},
            onArchivedItems = {},
            onAdvancedSettings = {}
        )
    }
}
