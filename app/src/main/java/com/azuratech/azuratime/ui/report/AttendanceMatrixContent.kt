package com.azuratech.azuratime.ui.report

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.ui.core.designsystem.AzuraScreen
import com.azuratech.azuratime.ui.core.preview.AzuraPreviews
import com.azuratech.azuratime.ui.core.preview.PreviewMocks
import com.azuratech.azuratime.ui.report.components.MatrixTableView
import com.azuratech.azuratime.ui.report.components.ReportFilterSection
import com.azuratech.azuratime.ui.report.components.ReportTabSection
import com.azuratech.azuratime.ui.theme.AzuraTheme
import java.time.LocalDate


@Composable
fun AttendanceMatrixContent(
    data: AttendanceMatrixData,
    onSearchChanged: (String) -> Unit,
    onDateRangeSelected: (LocalDate, LocalDate) -> Unit,
    onClassSelected: (String?) -> Unit,
    onPolicySelected: (String) -> Unit,
    onTabSelected: (Int) -> Unit,
    onBack: () -> Unit,
    onCellClick: (String, String, LocalDate) -> Unit,
    onExportClick: () -> Unit
) {
    AzuraScreen(
        title = "Rekap Kehadiran",
        onBack = onBack,
        actions = {
            if (data.isExporting) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(onClick = onExportClick) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Export Report"
                    )
                }
            }
        },
        content = {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top
            ) {
                ReportFilterSection(
                    startDate = data.startDate,
                    endDate = data.endDate,
                    searchQuery = data.searchQuery,
                    selectedClassId = data.selectedClassId,
                    availableClasses = data.availableClasses,
                    onSearchChange = onSearchChanged,
                    onDateRangeSelected = onDateRangeSelected,
                    onClassSelected = onClassSelected
                )

                ReportTabSection(
                    selectedTabIndex = data.selectedTabIndex,
                    historyCount = 0,
                    currentPolicy = data.policy,
                    onTabSelected = onTabSelected,
                    onPolicySelected = onPolicySelected
                )

                HorizontalDivider()

                Box(modifier = Modifier.weight(1f)) {
                    if (data.rows.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Tidak ada data untuk ditampilkan.")
                        }
                    } else {
                        MatrixTableView(
                            rows = data.rows,
                            dateRange = data.dateRange,
                            onCellClick = onCellClick
                        )
                    }
                }
            }
        }
    )
}


@AzuraPreviews
@Composable
fun AttendanceMatrixContentSuccessPreview() {
    AzuraTheme {
        Surface {
            AttendanceMatrixContent(
                data = PreviewMocks.mockMatrixData,
                onSearchChanged = {},
                onDateRangeSelected = { _, _ -> },
                onClassSelected = {},
                onPolicySelected = {},
                onTabSelected = {},
                onBack = {},
                onCellClick = { _, _, _ -> },
                onExportClick = {}
            )
        }
    }
}

@AzuraPreviews
@Composable
fun AttendanceMatrixContentLoadingPreview() {
    AzuraTheme {
        Surface {
            AttendanceMatrixContent(
                data = AttendanceMatrixData(),
                onSearchChanged = {},
                onDateRangeSelected = { _, _ -> },
                onClassSelected = {},
                onPolicySelected = {},
                onTabSelected = {},
                onBack = {},
                onCellClick = { _, _, _ -> },
                onExportClick = {}
            )
        }
    }
}
