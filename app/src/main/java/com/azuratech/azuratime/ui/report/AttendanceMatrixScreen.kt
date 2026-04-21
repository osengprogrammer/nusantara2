package com.azuratech.azuratime.ui.report

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.ui.core.designsystem.AzuraScreen
import com.azuratech.azuratime.ui.report.components.MatrixTableView
import com.azuratech.azuratime.ui.report.components.ReportFilterSection
import com.azuratech.azuratime.ui.report.components.ReportTabSection
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceMatrixScreen(
    viewModel: AttendanceMatrixViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onCellClick: (String, String, LocalDate) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState) {
        if (uiState is AttendanceMatrixUiState.Success) {
            val file = (uiState as AttendanceMatrixUiState.Success).data.exportedFile
            if (file != null) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Report"))
                viewModel.onExportHandled()
            }
        }
    }

    when (val state = uiState) {
        is AttendanceMatrixUiState.Loading -> {
            AzuraScreen(title = "Rekap Kehadiran") {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
        is AttendanceMatrixUiState.Success -> {
            AttendanceMatrixContent(
                data = state.data,
                onSearchChange = { viewModel.onSearchQueryChanged(it) },
                onDateRangeSelected = { start, end -> viewModel.onDateRangeSelected(start, end) },
                onClassSelected = { viewModel.onClassSelected(it) },
                onPolicySelected = { viewModel.onPolicySelected(it) },
                onCellClick = onCellClick,
                onExportClick = { viewModel.exportReport(context) }
            )
        }
        is AttendanceMatrixUiState.Error -> {
            AzuraScreen(title = "Rekap Kehadiran") {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
