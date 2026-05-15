package com.azuratech.azuratime.features.reporting.ui.integrity

import androidx.compose.runtime.Composable
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.features.student.ui.bulk.RegisterViewModel

@Composable
fun DataManagementScreen(
    initialDataType: String,
    onNavigateBack: () -> Unit,
    onNavigateToClassList: () -> Unit,
    registerViewModel: RegisterViewModel
) {
    AzuraScreen(title = "Manajemen Data", onBack = onNavigateBack) {
        // Placeholder
    }
}
