package com.azuratech.azuratime.features.session.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.features.session.domain.model.SessionType

/**
 * 🏷️ SHARED TIER BADGE
 * Consistent visual representation of session hierarchy across the app.
 */
@Composable
fun TierBadge(type: SessionType, modifier: Modifier = Modifier) {
    val (color, label) = when (type) {
        SessionType.ACADEMIC -> MaterialTheme.colorScheme.primary to "Academic"
        SessionType.CLASS_WIDE -> MaterialTheme.colorScheme.secondary to "Class"
        SessionType.GLOBAL -> MaterialTheme.colorScheme.tertiary to "Global"
    }
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.1f),
        shape = CircleShape,
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
        )
    }
}

fun getDayName(day: Int): String = when (day) {
    1 -> "Monday"
    2 -> "Tuesday"
    3 -> "Wednesday"
    4 -> "Thursday"
    5 -> "Friday"
    6 -> "Saturday"
    7 -> "Sunday"
    else -> "Unknown"
}
