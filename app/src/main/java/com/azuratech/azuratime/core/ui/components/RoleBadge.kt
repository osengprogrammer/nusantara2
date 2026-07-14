package com.azuratech.azuratime.core.ui.components

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
import com.azuratech.azuratime.core.domain.model.AccountRole

/**
 * 🏷️ SHARED ROLE BADGE
 * Consistent visual representation of account roles across the app.
 */
@Composable
fun RoleBadge(roleStr: String, modifier: Modifier = Modifier) {
    val role = try { AccountRole.valueOf(roleStr.uppercase()) } catch (e: Exception) { AccountRole.USER }
    val (color, label) = when (role) {
        AccountRole.SUPER_ADMIN -> MaterialTheme.colorScheme.error to "Super Admin"
        AccountRole.ADMIN -> MaterialTheme.colorScheme.primary to "Admin"
        AccountRole.SUPERVISOR -> MaterialTheme.colorScheme.secondary to "Supervisor"
        AccountRole.USER -> MaterialTheme.colorScheme.outline to "Member"
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
