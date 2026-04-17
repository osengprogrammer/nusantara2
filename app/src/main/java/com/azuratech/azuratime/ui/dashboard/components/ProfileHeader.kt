package com.azuratech.azuratime.ui.dashboard.components

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.azuratech.azuratime.ui.theme.AzuraSpacing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout


@Composable
fun ProfileHeader(
    name: String,
    email: String?,
    schoolName: String?,
    photoUrl: Uri?,
    onLogout: () -> Unit,
    onProfileClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AzuraSpacing.md)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AzuraSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "User Profile Photo",
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .clickable { onProfileClick() },
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(AzuraSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = email ?: "", style = MaterialTheme.typography.bodyMedium)
                Text(text = schoolName ?: "", style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onLogout) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.Logout,
                    contentDescription = "Logout"
                )
            }
        }
    }
}
