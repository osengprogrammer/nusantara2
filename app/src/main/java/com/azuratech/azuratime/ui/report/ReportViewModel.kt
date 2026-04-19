package com.azuratech.azuratime.ui.report

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.data.local.*
import com.azuratech.azuratime.data.repository.ReportRepository
import com.azuratech.azuratime.domain.report.usecase.GetReportDataUseCase
import com.azuratech.azuratime.domain.sync.usecase.SyncMasterDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class MatrixParams(
    val start: LocalDate,
    val end: LocalDate,
    val classId: String?,
    val role: String,
    val assigned: List<String>
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    application: Application,
    private val repository: ReportRepository,
    private val getReportDataUseCase: GetReportDataUseCase,
    private val syncMasterDataUseCase: SyncMasterDataUseCase
) : AndroidViewModel(application) {

    private val _startDate = MutableStateFlow(LocalDate.now().withDayOfMonth(1))
    private val _endDate = MutableStateFlow(LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()))
    private val _selectedClassId = MutableStateFlow<String?>("ALL")
    private val _userRole = MutableStateFlow("ADMIN")
    private val _assignedClasses = MutableStateFlow<List<String>>(emptyList())
    private val _reportPolicy = MutableStateFlow("SCHOOL")

    @OptIn(ExperimentalCoroutinesApi::class)
    val availableClasses: StateFlow<List<ClassEntity>> = combine(_userRole, _assignedClasses) { role, assigned ->
        role to assigned
    }.flatMapLatest { (role, assigned) ->
        getReportDataUseCase.getAvailableClasses(role, assigned)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @Suppress("UNCHECKED_CAST")
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val matrixReport: StateFlow<List<MatrixRowModel>> =
        combine(
            _startDate, _endDate, _selectedClassId, _userRole, _assignedClasses, _reportPolicy
        ) { args: Array<*> ->
            val start = args[0] as LocalDate
            val end = args[1] as LocalDate
            val classId = args[2] as String?
            val role = args[3] as String
            val assigned = args[4] as List<String>
            val policy = args[5] as String
            Triple(MatrixParams(start, end, classId, role, assigned), policy, generateDateRange(start, end))
        }
        .debounce(200)
        .flatMapLatest { (params, policy, dateRange) ->
            combine(
                getReportDataUseCase.getStudentsInReport(params.role, params.classId, params.assigned),
                repository.getCheckInRecordDao().getFilteredRecords(
                    schoolId = "",
                    startDate = params.start,
                    endDate = params.end,
                    classId = params.classId,
                    assignedIds = params.assigned
                ),
                availableClasses
            ) { results: Array<*> ->
                val students = results[0] as List<FaceEntity>
                val logs = results[1] as List<CheckInRecordEntity>
                val classes = results[2] as List<ClassEntity>
                buildMatrix(students, logs, dateRange, policy, classes.associate { it.id to it.name })
            }
        }
        .flowOn(Dispatchers.Default) // Run the entire chain on a background thread
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- The Engine ---
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
                estimatedSalary = "Rp 0" // Salary logic can be added here
            )
        }
    }

    private fun getCellText(policy: String, dailyMinutes: Long, status: String, logs: List<CheckInRecordEntity>): String {
        return when (policy.trim().uppercase()) {
            "HOURLY" -> when {
                dailyMinutes > 0 -> {
                    val hours = dailyMinutes / 60
                    val mins = dailyMinutes % 60
                    String.format("%d.%02d", hours, mins)
                }
                status == "A" || status == "H" -> "0"
                else -> status
            }
            "GARMENT", "FACTORY" -> when {
                dailyMinutes > 0 -> "${dailyMinutes / 60}j ${dailyMinutes % 60}m"
                status == "H" -> "Incomp."
                else -> status
            }
            else -> status // SCHOOL / OFFICE
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

    fun setClassId(id: String?) { _selectedClassId.value = id }
    fun setDateRange(start: LocalDate, end: LocalDate) {
        _startDate.value = start
        _endDate.value = end
    }
    fun setUserRole(role: String) { _userRole.value = role }
    fun setAssignedClasses(classes: List<String>) { _assignedClasses.value = classes }
    fun setPolicy(policy: String) { _reportPolicy.value = policy }

    fun refreshMasterData() {
        viewModelScope.launch { syncMasterDataUseCase() }
    }
}
