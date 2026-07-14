package com.azuratech.azuratime.features.reporting.ui.integrity

import androidx.compose.runtime.Composable
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen

@Composable
fun DataManagementScreen(
    @Suppress("UNUSED_PARAMETER") initialDataType: String,
    onNavigateBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onNavigateToClassList: () -> Unit,
    
) {
    AzuraScreen(title = "Manajemen Data", onBack = onNavigateBack) {
        // Placeholder
    }
}
