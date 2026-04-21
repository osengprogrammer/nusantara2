package com.azuratech.azuratime.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.ui.theme.AzuraShapes
import com.azuratech.azuratime.ui.theme.AzuraSpacing

@Composable
fun AzuraDynamicSnackbarHost(hostState: SnackbarHostState) {
    SnackbarHost(hostState = hostState) { data ->
        val message = data.visuals.message
        
        // Menentukan warna dan ikon berdasarkan awalan teks
        val bgColor: androidx.compose.ui.graphics.Color
        val contentColor: androidx.compose.ui.graphics.Color
        val icon: ImageVector
        
        when {
            message.startsWith("✅") -> {
                bgColor = MaterialTheme.colorScheme.primary
                contentColor = MaterialTheme.colorScheme.onPrimary
                icon = Icons.Default.CheckCircle
            }
            message.startsWith("⚠️") -> {
                bgColor = MaterialTheme.colorScheme.tertiary
                contentColor = MaterialTheme.colorScheme.onTertiary
                icon = Icons.Default.Warning
            }
            message.startsWith("❌") || message.contains("Gagal", ignoreCase = true) -> {
                bgColor = MaterialTheme.colorScheme.error
                contentColor = MaterialTheme.colorScheme.onError
                icon = Icons.Default.Error
            }
            else -> {
                bgColor = MaterialTheme.colorScheme.inverseSurface
                contentColor = MaterialTheme.colorScheme.inverseOnSurface
                icon = Icons.Default.Info
            }
        }

        Snackbar(
            containerColor = bgColor,
            contentColor = contentColor,
            shape = AzuraShapes.medium,
            modifier = Modifier.padding(AzuraSpacing.md)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(AzuraSpacing.sm))
                Text(
                    text = message.removePrefix("✅ ").removePrefix("⚠️ ").removePrefix("❌ ").trim(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor
                )
            }
        }
    }
}