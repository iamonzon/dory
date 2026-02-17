package com.iamonzon.dory.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iamonzon.dory.R
import com.iamonzon.dory.data.repository.CategoryDeleteStrategy
import com.iamonzon.dory.ui.components.DoryTopAppBar

@Composable
fun CategoryManagementScreen(
    viewModel: CategoryManagementViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var renameCategory by remember { mutableStateOf<Long?>(null) }
    var deleteCategory by remember { mutableStateOf<Long?>(null) }
    var newName by remember { mutableStateOf("") }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(stringResource(R.string.category_create_title)) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.category_create_label)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.createCategory(newName)
                        showCreateDialog = false
                        newName = ""
                    }
                ) {
                    Text(stringResource(R.string.category_create_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false; newName = "" }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    renameCategory?.let { catId ->
        AlertDialog(
            onDismissRequest = { renameCategory = null },
            title = { Text(stringResource(R.string.category_rename_title)) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.category_rename_label)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renameCategory(catId, newName)
                        renameCategory = null
                        newName = ""
                    }
                ) {
                    Text(stringResource(R.string.category_rename_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { renameCategory = null; newName = "" }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    deleteCategory?.let { catId ->
        val cat = uiState.categories.find { it.category.id == catId }
        AlertDialog(
            onDismissRequest = { deleteCategory = null },
            title = { Text(stringResource(R.string.category_delete_title, cat?.category?.name ?: "")) },
            text = { Text(stringResource(R.string.category_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCategory(catId, CategoryDeleteStrategy.UncategorizeItems)
                        deleteCategory = null
                    }
                ) {
                    Text(stringResource(R.string.category_delete_uncategorize))
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            viewModel.deleteCategory(catId, CategoryDeleteStrategy.DeleteItems)
                            deleteCategory = null
                        }
                    ) {
                        Text(stringResource(R.string.category_delete_items_too))
                    }
                    TextButton(onClick = { deleteCategory = null }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            DoryTopAppBar(
                title = stringResource(R.string.category_title),
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.category_add_content_description))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.categories, key = { it.category.id }) { categoryWithCount ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        newName = categoryWithCount.category.name
                        renameCategory = categoryWithCount.category.id
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = categoryWithCount.category.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = pluralStringResource(R.plurals.category_item_count, categoryWithCount.itemCount, categoryWithCount.itemCount),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = { deleteCategory = categoryWithCount.category.id }
                        ) {
                            Text(stringResource(R.string.common_delete))
                        }
                    }
                }
            }
        }
    }
}
