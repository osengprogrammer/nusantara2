package com.azuratech.azuratime.features.reporting.ui.matrix

import androidx.compose.ui.graphics.Color
import com.azuratech.azuraengine.model.ClassModel
import java.time.LocalDate

/**
 * 📊 ATTENDANCE MATRIX UI STATE
 */
sealed class AttendanceMatrixUiState {
    object Loading : AttendanceMatrixUiState()
    data class Success(val data: AttendanceMatrixData) : AttendanceMatrixUiState()
    data class Error(val message: String) : AttendanceMatrixUiState()
}

data class AttendanceMatrixData(
    val rows: List<MatrixRowModel>,
    val availableClasses: List<ClassModel>,
    val dateRange: List<LocalDate>,
    val searchQuery: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val selectedClassId: String,
    val policy: String,
)

data class MatrixRowModel(
    val studentId: String,
    val studentName: String,
    val studentClass: String,
    val cells: List<MatrixCellModel>,
    val totalHours: String,
    val summaryH: String,
    val summaryT: String,
    val summaryS: String,
    val summaryI: String,
    val summaryA: String,
    val estimatedSalary: String,
)

data class MatrixCellModel(
    val text: String,
    val textColor: Color,
    val backgroundColor: Color,
    val isPresent: Boolean,
)
