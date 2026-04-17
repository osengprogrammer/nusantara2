package com.azuratech.azuratime.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun AzuraDynamicSnackbarHost(hostState: SnackbarHostState) {
    SnackbarHost(hostState = hostState) { data ->
        val message = data.visuals.message
        
        // Menentukan warna dan ikon berdasarkan awalan teks
        val (bgColor, contentColor, icon: ImageVector) = when {
            message.startsWith("✅") -> Triple(
                Color(0xFF2E7D32), // Hijau Sukses
                Color.White,
                Icons.Default.CheckCircle
            )
            message.startsWith("⚠️") -> Triple(
                Color(0xFFF9A825), // Oranye/Kuning Warning
                Color.Black,
                Icons.Default.Warning
            )
            message.startsWith("❌") || message.contains("Gagal", ignoreCase = true) -> Triple(
                Color(0xFFC62828), // Merah Error
                Color.White,
                Icons.Default.Error
            )
            else -> Triple(
                Color(0xFF323232), // Gelap Default
                Color.White,
                Icons.Default.Info
            )
        }

        Snackbar(
            containerColor = bgColor,
            contentColor = contentColor,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(16.dp)
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
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = message.removePrefix("✅ ").removePrefix("⚠️ ").removePrefix("❌ ").trim(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor
                )
            }
        }
    }
}