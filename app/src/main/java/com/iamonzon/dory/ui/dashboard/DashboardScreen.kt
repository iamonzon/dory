package com.iamonzon.dory.ui.dashboard

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.iamonzon.dory.R
import com.iamonzon.dory.data.mock.MockData
import com.iamonzon.dory.ui.theme.DoryTheme
import com.iamonzon.dory.ui.components.ItemCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onItemClick: (Long) -> Unit,
    onEditItem: (Long) -> Unit
) {
    val context = LocalContext.current
    val dashboardItems = remember { MockData.dashboardItems }
    var contextMenuItemId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.dashboard_title)) })
        }
    ) { padding ->
        if (dashboardItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.dashboard_empty_message),
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
                items(dashboardItems, key = { it.item.id }) { dashboardItem ->
                    Box {
                        ItemCard(
                            title = dashboardItem.item.title,
                            urgency = dashboardItem.urgency,
                            categoryName = dashboardItem.categoryName,
                            onClick = { onItemClick(dashboardItem.item.id) },
                            onLongClick = { contextMenuItemId = dashboardItem.item.id }
                        )

                        DropdownMenu(
                            expanded = contextMenuItemId == dashboardItem.item.id,
                            onDismissRequest = { contextMenuItemId = null }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.dashboard_menu_edit)) },
                                onClick = {
                                    contextMenuItemId = null
                                    onEditItem(dashboardItem.item.id)
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.dashboard_icon_edit)) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.dashboard_menu_archive)) },
                                onClick = {
                                    contextMenuItemId = null
                                    Toast.makeText(context, context.getString(R.string.toast_item_archived), Toast.LENGTH_SHORT).show()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = stringResource(R.string.dashboard_icon_archive)
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.dashboard_menu_delete)) },
                                onClick = {
                                    contextMenuItemId = null
                                    Toast.makeText(context, context.getString(R.string.toast_item_deleted), Toast.LENGTH_SHORT).show()
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.dashboard_icon_delete)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    DoryTheme {
        DashboardScreen(onItemClick = {}, onEditItem = {})
    }
}
