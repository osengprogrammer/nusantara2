package com.azuratech.azuratime.features.student.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.core.ui.designsystem.AzuraCard
import com.azuratech.azuratime.core.ui.designsystem.StudentAvatar
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.core.domain.model.SyncStatus

@Composable
fun StudentRosterItem(
    item: StudentDisplayItem,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AzuraCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(AzuraSpacing.md)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StudentAvatar(
                photoUrl = item.profile.photoUrl,
                size = 50.dp,
            )

            Spacer(modifier = Modifier.width(AzuraSpacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.profile.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = item.assignedClassNames,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.xs),
                ) {
                    if (item.isBiometricReady) {
                        Icon(
                            Icons.Default.Face,
                            contentDescription = "Face Ready",
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFF2E7D32),
                        )
                        Text(
                            "Biometric Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2E7D32),
                        )
                    } else {
                        Text(
                            "Biometric Missing",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    if (item.profile.syncStatus != SyncStatus.SYNCED) {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = "Pending Sync",
                            modifier = Modifier.size(14.dp),
                            tint = Color.Gray,
                        )
                    }
                }
            }

            Row {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
