package com.iamonzon.dory.ui.profile

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.iamonzon.dory.data.mock.MockData
import com.iamonzon.dory.ui.theme.DoryTheme
import com.iamonzon.dory.ui.components.DoryTopAppBar

@Composable
fun CategoryManagementScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val categories = remember { MockData.categories }
    val itemCounts = remember {
        MockData.items
            .filter { !it.isArchived }
            .groupBy { it.categoryId }
            .mapValues { it.value.size }
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var renameCategory by remember { mutableStateOf<Long?>(null) }
    var deleteCategory by remember { mutableStateOf<Long?>(null) }
    var newName by remember { mutableStateOf("") }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Category") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Category name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCreateDialog = false
                        newName = ""
                        Toast.makeText(context, "Category created", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false; newName = "" }) {
                    Text("Cancel")
                }
            }
        )
    }

    renameCategory?.let { catId ->
        val cat = categories.find { it.id == catId }
        AlertDialog(
            onDismissRequest = { renameCategory = null },
            title = { Text("Rename Category") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        renameCategory = null
                        newName = ""
                        Toast.makeText(context, "Category renamed", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameCategory = null; newName = "" }) {
                    Text("Cancel")
                }
            }
        )
    }

    deleteCategory?.let { catId ->
        val cat = categories.find { it.id == catId }
        AlertDialog(
            onDismissRequest = { deleteCategory = null },
            title = { Text("Delete \"${cat?.name}\"?") },
            text = { Text("What should happen to items in this category?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteCategory = null
                        Toast.makeText(context, "Category deleted, items uncategorized", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Uncategorize items")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            deleteCategory = null
                            Toast.makeText(context, "Category and items deleted", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Delete items too")
                    }
                    TextButton(onClick = { deleteCategory = null }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            DoryTopAppBar(
                title = "Categories",
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add category")
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
            items(categories, key = { it.id }) { category ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        newName = category.name
                        renameCategory = category.id
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
                                text = category.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${itemCounts[category.id] ?: 0} items",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = { deleteCategory = category.id }
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoryManagementScreenPreview() {
    DoryTheme {
        CategoryManagementScreen(onBackClick = {})
    }
}
