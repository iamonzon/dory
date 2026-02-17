package com.iamonzon.dory.ui.creation

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iamonzon.dory.R
import com.iamonzon.dory.ui.components.DoryTopAppBar

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ItemCreationScreen(
    viewModel: CreationViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) {
            onBackClick()
        }
    }

    var showMoreOptions by remember { mutableStateOf(false) }

    val stepIndex = CreationStep.entries.indexOf(uiState.currentStep)
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
                targetState = uiState.currentStep,
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
                                value = uiState.title,
                                onValueChange = { viewModel.updateTitle(it) },
                                label = { Text(stringResource(R.string.creation_title_label)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        CreationStep.SOURCE -> {
                            Text(stringResource(R.string.creation_source_prompt), style = MaterialTheme.typography.titleLarge)
                            OutlinedTextField(
                                value = uiState.source,
                                onValueChange = { viewModel.updateSource(it) },
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
                                    value = uiState.selectedCategoryName ?: "",
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
                                    uiState.categories.forEach { category ->
                                        DropdownMenuItem(
                                            text = { Text(category.name) },
                                            onClick = {
                                                viewModel.selectCategory(category.id, category.name)
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
                                value = uiState.notes,
                                onValueChange = { viewModel.updateNotes(it) },
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
                if (uiState.currentStep != CreationStep.TITLE) {
                    OutlinedButton(
                        onClick = { viewModel.previousStep() }
                    ) {
                        Text(stringResource(R.string.creation_back))
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                if (uiState.currentStep == CreationStep.NOTES) {
                    Button(
                        onClick = { viewModel.saveItem() },
                        enabled = !uiState.isSaving
                    ) {
                        Text(stringResource(R.string.creation_save))
                    }
                } else {
                    Button(
                        onClick = { viewModel.nextStep() },
                        enabled = viewModel.canAdvance()
                    ) {
                        Text(stringResource(R.string.creation_next))
                    }
                }
            }
        }
    }
}
