package com.azuratech.azuratime.features.reporting.ui.matrix

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.features.reporting.ui.components.MatrixTableView

@Composable
fun AttendanceMatrixScreen(
    onNavigateBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onCellClick: (String, String, java.time.LocalDate) -> Unit,
    viewModel: AttendanceMatrixViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()

    AzuraScreen(
        title = "Matriks Presensi",
        onBack = onNavigateBack,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = AzuraSpacing.md),
        ) {
            when (val state = uiState) {
                is AttendanceMatrixUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is AttendanceMatrixUiState.Success -> {
                    // TODO: Add filters here (Date Picker, Class Dropdown)

                    Box(modifier = Modifier.weight(1f)) {
                        MatrixTableView(data = state.data)
                    }
                }
                is AttendanceMatrixUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
