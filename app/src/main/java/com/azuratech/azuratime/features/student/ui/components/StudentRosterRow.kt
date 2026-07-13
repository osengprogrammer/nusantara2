package com.azuratech.azuratime.features.student.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.core.ui.theme.AzuraShapes
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing

@Composable
fun StudentRosterRow(
    studentId: String,
    name: String,
    code: String?,
    classNames: String,
    balance: String,
    @Suppress("UNUSED_PARAMETER") isBiometricReady: Boolean,
    onClick: () -> Unit,
    onHistoryClick: (String) -> Unit,
    onDeductClick: (String) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AzuraSpacing.md, vertical = 4.dp)
            .clickable { onClick() },
        shape = AzuraShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .padding(AzuraSpacing.md)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Left Side: Student Info
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, style = MaterialTheme.typography.titleMedium)
                code?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
                Text(
                    text = classNames,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Right Side: Balance & Action Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = balance,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = { onHistoryClick(studentId) }) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "View History",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                IconButton(onClick = { onDeductClick(studentId) }) {
                    Icon(
                        imageVector = Icons.Default.RemoveCircle,
                        contentDescription = "Deduct Balance",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
