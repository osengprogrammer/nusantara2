package com.azuratech.azuratime.features.biometric.ui.enroll

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.designsystem.AzuraCard
import com.azuratech.azuratime.core.ui.designsystem.StudentAvatar
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.features.biometric.domain.model.BiometricEnrollmentProfile

@Composable
fun BiometricScreen(
    onNavigateBack: () -> Unit,
    viewModel: BiometricViewModel = hiltViewModel()
) {
    val enrollmentList by viewModel.enrollmentList.collectAsStateWithLifecycle()

    AzuraScreen(
        title = "Manajemen Biometrik",
        onBack = onNavigateBack
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)
        ) {
            items(enrollmentList) { profile ->
                BiometricEnrollmentItem(
                    profile = profile,
                    onDelete = { viewModel.deleteEnrollment(profile.studentId) }
                )
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
            StudentAvatar(photoPath = profile.photoUri, size = 56.dp)
            Spacer(modifier = Modifier.width(AzuraSpacing.md))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(profile.studentName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "ID: ${profile.studentId}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            IconButton(onClick = onDelete) {
                // Delete icon placeholder
            }
        }
    }
}
