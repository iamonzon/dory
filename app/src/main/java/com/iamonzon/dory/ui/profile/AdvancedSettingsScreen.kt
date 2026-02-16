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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.iamonzon.dory.algorithm.FsrsParameters
import com.iamonzon.dory.ui.theme.DoryTheme
import com.iamonzon.dory.ui.components.DoryTopAppBar

@Composable
fun AdvancedSettingsScreen(
    onBackClick: () -> Unit
) {
    var desiredRetention by remember {
        mutableFloatStateOf(FsrsParameters.DEFAULT_DESIRED_RETENTION.toFloat())
    }
    val defaultWeights = remember { FsrsParameters.DEFAULT_WEIGHTS }

    Scaffold(
        topBar = {
            DoryTopAppBar(
                title = "Advanced Settings",
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
            Text(text = "Desired Retention", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Target probability of recalling an item at review time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "%.0f%%".format(desiredRetention * 100),
                style = MaterialTheme.typography.titleLarge
            )

            Slider(
                value = desiredRetention,
                onValueChange = { desiredRetention = it },
                valueRange = 0.70f..0.97f,
                steps = 26
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("70%", style = MaterialTheme.typography.labelMedium)
                Text("97%", style = MaterialTheme.typography.labelMedium)
            }

            HorizontalDivider()

            Text(text = "FSRS Parameters (Default)", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "These weights are from the published FSRS-4.5 defaults. They will be personalized as you review items.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            defaultWeights.forEachIndexed { index, weight ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "w$index",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "%.4f".format(weight),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AdvancedSettingsScreenPreview() {
    DoryTheme {
        AdvancedSettingsScreen(onBackClick = {})
    }
}
