package com.azuratech.azuratime.ui.attendance

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.domain.model.AttendanceProfile
import com.azuratech.azuratime.domain.model.SyncStatus
import com.azuratech.azuratime.ui.core.designsystem.AzuraScreen
import com.azuratech.azuratime.ui.report.AttendanceMatrixContent
import com.azuratech.azuratime.ui.report.AttendanceMatrixData
import com.azuratech.azuratime.ui.report.AttendanceMatrixUiState
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceMatrixScreen(
    viewModel: AttendanceMatrixViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onCellClick: (String, String, LocalDate) -> Unit
) {
    val attendanceMatrix by viewModel.attendanceMatrixStateFlow.collectAsStateWithLifecycle()
    val uiState by viewModel.uiStateStateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current

    AzuraScreen(
        title = "Rekap Kehadiran",
        onBack = onBack
    ) {
        // Render Matrix using AttendanceProfile stream
        AttendanceMatrixContent(
            data = (uiState as? AttendanceMatrixUiState.Success)?.data ?: AttendanceMatrixData(),
            onSearchChange = { viewModel.onSearchQueryChanged(it) },
            onDateRangeSelected = { start, end -> viewModel.onDateRangeSelected(start, end) },
            onClassSelected = { viewModel.onClassSelected(it) },
            onPolicySelected = { viewModel.onPolicySelected(it) },
            onTabSelected = { viewModel.onTabSelected(it) },
            onBack = onBack,
            onCellClick = onCellClick,
            onExportClick = { viewModel.exportReport() }
        )
        
        // Sync Indicators
        attendanceMatrix.forEach { profile ->
            if (profile.syncStatus != SyncStatus.SYNCED) {
                Icon(Icons.Default.CloudOff, contentDescription = null, tint = Color.Gray)
            }
        }
    }
}
