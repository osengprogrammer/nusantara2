package com.azuratech.azuratime.features.school.ui.classes

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen

@Composable
fun ClassDetailScreen(
    classId: String,
    className: String,
    classViewModel: ViewModel,
    faceViewModel: ViewModel,
    onNavigateBack: () -> Unit,
    onAddStudent: () -> Unit
) {
    AzuraScreen(title = "Detail Kelas: $className", onBack = onNavigateBack) {
        // Placeholder
    }
}
