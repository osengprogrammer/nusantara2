package com.azuratech.azuratime.ui.report

import androidx.compose.ui.graphics.Color
import com.azuratech.azuratime.data.local.CheckInRecordEntity
import com.azuratech.azuraengine.model.ClassModel
import java.time.LocalDate

/**
 * A display-ready model for a single cell in the attendance grid.
 * It contains all pre-calculated and pre-formatted data.
 * The UI just needs to render these properties.
 */
data class MatrixCellModel(
    val text: String,
    val textColor: Color,
    val bgColor: Color,
    val isBold: Boolean = false
)

/**
 * A display-ready model for an entire row in the attendance grid.
 */
data class MatrixRowModel(
    val studentId: String,
    val studentName: String,
    val studentClass: String,
    val cells: List<MatrixCellModel>, // Daily cells
    val totalHours: String,
    val summaryH: String,
    val summaryS: String,
    val summaryI: String,
    val summaryA: String,
    val estimatedSalary: String
)

data class DailyAttendance(
    val status: String,
    val logs: List<CheckInRecordEntity>
)

data class AttendanceMatrixData(
    val rows: List<MatrixRowModel> = emptyList(),
    val dateRange: List<LocalDate> = emptyList(),
    val availableClasses: List<ClassModel> = emptyList(),
    val searchQuery: String = "",
    val startDate: LocalDate = LocalDate.now().withDayOfMonth(1),
    val endDate: LocalDate = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()),
    val selectedClassId: String? = "ALL",
    val policy: String = "SCHOOL",
    val isExporting: Boolean = false,
    val exportedFile: String? = null
)

sealed class AttendanceMatrixUiState {
    object Loading : AttendanceMatrixUiState()
    data class Success(val data: AttendanceMatrixData) : AttendanceMatrixUiState()
    data class Error(val message: String) : AttendanceMatrixUiState()
}

data class ReportData(
    val rows: List<MatrixRowModel> = emptyList(),
    val dateRange: List<LocalDate> = emptyList(),
    val availableClasses: List<ClassModel> = emptyList(),
    val searchQuery: String = "",
    val startDate: LocalDate = LocalDate.now().withDayOfMonth(1),
    val endDate: LocalDate = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()),
    val selectedClassId: String? = "ALL",
    val policy: String = "SCHOOL"
)

sealed class ReportUiState {
    object Loading : ReportUiState()
    data class Success(val data: ReportData) : ReportUiState()
    data class Error(val message: String) : ReportUiState()
}

data class MatrixParams(
    val start: LocalDate,
    val end: LocalDate,
    val classId: String?,
    val role: String,
    val assigned: List<String>,
    val schoolId: String
)
