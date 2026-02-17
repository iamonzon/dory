package com.iamonzon.dory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.tooling.preview.Preview
import com.iamonzon.dory.R
import com.iamonzon.dory.data.model.ReviewUrgency
import com.iamonzon.dory.ui.theme.DoryTheme
import com.iamonzon.dory.ui.theme.doryColors

@Composable
fun UrgencyIndicator(
    urgency: ReviewUrgency,
    modifier: Modifier = Modifier
) {
    val doryColors = MaterialTheme.doryColors
    val (bgColor, icon, contentDescription) = when (urgency) {
        ReviewUrgency.Overdue -> Triple(doryColors.urgencyRed, Icons.Default.Warning, stringResource(R.string.urgency_overdue))
        ReviewUrgency.DueToday -> Triple(doryColors.urgencyYellow, Icons.Default.Info, stringResource(R.string.urgency_due_today))
        ReviewUrgency.NotDue -> Triple(doryColors.urgencyGreen, Icons.Default.Check, stringResource(R.string.urgency_not_due))
    }

    Box(
        modifier = modifier
            .size(32.dp)
            .background(bgColor.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = bgColor,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UrgencyIndicatorPreview() {
    DoryTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UrgencyIndicator(urgency = ReviewUrgency.Overdue)
            UrgencyIndicator(urgency = ReviewUrgency.DueToday)
            UrgencyIndicator(urgency = ReviewUrgency.NotDue)
        }
    }
}
