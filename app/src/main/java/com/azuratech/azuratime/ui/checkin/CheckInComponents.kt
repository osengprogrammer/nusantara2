package com.azuratech.azuratime.ui.checkin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azuratech.azuratime.ui.theme.AzuraShapes
import com.azuratech.azuratime.ui.theme.AzuraSpacing

@Composable
fun StatusLabel(text: String, color: Color) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onPrimary,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier
            .background(color, AzuraShapes.medium)
            .padding(horizontal = AzuraSpacing.md, vertical = AzuraSpacing.sm)
    )
}

@Composable
fun MatchResultLabel(name: String, isAlreadyIn: Boolean, primaryColor: Color) {
    val bgColor = if (isAlreadyIn) MaterialTheme.colorScheme.tertiary else primaryColor
    val status = if (isAlreadyIn) "SUDAH ABSEN" else "BERHASIL"
    
    Column(
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        modifier = Modifier
            .background(bgColor, AzuraShapes.large)
            .padding(horizontal = AzuraSpacing.lg, vertical = AzuraSpacing.md)
    ) {
        Text(
            text = name,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black)
        )
        Text(
            text = status,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
fun HeaderOverlay(
    activeClass: String,
    onFlipCamera: () -> Unit,
    onSwitchToBarcode: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(AzuraSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.7f), // Preserved for camera overlay contrast
            shape = AzuraShapes.medium,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
        ) {
            Column(Modifier.padding(horizontal = AzuraSpacing.md, vertical = AzuraSpacing.sm)) {
                Text(
                    "AZURA TIME: FACE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black
                    )
                )
                val display = if (activeClass.isBlank()) "SCAN BEBAS" else activeClass.uppercase()
                Text(
                    display,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White) // Preserved for camera overlay contrast
                )
            }
        }
        Row {
            FilledIconButton(
                onClick = onFlipCamera,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                )
            ) {
                Icon(Icons.Default.Cameraswitch, contentDescription = "Flip Camera", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(AzuraSpacing.sm))
            FilledIconButton(
                onClick = onSwitchToBarcode,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                )
            ) {
                Icon(Icons.Default.QrCode, contentDescription = "Switch to Barcode", tint = Color.White)
            }
        }
    }
}

@Composable
fun LivenessLabel(text: String) {
    Text(
        text = text,
        color = Color.White,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.5f), AzuraShapes.medium)
            .padding(horizontal = AzuraSpacing.md, vertical = AzuraSpacing.sm)
    )
}
