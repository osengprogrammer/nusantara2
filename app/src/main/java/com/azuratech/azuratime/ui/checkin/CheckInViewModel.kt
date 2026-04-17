package com.azuratech.azuratime.ui.checkin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuratime.data.local.CheckInRecordEntity
import com.azuratech.azuratime.data.local.ClassEntity
import com.azuratech.azuratime.data.local.FaceEntity
import com.azuratech.azuratime.data.repository.CheckInRepository
import com.azuratech.azuratime.data.repository.FaceRepository
import com.azuratech.azuratime.core.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel // 🔥 Import Hilt
import javax.inject.Inject // 🔥 Import Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime

@HiltViewModel // 🔥 1. Anotasi Hilt
class CheckInViewModel @Inject constructor( // 🔥 2. Inject semua dependensi
    application: Application,
    database: AppDatabase,
    private val faceRepository: FaceRepository,
    private val checkInRepository: CheckInRepository,
    private val sessionManager: SessionManager // 🔥 SessionManager disuntikkan langsung
) : AndroidViewModel(application) {

    private val faceAssignmentDao = database.faceAssignmentDao()
    private val checkInRecordDao = database.checkInRecordDao()

    private val _activeClassId = MutableStateFlow<String?>(null)
    
    // 🔥 Mengambil schoolId secara reaktif dari sessionManager
    private val schoolId: String get() = sessionManager.getActiveSchoolId() ?: ""

    val unassignedCount: StateFlow<Int> = faceAssignmentDao.getUnassignedStudentCount(schoolId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeSessionStudents: StateFlow<List<FaceEntity>> = _activeClassId
        .flatMapLatest { classId ->
            if (classId != null) faceAssignmentDao.getFacesByClass(classId, schoolId)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeStudentCount: StateFlow<Int> = _activeClassId
        .flatMapLatest { classId ->
            if (classId != null) faceAssignmentDao.getStudentCountInClass(classId, schoolId)
            else flowOf(0)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val sessionSummary: StateFlow<Pair<Int, Int>> = _activeClassId
        .flatMapLatest { classId ->
            if (classId != null) {
                faceAssignmentDao.getStudentCountInClass(classId, schoolId)
                    .combine(checkInRecordDao.getTodayPresentCount(LocalDate.now(), schoolId)) { total, present ->
                        present to total
                    }
            } else flowOf(0 to 0)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0 to 0)

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
    val checkInRecords: StateFlow<List<CheckInRecordEntity>> =
        _filterParams
            .flatMapLatest { params ->
                val targetClassId = if (params.classId == "ALL" || params.classId.isNullOrBlank()) null else params.classId
                checkInRepository.getFilteredRecords(
                    nameFilter = params.name,
                    startDate = params.start,
                    endDate = params.end,
                    userId = params.userId,
                    classId = targetClassId,
                    assignedIds = params.assignedIds
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun processScannedFace(scannedFaceId: String, studentName: String, onResult: (isSuccess: Boolean, message: String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentSessionId = _activeClassId.value
                val teacherEmail = _filterParams.value.userId ?: ""
                val studentClasses = faceAssignmentDao.getClassIdsForFace(scannedFaceId, schoolId).firstOrNull() ?: emptyList()

                if (currentSessionId != null) {
                    if (studentClasses.contains(currentSessionId)) {
                        withContext(Dispatchers.Main) { onResult(true, "✅ Berhasil: $studentName") }
                        saveRecord(scannedFaceId, studentName, teacherEmail, currentSessionId)
                    } else {
                        withContext(Dispatchers.Main) {
                            onResult(false, "❌ Ditolak: $studentName beda kelas!")
                        }
                    }
                } else {
                    val primaryClassId = studentClasses.firstOrNull() ?: "UNASSIGNED"
                    withContext(Dispatchers.Main) {
                        val msg = if (primaryClassId == "UNASSIGNED") "⚠️ $studentName absen (Belum Masuk Kelas)"
                        else "✅ Gerbang: $studentName hadir."
                        onResult(true, msg)
                    }
                    saveRecord(scannedFaceId, studentName, teacherEmail, primaryClassId)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, "❌ Error: ${e.message}") }
            }
        }
    }

    private suspend fun saveRecord(faceId: String, name: String, email: String, classId: String) {
        val record = CheckInRecordEntity(
            faceId = faceId,
            name = name,
            userId = email,
            classId = classId,
            className = "Terekam",
            schoolId = schoolId,
            status = "H",
            attendanceDate = LocalDate.now(),
            checkInTime = LocalDateTime.now(),
            // 🔥 createdAt dihapus dari sini karena sekarang otomatis di-handle oleh timestamp di dalam Entity
            isSynced = false
        )
        checkInRepository.saveRecord(record) 
    }

    fun updateFilters(name: String? = null, start: LocalDate? = null, end: LocalDate? = null) {
        _filterParams.value = _filterParams.value.copy(
            name = name ?: _filterParams.value.name,
            start = start ?: _filterParams.value.start,
            end = end ?: _filterParams.value.end
        )
    }

    fun updateNameFilter(name: String) { _filterParams.value = _filterParams.value.copy(name = name) }
    
    fun updateRecord(record: CheckInRecordEntity) { 
        viewModelScope.launch { checkInRepository.updateRecord(record) } 
    }
    
    fun addRecord(record: CheckInRecordEntity) { 
        viewModelScope.launch { checkInRepository.saveRecord(record) } 
    }
    
    fun updateRecordClass(record: CheckInRecordEntity, selectedClass: ClassEntity) {
        viewModelScope.launch { 
            checkInRepository.updateRecordClass(record.id, selectedClass.id, selectedClass.name) 
        }
    }
    
    fun deleteRecord(record: CheckInRecordEntity) { 
        viewModelScope.launch { checkInRepository.deleteRecord(record) } 
    }
}