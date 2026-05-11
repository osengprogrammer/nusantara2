package com.azuratech.azuratime.ui.biometric

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.domain.model.BiometricEnrollmentProfile
import com.azuratech.azuratime.domain.model.SyncStatus
import com.azuratech.azuratime.ui.core.designsystem.AzuraCard
import com.azuratech.azuratime.ui.core.designsystem.AzuraScreen
import com.azuratech.azuratime.ui.core.designsystem.AzuraTextField
import com.azuratech.azuratime.ui.core.designsystem.StudentAvatar
import com.azuratech.azuratime.ui.theme.AzuraSpacing
import com.azuratech.azuratime.ui.core.preview.AzuraPreviews

@Composable
fun BiometricScreen(
    viewModel: BiometricViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val enrollmentList by viewModel.enrollmentList.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    AzuraScreen(
        title = "Manajemen Biometrik",
        onBack = onNavigateBack
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = AzuraSpacing.md)) {
            // Student Selector / Search
            AzuraTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                label = "Cari Siswa...",
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(vertical = AzuraSpacing.sm)
            )

            if (enrollmentList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada data biometrik.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(enrollmentList, key = { it.faceId }) { profile ->
                        BiometricEnrollmentItem(
                            profile = profile,
                            onDelete = { viewModel.deleteEnrollment(profile.faceId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BiometricEnrollmentItem(
    profile: BiometricEnrollmentProfile,
    onDelete: () -> Unit
) {
    AzuraCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(AzuraSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StudentAvatar(photoPath = profile.photoUri, size = 56)
            Spacer(modifier = Modifier.width(AzuraSpacing.md))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(profile.studentName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "ID: ${profile.studentId ?: "Umum"}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Sync Indicator
            if (profile.syncStatus != SyncStatus.SYNCED) {
                Icon(
                    Icons.Default.CloudOff,
                    contentDescription = "Belum Sinkron",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(AzuraSpacing.sm))
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Hapus", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@AzuraPreviews
@Composable
fun BiometricScreenPreview() {
    MaterialTheme {
        BiometricScreen(onNavigateBack = {})
    }
}
