package com.azuratech.azuratime.ui.report

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.data.local.CheckInRecordEntity
import com.azuratech.azuratime.data.local.FaceEntity
import com.azuratech.azuratime.data.repo.ReportRepository
import com.azuratech.azuratime.data.repo.UserRepository
import com.azuratech.azuratime.domain.report.usecase.GetReportDataUseCase
import com.azuratech.azuratime.domain.sync.ExportUtils
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuraengine.model.ClassModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class AttendanceMatrixViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val getReportDataUseCase: GetReportDataUseCase,
    private val userRepository: UserRepository,
    private val exportUtils: ExportUtils,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _startDate = MutableStateFlow(LocalDate.now().withDayOfMonth(1))
    private val _endDate = MutableStateFlow(LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()))
    private val _selectedClassId = MutableStateFlow<String?>("ALL")
    private val _policy = MutableStateFlow("SCHOOL")

    // In a real app, this would come from a session manager
    private val _userRole = MutableStateFlow("ADMIN")
    private val _assignedClasses = MutableStateFlow<List<String>>(emptyList())

    @Suppress("UNCHECKED_CAST")
    private val _matrixRows = combine(
        _startDate, _endDate, _selectedClassId, _userRole, _assignedClasses, _policy, sessionManager.activeSchoolIdFlow
    ) { args: Array<*> ->
        val start = args[0] as LocalDate
        val end = args[1] as LocalDate
        val classId = args[2] as String?
        val role = args[3] as String
        val assigned = args[4] as List<String>
        val policy = args[5] as String
        val schoolId = args[6] as String? ?: ""
        Triple(MatrixParams(start, end, classId, role, assigned, schoolId), policy, generateDateRange(start, end))
    }
    .debounce(200)
    .flatMapLatest { (params, policy, dateRange) ->
        combine(
            getReportDataUseCase.getStudentsInReport(params.schoolId, params.role, params.classId, params.assigned),
            reportRepository.getCheckInRecordDao().getFilteredRecords(
                schoolId = params.schoolId,
                startDate = params.start,
                endDate = params.end,
                classId = params.classId,
                assignedIds = params.assigned
            ),
            getReportDataUseCase.getAvailableClasses(params.schoolId, params.role, params.assigned)
        ) { results: Array<*> ->
            val students = results[0] as List<FaceEntity>
            val logs = results[1] as List<CheckInRecordEntity>
            val classes = results[2] as List<ClassModel>
            buildMatrix(students, logs, dateRange, policy, classes.associate { it.id to it.name })
        }
    }
    .flowOn(Dispatchers.Default)

    private val _filteredRows = _searchQuery
        .debounce(300)
        .combine(_matrixRows) { query, rows ->
            if (query.isBlank()) {
                rows
            } else {
                rows.filter { it.studentName.contains(query, ignoreCase = true) }
            }
        }

    private val availableClasses: StateFlow<List<ClassModel>> = combine(
        _userRole, _assignedClasses, sessionManager.activeSchoolIdFlow
    ) { role, assigned, schoolId ->
        Triple(role, assigned, schoolId ?: "")
    }.flatMapLatest { (role, assigned, schoolId) ->
        getReportDataUseCase.getAvailableClasses(schoolId, role, assigned)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isExporting = MutableStateFlow(false)
    private val _exportedFile = MutableStateFlow<String?>(null)

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<AttendanceMatrixUiState> = combine(
        _filteredRows,
        availableClasses,
        _searchQuery,
        _startDate,
        _endDate,
        _selectedClassId,
        _policy,
        _isExporting,
        _exportedFile
    ) { args: Array<*> ->
        val rows = args[0] as List<MatrixRowModel>
        val classes = args[1] as List<ClassModel>
        val query = args[2] as String
        val start = args[3] as LocalDate
        val end = args[4] as LocalDate
        val classId = args[5] as String?
        val policy = args[6] as String
        val isExporting = args[7] as Boolean
        val exportedFile = args[8] as String?
        
        AttendanceMatrixUiState.Success(
            AttendanceMatrixData(
                rows = rows,
                availableClasses = classes,
                dateRange = generateDateRange(start, end),
                searchQuery = query,
                startDate = start,
                endDate = end,
                selectedClassId = classId,
                policy = policy,
                isExporting = isExporting,
                exportedFile = exportedFile
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AttendanceMatrixUiState.Loading
    )

    fun exportReport() {
        viewModelScope.launch {
            val currentState = uiState.value
            if (currentState !is AttendanceMatrixUiState.Success) return@launch

            _isExporting.value = true
            _exportedFile.value = null
            
            val data = currentState.data
            val className = data.availableClasses.find { it.id == data.selectedClassId }?.name ?: "All Classes"
            
            val filePath = exportUtils.exportMatrixToCsv(
                rows = data.rows,
                dateRange = data.dateRange,
                className = className
            )
            
            _exportedFile.value = filePath
            _isExporting.value = false
        }
    }
    
    fun onExportHandled() {
        _exportedFile.value = null
    }

    private suspend fun buildMatrix(
        students: List<FaceEntity>,
        logs: List<CheckInRecordEntity>,
        dateRange: List<LocalDate>,
        policy: String,
        classMap: Map<String, String>
    ): List<MatrixRowModel> = withContext(Dispatchers.Default) {
        val logsByFace = logs.groupBy { it.faceId }

        students.map { student ->
            var aggregateMinutes = 0L
            var h = 0; var s = 0; var i = 0; var a = 0

            val cells = dateRange.map { date ->
                val dailyLogs = logsByFace[student.faceId]?.filter { it.attendanceDate == date }?.sortedBy { it.checkInTime } ?: emptyList()
                val status = dailyLogs.firstOrNull()?.status ?: if (!date.isAfter(LocalDate.now())) "A" else "-"

                var dailyMinutes = 0L
                if (dailyLogs.size >= 2) {
                    for (idx in 0 until (dailyLogs.size - 1) step 2) {
                        val startTime = dailyLogs[idx].checkInTime
                        val endTime = dailyLogs[idx + 1].checkInTime
                        if (startTime != null && endTime != null && endTime.isAfter(startTime)) {
                            dailyMinutes += Duration.between(startTime, endTime).toMinutes()
                        }
                    }
                }

                aggregateMinutes += dailyMinutes
                when (status) {
                    "H" -> h++; "S" -> s++; "I" -> i++; "A" -> a++
                }

                val cellText = getCellText(policy, dailyMinutes, status, dailyLogs)
                val (textColor, bgColor) = getCellColors(dailyMinutes, status)

                MatrixCellModel(text = cellText, textColor = textColor, bgColor = bgColor, isBold = dailyMinutes > 0)
            }

            val totalHours = if (policy != "SCHOOL") {
                val hours = aggregateMinutes / 60
                val mins = aggregateMinutes % 60
                if (policy == "HOURLY") String.format("%d.%02d", hours, mins) else "${hours}j ${mins}m"
            } else ""

            MatrixRowModel(
                studentId = student.faceId,
                studentName = student.name,
                studentClass = logsByFace[student.faceId]?.firstOrNull()?.let { classMap[it.classId] } ?: "Tanpa Kelas",
                cells = cells,
                totalHours = totalHours,
                summaryH = h.toString(),
                summaryS = s.toString(),
                summaryI = i.toString(),
                summaryA = a.toString(),
                estimatedSalary = "Rp 0"
            )
        }
    }

    private fun getCellText(policy: String, dailyMinutes: Long, status: String, logs: List<CheckInRecordEntity>): String {
        return when (policy.trim().uppercase()) {
            "HOURLY" -> when {
                dailyMinutes > 0 -> String.format("%d.%02d", dailyMinutes / 60, dailyMinutes % 60)
                status == "A" || status == "H" -> "0"
                else -> status
            }
            "GARMENT", "FACTORY" -> when {
                dailyMinutes > 0 -> "${dailyMinutes / 60}j ${dailyMinutes % 60}m"
                status == "H" -> "Incomp."
                else -> status
            }
            else -> status
        }
    }

    private fun getCellColors(dailyMinutes: Long, status: String): Pair<Color, Color> {
        return when {
            dailyMinutes > 0 -> Color(0xFF2E7D32) to Color(0xFFE8F5E9)
            status == "S" -> Color(0xFFF9A825) to Color(0xFFFFF9C4)
            status == "I" -> Color(0xFF1565C0) to Color(0xFFE3F2FD)
            status == "A" -> Color(0xFFC62828) to Color(0xFFFFEBEE)
            else -> Color.Gray.copy(alpha = 0.4f) to Color.Transparent
        }
    }

    private fun generateDateRange(start: LocalDate, end: LocalDate): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var current = start
        while (!current.isAfter(end)) {
            dates.add(current)
            current = current.plusDays(1)
        }
        return dates
    }

    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }
    fun onDateRangeSelected(start: LocalDate, end: LocalDate) {
        _startDate.value = start
        _endDate.value = end
    }
    fun onClassSelected(classId: String?) { _selectedClassId.value = classId }
    fun onPolicySelected(policy: String) { _policy.value = policy }
}
