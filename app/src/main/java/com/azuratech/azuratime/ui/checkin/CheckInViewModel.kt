package com.azuratech.azuratime.ui.checkin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.data.local.BiometricFaceEntity
import com.azuratech.azuratime.domain.checkin.model.AttendanceRecord
import com.azuratech.azuratime.domain.checkin.model.CheckInResult
import com.azuratech.azuratime.domain.checkin.repository.ProcessCheckInParams
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.repo.SchoolRepository
import com.azuratech.azuratime.domain.checkin.repository.CheckInRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import com.azuratech.azuratime.domain.sync.ExportUtils
import com.azuratech.azuraengine.result.Result
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@HiltViewModel
class AttendanceCaptureViewModel @Inject constructor(
    application: Application,
    private val repository: CheckInRepository,
    private val schoolRepository: SchoolRepository,
    private val sessionManager: SessionManager,
    private val exportUtils: ExportUtils
) : AndroidViewModel(application) {

    private val _activeClassId = MutableStateFlow<String?>(null)

    // 🔥 Stream reaktif untuk SchoolId
    private val schoolContextFlow = sessionManager.activeSchoolIdFlow
        .filterNotNull()

    @OptIn(ExperimentalCoroutinesApi::class)
    val unassignedCount: StateFlow<Int> = schoolContextFlow
        .flatMapLatest { schoolId -> repository.getUnassignedStudentCount(schoolId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeSessionStudents: StateFlow<List<BiometricFaceEntity>> = combine(_activeClassId, schoolContextFlow) { classId, schoolId ->
        classId to schoolId
    }.flatMapLatest { (classId, schoolId) ->
        if (classId != null) repository.getFacesByClass(classId, schoolId)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeStudentCount: StateFlow<Int> = combine(_activeClassId, schoolContextFlow) { classId, schoolId ->
        classId to schoolId
    }.flatMapLatest { (classId, schoolId) ->
        if (classId != null) repository.getStudentCountInClass(classId, schoolId)
        else flowOf(0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val sessionSummary: StateFlow<Pair<Int, Int>> = combine(_activeClassId, schoolContextFlow) { classId, schoolId ->
        classId to schoolId
    }.flatMapLatest { (classId, schoolId) ->
        if (classId != null) {
            repository.getStudentCountInClass(classId, schoolId)
                .combine(repository.getTodayPresentCount(LocalDate.now(), schoolId)) { total, present ->
                    present to total
                }
        } else flowOf(0 to 0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0 to 0)

    fun setActiveClass(classId: String?) {
        _activeClassId.value = classId
    }

    data class FilterParams(
        val name: String = "",
        val start: LocalDate? = null,
        val end: LocalDate? = null,
        val userId: String? = null,
        val classId: String? = null,
        val assignedIds: List<String> = emptyList()
    )

    private val _filterParams = MutableStateFlow(FilterParams())
    val filterParams: StateFlow<FilterParams> = _filterParams.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val checkInRecords: StateFlow<List<AttendanceRecord>> =
        combine(sessionManager.activeSchoolIdFlow.filterNotNull(), _filterParams) { schoolId, params ->
            schoolId to params
        }.flatMapLatest { (schoolId, params) ->
            val targetClassId = if (params.classId == "ALL" || params.classId.isNullOrBlank()) null else params.classId
            repository.getCheckInRecords(
                name = params.name,
                startDate = params.start,
                endDate = params.end,
                userId = params.userId,
                classId = targetClassId,
                assignedIds = params.assignedIds,
                schoolId = schoolId
            ).map { entities -> entities.map { it.toDomain() } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun processScannedFace(scannedFaceId: String, studentName: String, onResult: (isSuccess: Boolean, message: String) -> Unit) {
        viewModelScope.launch {
            try {
                val schoolId = sessionManager.getActiveSchoolId()
                if (schoolId == null) {
                    onResult(false, "❌ Error: Silakan pilih sekolah")
                    return@launch
                }
                
                val currentSessionId = _activeClassId.value
                val teacherEmail = _filterParams.value.userId ?: ""
                val studentClasses = withContext(Dispatchers.IO) {
                    repository.getClassIdsForFace(scannedFaceId, schoolId).firstOrNull() ?: emptyList()
                }

                val params = ProcessCheckInParams(
                    faceId = scannedFaceId,
                    studentName = studentName,
                    teacherEmail = teacherEmail,
                    activeClassId = currentSessionId,
                    studentClassIds = studentClasses
                )

                val result = repository.processCheckIn(params)
                
                withContext(Dispatchers.Main) {
                    when (result) {
                        is Result.Success<CheckInResult> -> {
                            when (val checkInRes = result.data) {
                                is CheckInResult.Success -> onResult(true, checkInRes.message)
                                is CheckInResult.Rejected -> onResult(false, checkInRes.reason)
                                is CheckInResult.AlreadyCheckedIn -> onResult(true, "${checkInRes.name} sudah absen.")
                                is CheckInResult.Unregistered -> onResult(false, "Wajah tidak dikenal")
                            }
                        }
                        is Result.Failure -> onResult(false, "❌ Error: ${result.error.message}")
                        is Result.Loading -> { /* Loading not handled in this callback pattern */ }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, "❌ Error: ${e.message}") }
            }
        }
    }

    fun updateFilters(name: String? = null, start: LocalDate? = null, end: LocalDate? = null) {
        _filterParams.value = _filterParams.value.copy(
            name = name ?: _filterParams.value.name,
            start = start ?: _filterParams.value.start,
            end = end ?: _filterParams.value.end
        )
    }

    fun updateNameFilter(name: String) { _filterParams.value = _filterParams.value.copy(name = name) }
    
    fun updateRecord(record: AttendanceRecord) { 
        viewModelScope.launch { 
            repository.updateRecord(record.recordId, record.classId, record.className)
        } 
    }
    
    fun addRecord(record: AttendanceRecord) { 
        viewModelScope.launch { 
            repository.saveRecord(record)
        } 
    }
    
    fun updateRecordClass(record: AttendanceRecord, selectedClass: ClassModel) {
        viewModelScope.launch { 
            repository.updateRecord(record.recordId, selectedClass.id, selectedClass.name) 
        }
    }
    
    fun deleteRecord(record: AttendanceRecord) { 
        viewModelScope.launch { 
            val schoolId = sessionManager.getActiveSchoolId() ?: ""
            repository.deleteRecord(record.recordId, schoolId) 
        } 
    }

    fun exportRecords(records: List<AttendanceRecord>) {
        viewModelScope.launch { exportUtils.exportRawLogsToCsv(records) }
    }
}

typealias CheckInViewModel = AttendanceCaptureViewModel
