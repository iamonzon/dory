package com.iamonzon.dory.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iamonzon.dory.R
import com.iamonzon.dory.ui.components.DoryTopAppBar

@Composable
fun AdvancedSettingsScreen(
    viewModel: AdvancedSettingsViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            DoryTopAppBar(
                title = stringResource(R.string.advanced_title),
                onBackClick = onBackClick
            )
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
            Text(text = stringResource(R.string.advanced_desired_retention), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.advanced_retention_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = stringResource(R.string.advanced_retention_format, uiState.desiredRetention.toFloat() * 100),
                style = MaterialTheme.typography.titleLarge
            )

            Slider(
                value = uiState.desiredRetention.toFloat(),
                onValueChange = { viewModel.setDesiredRetention(it.toDouble()) },
                valueRange = 0.70f..0.97f,
                steps = 26
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.advanced_retention_min), style = MaterialTheme.typography.labelMedium)
                Text(stringResource(R.string.advanced_retention_max), style = MaterialTheme.typography.labelMedium)
            }

            HorizontalDivider()

            Text(text = stringResource(R.string.advanced_fsrs_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.advanced_fsrs_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            uiState.fsrsWeights.forEachIndexed { index, weight ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.advanced_weight_label, index),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(R.string.advanced_weight_format, weight),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
