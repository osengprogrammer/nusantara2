package com.azuratech.azuratime.features.school.ui.classes

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen

@Composable
fun ClassDetailScreen(
    @Suppress("UNUSED_PARAMETER") classId: String,
    className: String,
    @Suppress("UNUSED_PARAMETER") classViewModel: ViewModel,
    @Suppress("UNUSED_PARAMETER") biometricViewModel: ViewModel,
    onNavigateBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onAddStudent: () -> Unit
) {
    AzuraScreen(title = "Detail Kelas: $className", onBack = onNavigateBack) {
        // Placeholder
    }
}
