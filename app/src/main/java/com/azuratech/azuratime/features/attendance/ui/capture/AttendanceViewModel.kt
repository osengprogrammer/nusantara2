package com.azuratech.azuratime.features.attendance.ui.capture

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceRecord
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceStatus
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceResult
import com.azuratech.azuratime.features.attendance.domain.repository.AttendanceRepository
import com.azuratech.azuratime.features.attendance.domain.repository.ProcessAttendanceParams
import com.azuratech.azuratime.features.school.data.repo.SchoolRepository
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.ui.UiEvent
import com.azuratech.azuratime.core.ui.util.UiState
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.domain.sync.ExportUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    application: Application,
    private val repository: AttendanceRepository,
    private val schoolRepository: SchoolRepository,
    private val sessionManager: SessionManager,
    private val exportUtils: ExportUtils
) : AndroidViewModel(application) {

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    data class FilterParams(
        val name: String = "",
        val startDate: LocalDate? = null,
        val endDate: LocalDate? = null,
        val classId: String? = null
    )

    private val _filterParams = MutableStateFlow(FilterParams())
    val filterParams = _filterParams.asStateFlow()

    val classes = sessionManager.activeSchoolIdFlow
        .filterNotNull()
        .flatMapLatest { schoolId ->
            schoolRepository.observeClasses(schoolId).map { it.getOrNull() ?: emptyList() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attendanceRecords = combine(
        sessionManager.activeSchoolIdFlow.filterNotNull(),
        _filterParams
    ) { schoolId, params ->
        repository.getAttendanceRecords(
            name = params.name,
            startDate = params.startDate,
            endDate = params.endDate,
            userId = null,
            classId = params.classId,
            assignedIds = emptyList(),
            schoolId = schoolId
        ).map { entities -> entities.map { it.toDomain() } }
    }.flattenMerge().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateNameFilter(name: String) {
        _filterParams.update { it.copy(name = name) }
    }

    fun onClassSelected(classId: String?) {
        _filterParams.update { it.copy(classId = classId) }
    }

    fun updateFilters(start: LocalDate?, end: LocalDate?) {
        _filterParams.update { it.copy(startDate = start, endDate = end) }
    }

    fun processManualAttendance(scannedFaceId: String, studentName: String, studentClasses: List<String>, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val teacherEmail = sessionManager.getUserEmail() ?: "unknown@azuratech.com"
                val schoolId = sessionManager.getActiveSchoolId() ?: return@launch
                val currentSessionId = _filterParams.value.classId

                val params = ProcessAttendanceParams(
                    studentId = scannedFaceId,
                    studentName = studentName,
                    teacherEmail = teacherEmail,
                    activeClassId = currentSessionId,
                    studentClassIds = studentClasses
                )

                val result = repository.processAttendance(params)
                
                withContext(Dispatchers.Main) {
                    when (result) {
                        is Result.Success<AttendanceResult> -> {
                            when (val res = result.data) {
                                is AttendanceResult.Success -> onResult(true, res.message)
                                is AttendanceResult.Rejected -> onResult(false, res.reason)
                                is AttendanceResult.AlreadyCheckedIn -> onResult(true, "${res.name} sudah absen.")
                                AttendanceResult.Unregistered -> onResult(false, "Wajah tidak dikenal")
                            }
                        }
                        is Result.Failure -> onResult(false, "❌ Error: ${result.error.message}")
                        Result.Loading -> { }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, "❌ Error: ${e.message}") }
            }
        }
    }

    fun deleteRecord(record: AttendanceRecord) {
        viewModelScope.launch {
            val schoolId = sessionManager.getActiveSchoolId() ?: return@launch
            repository.deleteRecord(record.recordId, schoolId) 
        } 
    }

    fun addRecord(record: AttendanceRecord) {
        viewModelScope.launch {
            repository.saveRecord(record)
        }
    }

    fun updateRecord() {
        // This is a generic update, but usually we update status or class
    }

    fun updateRecordStatus(record: AttendanceRecord, newStatus: AttendanceStatus) {
        viewModelScope.launch {
            val schoolId = sessionManager.getActiveSchoolId() ?: return@launch
            repository.updateRecordStatus(record.recordId, newStatus, schoolId)
        }
    }

    fun updateRecordClass(record: AttendanceRecord, classModel: ClassModel) {
        viewModelScope.launch {
            repository.updateRecord(record.recordId, classModel.id, classModel.name)
        }
    }

    fun exportRecords(records: List<AttendanceRecord>) {
        viewModelScope.launch { exportUtils.exportRawLogsToCsv(records) }
    }
}
