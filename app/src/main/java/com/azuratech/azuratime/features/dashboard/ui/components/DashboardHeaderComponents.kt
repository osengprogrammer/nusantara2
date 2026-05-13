package com.azuratech.azuratime.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.azuratech.azuratime.ui.theme.AzuraSpacing
import com.azuratech.azuratime.ui.theme.AzuraShapes

/**
 * 🔄 CLOUD SYNC BUTTON
 * Pulsing or showing progress during the background sync flow.
 */
@Composable
fun DashboardSyncButton(isSyncing: Boolean, onSyncClick: () -> Unit) {
    FilledTonalIconButton(
        onClick = onSyncClick,
        enabled = !isSyncing,
        shape = CircleShape,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        if (isSyncing) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp), 
                strokeWidth = 2.5.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Icon(
                Icons.Default.CloudSync, 
                contentDescription = "Force Sync Cloud", 
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * 👤 PROFILE HEADER
 * Displaying user identity and the active school workspace.
 */
@Composable
fun ProfileHeader(
    name: String,
    email: String?,
    schoolName: String?, 
    photoUrl: Any?,
    onLogout: () -> Unit,
    onProfileClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AzuraSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = photoUrl,
            contentDescription = "Profile Photo",
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onProfileClick() }
        )
        
        Spacer(modifier = Modifier.width(AzuraSpacing.md))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name, 
                style = MaterialTheme.typography.titleLarge, 
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!email.isNullOrBlank()) {
                Text(
                    text = email,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            if (!schoolName.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = AzuraShapes.small,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = schoolName, 
                        style = MaterialTheme.typography.labelSmall, 
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        IconButton(
            onClick = onLogout,
            modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, null, tint = MaterialTheme.colorScheme.error)
        }
    }
}

/**
 * ⚠️ DATA INTEGRITY ALERT
 * Prompts the admin to assign classes to new student enrollments.
 */
@Composable
fun UnassignedAlertButton(count: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = AzuraShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error)
    ) {
        Row(
            modifier = Modifier.padding(AzuraSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "Perlu Tindakan!", 
                    style = MaterialTheme.typography.labelLarge, 
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "$count Siswa belum masuk ke kelas manapun.", 
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White)
        }
    }
}