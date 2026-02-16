package com.iamonzon.dory.ui.actionhub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.iamonzon.dory.ui.theme.DoryTheme

@Composable
fun ActionHubDialog(
    onDismiss: () -> Unit,
    onAddNewItem: () -> Unit,
    onStartReviewSession: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("What would you like to do?") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilledTonalButton(
                    onClick = {
                        onDismiss()
                        onAddNewItem()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Add new item")
                }
                FilledTonalButton(
                    onClick = {
                        onDismiss()
                        onStartReviewSession()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Start review session")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun ActionHubDialogPreview() {
    DoryTheme {
        ActionHubDialog(
            onDismiss = {},
            onAddNewItem = {},
            onStartReviewSession = {}
        )
    }
}
