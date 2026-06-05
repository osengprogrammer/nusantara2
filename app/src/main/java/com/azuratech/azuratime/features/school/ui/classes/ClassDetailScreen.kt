package com.azuratech.azuratime.features.school.ui.classes

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen

import com.azuratech.azuratime.features.biometric.ui.enroll.BiometricEnrollmentViewModel

@Composable
fun ClassDetailScreen(
    @Suppress("UNUSED_PARAMETER") classId: String,
    className: String,
    @Suppress("UNUSED_PARAMETER") classViewModel: ViewModel,
    @Suppress("UNUSED_PARAMETER") biometricViewModel: BiometricEnrollmentViewModel,
    onNavigateBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onAddStudent: () -> Unit,
) {
    AzuraScreen(title = "Class Detail: $className", onBack = onNavigateBack) {
        // Placeholder
    }
}
