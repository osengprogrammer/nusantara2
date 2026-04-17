package com.azuratech.azuratime.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// 🔥 Azura Design System Imports
import com.azuratech.azuratime.ui.core.designsystem.AzuraScreen
import com.azuratech.azuratime.ui.theme.AzuraSpacing
import com.azuratech.azuratime.ui.theme.AzuraShapes
import com.azuratech.azuratime.ui.theme.AzuraPrimaryDark

@Composable
fun RegistrationMenuScreen(
    onNavigateToAddUser: () -> Unit,
    onNavigateToBulkRegister: () -> Unit,
    onNavigateBack: () -> Unit
) {
    AzuraScreen(
        title = "Pendaftaran",
        onBack = onNavigateBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = AzuraSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = null,
                modifier = Modifier.size(64.dp).alpha(0.1f),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(AzuraSpacing.md))

            Text(
                text = "Pilih metode pendaftaran siswa",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(AzuraSpacing.xl))

            RegistrationCard(
                title = "Tambah Siswa Baru",
                description = "Daftarkan satu siswa lengkap dengan pemindaian wajah biometrik.",
                icon = Icons.Default.PersonAdd,
                gradient = Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primary, AzuraPrimaryDark)
                ),
                onClick = onNavigateToAddUser
            )

            Spacer(modifier = Modifier.height(AzuraSpacing.lg))

            RegistrationCard(
                title = "Impor Massal (CSV)",
                description = "Proses ratusan data siswa sekaligus menggunakan file template Excel/CSV.",
                icon = Icons.Default.Group,
                gradient = Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.secondary, Color(0xFF5C6BC0))
                ),
                onClick = onNavigateToBulkRegister
            )

            Spacer(modifier = Modifier.height(AzuraSpacing.xl))

            Text(
                text = "Pastikan pencahayaan cukup saat melakukan pendaftaran wajah manual.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = AzuraSpacing.xl)
            )
        }
    }
}

@Composable
private fun RegistrationCard(
    title: String,
    description: String,
    icon: ImageVector,
    gradient: Brush,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp), // Intentional fixed height for hero cards
        shape = AzuraShapes.large, // 🔥 System Shapes
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(AzuraSpacing.lg) // 🔥 System Spacing
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null, // Decorative icon
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.width(AzuraSpacing.md)) // 🔥 System Spacing

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(AzuraSpacing.xs)) // 🔥 System Spacing

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}