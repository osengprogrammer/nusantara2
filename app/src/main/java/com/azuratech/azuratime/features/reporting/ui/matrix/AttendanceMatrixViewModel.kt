package com.azuratech.azuratime.features.reporting.ui.matrix

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.attendance.data.local.AttendanceRecordEntity
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceStatus
import com.azuratech.azuratime.features.attendance.domain.repository.AttendanceRepository
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import com.azuratech.azuratime.features.student.domain.model.StudentProfile
import com.azuratech.azuratime.features.student.domain.repository.StudentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * 📊 ATTENDANCE MATRIX VIEW MODEL (v3.2.0-ai-native)
 */
@HiltViewModel
class AttendanceMatrixViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val studentRepository: StudentRepository,
    private val schoolRepository: SchoolRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _startDate = MutableStateFlow(LocalDate.now().withDayOfMonth(1))
    private val _endDate = MutableStateFlow(LocalDate.now())
    private val _searchQuery = MutableStateFlow("")
    private val _selectedClassId = MutableStateFlow("")

    private val _uiStateFlow = MutableStateFlow<AttendanceMatrixUiState>(AttendanceMatrixUiState.Loading)
    val uiStateFlow: StateFlow<AttendanceMatrixUiState> = _uiStateFlow.asStateFlow()

    init {
        onEvent(AttendanceMatrixUiEvent.LoadData)
    }

    fun onEvent(event: AttendanceMatrixUiEvent) {
        when (event) {
            is AttendanceMatrixUiEvent.LoadData -> observeData()
            is AttendanceMatrixUiEvent.FilterByDate -> {
                _startDate.value = event.startDate
                _endDate.value = event.endDate
            }
            is AttendanceMatrixUiEvent.FilterByClass -> {
                _selectedClassId.value = event.classId
            }
            is AttendanceMatrixUiEvent.Search -> {
                _searchQuery.value = event.query
                // 🔥 AI Native: Fast UI Update to prevent cursor jumping
                val currentState = _uiStateFlow.value
                if (currentState is AttendanceMatrixUiState.Success) {
                    _uiStateFlow.value = AttendanceMatrixUiState.Success(
                        currentState.data.copy(searchQuery = event.query),
                    )
                }
            }
            is AttendanceMatrixUiEvent.ExportToCsv -> {
                // Implement export functionality if needed
            }
        }
    }

    private fun observeData() {
        val schoolId = sessionManager.getActiveSchoolId() ?: return

        viewModelScope.launch {
            val filtersFlow = combine(
                _startDate,
                _endDate,
                _searchQuery.debounce(300), // ⚡ AI Native: Debounce heavy search
                _selectedClassId,
            ) { start, end, query, classId ->
                FilterInputs(start, end, query, classId)
            }

            combine(
                studentRepository.getStudentProfiles(),
                schoolRepository.observeClasses(schoolId),
                filtersFlow,
            ) { studentsResult, classesResult, filters ->
                val students = if (studentsResult is Result.Success) studentsResult.data else emptyList()
                val classes = if (classesResult is Result.Success) classesResult.data else emptyList()

                FilterParams(filters.startDate, filters.endDate, filters.query, filters.classId, students, classes)
            }
                .flatMapLatest { params ->
                    val classFilter = params.classId.ifEmpty { null }

                    attendanceRepository.getAttendanceRecords(
                        name = params.query,
                        startDate = params.startDate,
                        endDate = params.endDate,
                        accountId = null,
                        classId = classFilter,
                        assignedIds = emptyList(), // Admin assumes all access, could be filtered
                        schoolId = schoolId,
                    ).map { recordsResult ->
                        val records = if (recordsResult is Result.Success) recordsResult.data else emptyList()
                        val filteredStudents = params.students.filter { student ->
                            val matchesClass = classFilter == null || classFilter in student.classIds
                            val matchesQuery = params.query.isEmpty() || student.name.contains(params.query, ignoreCase = true)
                            matchesClass && matchesQuery
                        }
                        val rows = transformToMatrixRows(records, filteredStudents, params.startDate, params.endDate)

                        val dateRange = generateDateRange(params.startDate, params.endDate)
                        AttendanceMatrixData(
                            rows = rows,
                            availableClasses = params.classes,
                            dateRange = dateRange,
                            searchQuery = params.query,
                            startDate = params.startDate,
                            endDate = params.endDate,
                            selectedClassId = params.classId,
                            policy = "Standard",
                        )
                    }
                }
                .catch { e ->
                    _uiStateFlow.value = AttendanceMatrixUiState.Error(e.message ?: "Unknown error")
                }
                .collect { data ->
                    _uiStateFlow.value = AttendanceMatrixUiState.Success(data)
                }
        }
    }

    private data class FilterInputs(
        val startDate: LocalDate,
        val endDate: LocalDate,
        val query: String,
        val classId: String,
    )

    private data class FilterParams(
        val startDate: LocalDate,
        val endDate: LocalDate,
        val query: String,
        val classId: String,
        val students: List<StudentProfile>,
        val classes: List<com.azuratech.azuraengine.model.ClassModel>,
    )

    private fun generateDateRange(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
        val range = mutableListOf<LocalDate>()
        var current = startDate
        while (!current.isAfter(endDate)) {
            range.add(current)
            current = current.plusDays(1)
        }
        return range
    }

    private fun transformToMatrixRows(
        records: List<AttendanceRecordEntity>,
        students: List<StudentProfile>,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<MatrixRowModel> {
        val dateRange = generateDateRange(startDate, endDate)

        // Group by studentId
        val recordsByStudent = records.groupBy { it.studentId }

        return students.map { student ->
            val studentRecords = recordsByStudent[student.studentId] ?: emptyList()
            // Map records by date (assuming one record per day for matrix)
            val recordsByDate = studentRecords.associateBy { it.attendanceDate }

            var summaryH = 0
            var summaryS = 0
            var summaryI = 0
            var summaryA = 0
            var summaryT = 0

            val cells = dateRange.map { date ->
                val record = recordsByDate[date]
                if (record != null) {
                    val status = AttendanceStatus.fromCode(record.status)
                    when (status) {
                        AttendanceStatus.PRESENT -> summaryH++
                        AttendanceStatus.LATE -> summaryT++
                        AttendanceStatus.SICK -> summaryS++
                        AttendanceStatus.EXCUSED -> summaryI++
                        AttendanceStatus.ABSENT -> summaryA++
                    }
                    val colorData = getColorForStatus(status)
                    MatrixCellModel(
                        text = status.toCode(),
                        textColor = colorData.first,
                        backgroundColor = colorData.second,
                        isPresent = (status == AttendanceStatus.PRESENT || status == AttendanceStatus.LATE),
                    )
                } else {
                    // Empty days, wait for Alpa rule or just Empty?
                    MatrixCellModel(
                        text = "-",
                        textColor = Color.Gray,
                        backgroundColor = Color.Transparent,
                        isPresent = false,
                    )
                }
            }

            MatrixRowModel(
                studentId = student.studentId,
                studentName = student.name,
                studentClass = student.classIds.firstOrNull() ?: "-",
                cells = cells,
                totalHours = "0",
                summaryH = summaryH.toString(),
                summaryT = summaryT.toString(),
                summaryS = summaryS.toString(),
                summaryI = summaryI.toString(),
                summaryA = summaryA.toString(),
                estimatedSalary = "0",
            )
        }.sortedBy { it.studentName }
    }

    private fun getColorForStatus(status: AttendanceStatus): Pair<Color, Color> {
        return when (status) {
            AttendanceStatus.PRESENT -> Pair(Color(0xFF2E7D32), Color(0xFFE8F5E9))
            AttendanceStatus.LATE -> Pair(Color(0xFFF9A825), Color(0xFFFFF9C4))
            AttendanceStatus.SICK -> Pair(Color(0xFF1565C0), Color(0xFFE3F2FD))
            AttendanceStatus.EXCUSED -> Pair(Color(0xFF6A1B9A), Color(0xFFF3E5F5))
            AttendanceStatus.ABSENT -> Pair(Color(0xFFC62828), Color(0xFFFFEBEE))
        }
    }
}

sealed class AttendanceMatrixUiEvent {
    data object LoadData : AttendanceMatrixUiEvent()
    data class FilterByDate(val startDate: LocalDate, val endDate: LocalDate) : AttendanceMatrixUiEvent()
    data class FilterByClass(val classId: String) : AttendanceMatrixUiEvent()
    data class Search(val query: String) : AttendanceMatrixUiEvent()
    data object ExportToCsv : AttendanceMatrixUiEvent()
}
