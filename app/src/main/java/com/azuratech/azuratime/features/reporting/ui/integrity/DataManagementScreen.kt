package com.azuratech.azuratime.features.reporting.ui.integrity

import androidx.compose.runtime.Composable
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.features.student.ui.bulk.RegisterViewModel

@Composable
fun DataManagementScreen(
    @Suppress("UNUSED_PARAMETER") initialDataType: String,
    onNavigateBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onNavigateToClassList: () -> Unit,
    @Suppress("UNUSED_PARAMETER") registerViewModel: RegisterViewModel
) {
    AzuraScreen(title = "Manajemen Data", onBack = onNavigateBack) {
        // Placeholder
    }
}
