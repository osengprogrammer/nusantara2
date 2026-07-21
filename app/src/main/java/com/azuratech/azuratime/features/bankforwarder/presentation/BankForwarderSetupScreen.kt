package com.azuratech.azuratime.features.bankforwarder.presentation

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.features.bankforwarder.core.NotificationAccessHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankForwarderSetupScreen(
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    var isNotificationAccessGranted by remember {
        mutableStateOf(NotificationAccessHelper.isNotificationServiceEnabled(context))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bank Notification Forwarder") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Spacer(Modifier.height(32.dp))

            Text(
                text = "Auto Top-Up dari Notifikasi Bank",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Forwarder ini akan mendeteksi notifikasi dari aplikasi bank dan otomatis memproses top-up saldo siswa.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isNotificationAccessGranted)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isNotificationAccessGranted) "✅ Akses Notifikasi Aktif" else "⚠️ Akses Notifikasi Diperlukan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (isNotificationAccessGranted)
                            "Layanan notifikasi bank sudah aktif."
                        else
                            "Berikan izin akses notifikasi agar forwarder dapat bekerja.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isNotificationAccessGranted) "Kelola Pengaturan" else "Buka Pengaturan Notifikasi")
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    isNotificationAccessGranted = NotificationAccessHelper.isNotificationServiceEnabled(context)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Refresh Status")
            }
        }
    }
}
