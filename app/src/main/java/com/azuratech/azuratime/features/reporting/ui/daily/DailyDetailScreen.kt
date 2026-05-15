package com.azuratech.azuratime.features.reporting.ui.daily

import androidx.compose.runtime.Composable
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen

@Composable
fun DailyDetailScreen(
    faceId: String,
    studentName: String,
    dateString: String,
    onNavigateBack: () -> Unit,
    onNavigateToManual: (String, String) -> Unit
) {
    AzuraScreen(
        title = "Detail Harian",
        onBack = onNavigateBack
    ) {
        // Implementation placeholder
    }
}
