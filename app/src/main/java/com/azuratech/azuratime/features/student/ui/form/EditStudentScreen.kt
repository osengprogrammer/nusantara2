package com.azuratech.azuratime.features.student.ui.form

import androidx.compose.runtime.Composable
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen

@Composable
fun EditStudentScreen(
    faceId: String,
    onNavigateBack: () -> Unit
) {
    AzuraScreen(title = "Edit Siswa", onBack = onNavigateBack) {
        // Placeholder
    }
}
