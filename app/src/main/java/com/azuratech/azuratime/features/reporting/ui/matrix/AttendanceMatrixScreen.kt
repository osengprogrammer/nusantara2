package com.azuratech.azuratime.features.reporting.ui.matrix

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen

@Composable
fun AttendanceMatrixScreen(
    onNavigateBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onCellClick: (String, String, java.time.LocalDate) -> Unit,
    @Suppress("UNUSED_PARAMETER") viewModel: AttendanceMatrixViewModel = hiltViewModel()
) {
    AzuraScreen(
        title = "Matriks Presensi",
        onBack = onNavigateBack
    ) {
        // Implementation placeholder
    }
}
