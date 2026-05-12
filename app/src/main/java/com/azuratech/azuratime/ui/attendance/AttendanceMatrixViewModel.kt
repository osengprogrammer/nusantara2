package com.azuratech.azuratime.ui.attendance

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.features.attendance.data.local.AttendanceSummary
import com.azuratech.azuratime.features.attendance.data.local.AttendanceRecordEntity
import com.azuratech.azuratime.features.attendance.data.local.toProfile
import com.azuratech.azuratime.domain.model.AttendanceProfile
import com.azuratech.azuratime.domain.model.SyncStatus
import com.azuratech.azuratime.data.repo.AttendanceRepository
import com.azuratech.azuratime.features.staff.data.repo.StaffAccountRepository
import com.azuratech.azuratime.domain.sync.ExportUtils
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuraengine.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class AttendanceMatrixViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val schoolRepository: com.azuratech.azuratime.data.repo.SchoolRepository,
    private val userRepository: StaffAccountRepository,
    private val exportUtils: ExportUtils,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _startDate = MutableStateFlow(LocalDate.now().withDayOfMonth(1))
    private val _endDate = MutableStateFlow(LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()))
    private val _selectedClassId = MutableStateFlow<String?>("ALL")
    private val _policy = MutableStateFlow("SCHOOL")
    private val _selectedTabIndex = MutableStateFlow(0)

    private val _userRole = MutableStateFlow("ADMIN")
    private val _assignedClasses = MutableStateFlow<List<String>>(emptyList())

    // 🔥 v3.1: Reactive Attendance SSOT Migration (Phase 7.8)
    val attendanceMatrixStateFlow: StateFlow<List<AttendanceProfile>> = combine(
        sessionManager.activeSchoolIdFlow.filterNotNull(),
        _startDate, _endDate, _selectedClassId, _searchQuery
    ) { schoolId, start, end, classId, query ->
        AttendanceParams(schoolId, start, end, classId, query)
    }
    .debounce(300)
    .flatMapLatest { p ->
        attendanceRepository.observeAttendanceMatrix(p.schoolId)
            .map { entities -> 
                entities.map { it.toProfile() }
                    .filter { it.date in p.start..p.end }
                    .filter { p.classId == "ALL" || it.classId == p.classId }
                    .filter { p.query.isBlank() || it.studentName.contains(p.query, ignoreCase = true) }
            }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private data class AttendanceParams(val schoolId: String, val start: LocalDate, val end: LocalDate, val classId: String?, val query: String)

    val availableClassesStateFlow: StateFlow<List<ClassModel>> = sessionManager.activeSchoolIdFlow
        .filterNotNull()
        .flatMapLatest { schoolId ->
            schoolRepository.observeClasses(schoolId).map { 
                if (it is com.azuratech.azuraengine.result.Result.Success) it.data else emptyList() 
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isExporting = MutableStateFlow(false)
    private val _exportedFile = MutableStateFlow<String?>(null)

    val uiStateStateFlow: StateFlow<com.azuratech.azuratime.ui.report.AttendanceMatrixUiState> = combine(
        attendanceMatrixStateFlow,
        availableClassesStateFlow,
        _searchQuery,
        _startDate, _endDate,
        _selectedClassId, _policy, _selectedTabIndex,
        _isExporting, _exportedFile
    ) { args ->
        val profiles = args[0] as List<AttendanceProfile>
        val classes = args[1] as List<ClassModel>
        val query = args[2] as String
        val start = args[3] as LocalDate
        val end = args[4] as LocalDate
        val classId = args[5] as String?
        val policy = args[6] as String
        val selectedTabIndex = args[7] as Int
        val isExporting = args[8] as Boolean
        val exportedFile = args[9] as String?
        
        val dateRange = generateDateRange(start, end)
        val rows = buildMatrixFromProfiles(profiles, dateRange, policy)

        com.azuratech.azuratime.ui.report.AttendanceMatrixUiState.Success(
            com.azuratech.azuratime.ui.report.AttendanceMatrixData(
                rows = rows,
                availableClasses = classes,
                dateRange = dateRange,
                searchQuery = query,
                startDate = start,
                endDate = end,
                selectedClassId = classId,
                policy = policy,
                selectedTabIndex = selectedTabIndex,
                isExporting = isExporting,
                exportedFile = exportedFile
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.azuratech.azuratime.ui.report.AttendanceMatrixUiState.Loading)

    private fun buildMatrixFromProfiles(
        profiles: List<AttendanceProfile>,
        dateRange: List<LocalDate>,
        policy: String
    ): List<com.azuratech.azuratime.ui.report.MatrixRowModel> {
        val profilesByStudent = profiles.groupBy { it.studentId }
        
        return profilesByStudent.map { (studentId, studentProfiles) ->
            val firstProfile = studentProfiles.first()
            var h = 0; var s = 0; var i = 0; var a = 0

            val cells = dateRange.map { date ->
                val profile = studentProfiles.find { it.date == date }
                val status = profile?.status ?: if (!date.isAfter(LocalDate.now())) "A" else "-"
                
                when (status) {
                    "H" -> h++; "S" -> s++; "I" -> i++; "A" -> a++
                }

                val (textColor, bgColor) = getCellColors(0, status)
                com.azuratech.azuratime.ui.report.MatrixCellModel(
                    text = status,
                    textColor = textColor,
                    bgColor = bgColor,
                    isBold = status == "H"
                )
            }

            com.azuratech.azuratime.ui.report.MatrixRowModel(
                studentId = studentId,
                studentName = firstProfile.studentName,
                studentClass = firstProfile.className,
                cells = cells,
                totalHours = "",
                summaryH = h.toString(),
                summaryS = s.toString(),
                summaryI = i.toString(),
                summaryA = a.toString(),
                estimatedSalary = "Rp 0"
            )
        }
    }

    fun exportReport() {
        viewModelScope.launch {
            val currentState = uiStateStateFlow.value
            if (currentState !is com.azuratech.azuratime.ui.report.AttendanceMatrixUiState.Success) return@launch

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

    private fun getCellColors(dailyMinutes: Long, status: String): Pair<Color, Color> {
        return when {
            status == "H" -> Color(0xFF2E7D32) to Color(0xFFE8F5E9)
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
    fun markAttendance(studentId: String, date: Long, status: String) {
        viewModelScope.launch {
            val result = attendanceRepository.updateAttendanceStatus(studentId, date, status)
            if (result is Result.Success) {
                /* feedback logic */
            }
        }
    }

    fun onTabSelected(index: Int) { _selectedTabIndex.value = index }
}
