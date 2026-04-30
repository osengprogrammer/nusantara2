package com.azuratech.azuratime.ui.checkin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.data.local.FaceEntity
import com.azuratech.azuratime.domain.checkin.model.CheckInRecord
import com.azuratech.azuratime.domain.checkin.model.CheckInResult
import com.azuratech.azuratime.domain.checkin.usecase.*
import com.azuratech.azuratime.domain.school.usecase.GetActiveSchoolContextUseCase
import com.azuratech.azuratime.core.session.SessionManager
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
class CheckInViewModel @Inject constructor(
    application: Application,
    private val repository: CheckInRepository,
    private val getCheckInRecordsUseCase: GetCheckInRecordsUseCase,
    private val processCheckInUseCase: ProcessCheckInUseCase,
    private val updateCheckInRecordUseCase: UpdateCheckInRecordUseCase,
    private val deleteCheckInRecordUseCase: DeleteCheckInRecordUseCase,
    private val getActiveSchoolContextUseCase: GetActiveSchoolContextUseCase,
    private val sessionManager: SessionManager,
    private val exportUtils: ExportUtils
) : AndroidViewModel(application) {

    private val _activeClassId = MutableStateFlow<String?>(null)

    // 🔥 Stream reaktif untuk SchoolContext
    private val schoolContextFlow = sessionManager.activeSchoolIdFlow
        .map { getActiveSchoolContextUseCase() }
        .filterIsInstance<Result.Success<*>>()
        .map { (it as Result.Success).data as com.azuratech.azuratime.domain.school.usecase.SchoolContext }

    @OptIn(ExperimentalCoroutinesApi::class)
    val unassignedCount: StateFlow<Int> = schoolContextFlow
        .flatMapLatest { ctx -> repository.getUnassignedStudentCount(ctx.schoolId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeSessionStudents: StateFlow<List<FaceEntity>> = combine(_activeClassId, schoolContextFlow) { classId, ctx ->
        classId to ctx
    }.flatMapLatest { (classId, ctx) ->
        if (classId != null) repository.getFacesByClass(classId, ctx.schoolId)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeStudentCount: StateFlow<Int> = combine(_activeClassId, schoolContextFlow) { classId, ctx ->
        classId to ctx
    }.flatMapLatest { (classId, ctx) ->
        if (classId != null) repository.getStudentCountInClass(classId, ctx.schoolId)
        else flowOf(0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val sessionSummary: StateFlow<Pair<Int, Int>> = combine(_activeClassId, schoolContextFlow) { classId, ctx ->
        classId to ctx
    }.flatMapLatest { (classId, ctx) ->
        if (classId != null) {
            repository.getStudentCountInClass(classId, ctx.schoolId)
                .combine(repository.getTodayPresentCount(LocalDate.now(), ctx.schoolId)) { total, present ->
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
    val checkInRecords: StateFlow<List<CheckInRecord>> =
        _filterParams
            .flatMapLatest { params ->
                val targetClassId = if (params.classId == "ALL" || params.classId.isNullOrBlank()) null else params.classId
                val filters = CheckInFilters(
                    name = params.name,
                    startDate = params.start,
                    endDate = params.end,
                    userId = params.userId,
                    classId = targetClassId,
                    assignedIds = params.assignedIds
                )
                getCheckInRecordsUseCase(filters)
            }
            .map { it.getOrNull() ?: emptyList() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun processScannedFace(scannedFaceId: String, studentName: String, onResult: (isSuccess: Boolean, message: String) -> Unit) {
        viewModelScope.launch {
            try {
                val contextRes = getActiveSchoolContextUseCase()
                if (contextRes is Result.Failure) {
                    onResult(false, "❌ Error: ${contextRes.error.message ?: "Silakan pilih sekolah"}")
                    return@launch
                }
                val ctx = (contextRes as Result.Success).data
                
                val currentSessionId = _activeClassId.value
                val teacherEmail = _filterParams.value.userId ?: ""
                val studentClasses = withContext(Dispatchers.IO) {
                    repository.getClassIdsForFace(scannedFaceId, ctx.schoolId).firstOrNull() ?: emptyList()
                }

                val params = ProcessCheckInParams(
                    faceId = scannedFaceId,
                    studentName = studentName,
                    teacherEmail = teacherEmail,
                    activeClassId = currentSessionId,
                    studentClassIds = studentClasses
                )

                val result = processCheckInUseCase(params)
                
                withContext(Dispatchers.Main) {
                    when (result) {
                        is Result.Success -> {
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
    
    fun updateRecord(record: CheckInRecord) { 
        viewModelScope.launch { updateCheckInRecordUseCase(record) } 
    }
    
    fun addRecord(record: CheckInRecord) { 
        viewModelScope.launch { 
            val params = ProcessCheckInParams(
                faceId = record.studentId,
                studentName = record.studentName,
                teacherEmail = record.teacherEmail,
                activeClassId = record.classId,
                studentClassIds = record.classId.let { listOf(it) }
            )
            processCheckInUseCase(params)
        } 
    }
    
    fun updateRecordClass(record: CheckInRecord, selectedClass: ClassModel) {
        viewModelScope.launch { 
            updateCheckInRecordUseCase.updateClass(record.recordId, selectedClass.id, selectedClass.name) 
        }
    }
    
    fun deleteRecord(record: CheckInRecord) { 
        viewModelScope.launch { deleteCheckInRecordUseCase(record.recordId) } 
    }

    fun exportRecords(records: List<CheckInRecord>) {
        viewModelScope.launch { exportUtils.exportRawLogsToCsv(records) }
    }
}
