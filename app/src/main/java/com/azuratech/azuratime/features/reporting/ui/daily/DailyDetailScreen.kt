package com.azuratech.azuratime.features.reporting.ui.daily

import androidx.compose.runtime.Composable
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen

@Composable
fun DailyDetailScreen(
    @Suppress("UNUSED_PARAMETER") faceId: String,
    @Suppress("UNUSED_PARAMETER") studentName: String,
    @Suppress("UNUSED_PARAMETER") dateString: String,
    onNavigateBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onNavigateToManual: (String, String) -> Unit
) {
    AzuraScreen(
        title = "Detail Harian",
        onBack = onNavigateBack
    ) {
        // Implementation placeholder
    }
}
