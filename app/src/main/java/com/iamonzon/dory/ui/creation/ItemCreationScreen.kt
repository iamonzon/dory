package com.iamonzon.dory.ui.creation

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.iamonzon.dory.R
import com.iamonzon.dory.data.mock.MockData
import com.iamonzon.dory.ui.theme.DoryTheme
import com.iamonzon.dory.ui.components.DoryTopAppBar

private enum class CreationStep { TITLE, SOURCE, CATEGORY, NOTES }

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ItemCreationScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(CreationStep.TITLE) }
    var title by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }
    var selectedCategoryName by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }
    var showMoreOptions by remember { mutableStateOf(false) }

    val stepIndex = CreationStep.entries.indexOf(currentStep)
    val progress = (stepIndex + 1).toFloat() / CreationStep.entries.size

    Scaffold(
        topBar = {
            DoryTopAppBar(
                title = stringResource(R.string.creation_title),
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.creation_step_format, stepIndex + 1, CreationStep.entries.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                },
                label = "creation_step",
                modifier = Modifier.weight(1f)
            ) { step ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (step) {
                        CreationStep.TITLE -> {
                            Text(stringResource(R.string.creation_title_prompt), style = MaterialTheme.typography.titleLarge)
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text(stringResource(R.string.creation_title_label)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        CreationStep.SOURCE -> {
                            Text(stringResource(R.string.creation_source_prompt), style = MaterialTheme.typography.titleLarge)
                            OutlinedTextField(
                                value = source,
                                onValueChange = { source = it },
                                label = { Text(stringResource(R.string.creation_source_label)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            if (!showMoreOptions) {
                                TextButton(onClick = { showMoreOptions = true }) {
                                    Text(stringResource(R.string.creation_more_options))
                                }
                            }
                        }

                        CreationStep.CATEGORY -> {
                            Text(stringResource(R.string.creation_category_prompt), style = MaterialTheme.typography.titleLarge)
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedCategoryName ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(stringResource(R.string.creation_category_label)) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    MockData.categories.forEach { category ->
                                        DropdownMenuItem(
                                            text = { Text(category.name) },
                                            onClick = {
                                                selectedCategoryName = category.name
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        CreationStep.NOTES -> {
                            Text(stringResource(R.string.creation_notes_prompt), style = MaterialTheme.typography.titleLarge)
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text(stringResource(R.string.creation_notes_label)) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 4
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentStep != CreationStep.TITLE) {
                    OutlinedButton(
                        onClick = {
                            val idx = CreationStep.entries.indexOf(currentStep)
                            if (idx > 0) currentStep = CreationStep.entries[idx - 1]
                        }
                    ) {
                        Text(stringResource(R.string.creation_back))
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                if (currentStep == CreationStep.NOTES) {
                    Button(
                        onClick = {
                            Toast.makeText(context, context.getString(R.string.toast_item_saved), Toast.LENGTH_SHORT).show()
                            onBackClick()
                        }
                    ) {
                        Text(stringResource(R.string.creation_save))
                    }
                } else {
                    Button(
                        onClick = {
                            val idx = CreationStep.entries.indexOf(currentStep)
                            if (idx < CreationStep.entries.size - 1) {
                                currentStep = CreationStep.entries[idx + 1]
                            }
                        },
                        enabled = when (currentStep) {
                            CreationStep.TITLE -> title.isNotBlank()
                            CreationStep.SOURCE -> source.isNotBlank()
                            else -> true
                        }
                    ) {
                        Text(stringResource(R.string.creation_next))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemCreationScreenPreview() {
    DoryTheme {
        ItemCreationScreen(onBackClick = {})
    }
}
