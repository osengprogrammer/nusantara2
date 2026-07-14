package com.azuratech.azuratime.features.attendance.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceRecord
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceStatus
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceResult
import com.azuratech.azuratime.features.attendance.domain.repository.AttendanceRepository
import com.azuratech.azuratime.features.attendance.domain.repository.ProcessAttendanceParams
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuraengine.result.onFailure
import com.azuratech.azuraengine.result.onSuccess
import com.azuratech.azuratime.core.domain.repository.SyncRepository
import com.azuratech.azuratime.core.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 📝 ATTENDANCE VIEW MODEL (v3.2.0-ai-native)
 * Unified View Model for Attendance History and Management.
 */
@HiltViewModel
class AttendanceViewModel @Inject constructor(
    application: Application,
    private val attendanceRepository: AttendanceRepository,
    private val schoolRepository: SchoolRepository,
    private val sessionManager: SessionManager,
    private val syncRepository: SyncRepository,
    private val syncManager: SyncManager,
) : AndroidViewModel(application) {

    private val _uiStateFlow = MutableStateFlow(AttendanceUiState())
    val uiStateFlow: StateFlow<AttendanceUiState> = _uiStateFlow.asStateFlow()

    private val _uiEffectFlow = MutableSharedFlow<AttendanceUiEffect>()
    val uiEffectFlow = _uiEffectFlow.asSharedFlow()

    private val _refreshTriggerFlow = MutableStateFlow(0)

    init {
        observeData()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeData() {
        // 1. Observe Classes
        sessionManager.activeSchoolIdFlow
            .filterNotNull()
            .flatMapLatest { schoolId ->
                schoolRepository.observeClassesFlow(schoolId).map { it.getOrNull() ?: emptyList() }
            }
            .onEach { classes ->
                _uiStateFlow.update { it.copy(classes = classes) }
            }
            .launchIn(viewModelScope)

        // 2. Observe Records with Filters
        combine(
            sessionManager.activeSchoolIdFlow.filterNotNull(),
            _uiStateFlow.map { it.selectedClassId }.distinctUntilChanged(),
            _uiStateFlow.map { it.searchQuery }.distinctUntilChanged(),
            _refreshTriggerFlow,
        ) { schoolId, classId, query, _ ->
            Triple(schoolId, classId, query)
        }.flatMapLatest { (schoolId, classId, query) ->
            _uiStateFlow.update { it.copy(isLoading = true) }
            attendanceRepository.getAttendanceRecordsFlow(
                name = query,
                startDate = null,
                endDate = null,
                accountId = null,
                classId = classId,
                assignedIds = emptyList(),
                schoolId = schoolId,
            )
        }.onEach { result ->
            result.onSuccess { records ->
                _uiStateFlow.update { it.copy(isLoading = false, records = records) }
            }.onFailure { error ->
                _uiStateFlow.update { it.copy(isLoading = false) }
                _uiEffectFlow.emit(AttendanceUiEffect.ShowToast("Failed to load data: ${error.message}"))
            }
        }.launchIn(viewModelScope)

        // 3. Observe Global Sync Status
        syncRepository.isSyncingFlow
            .onEach { result ->
                val isSyncing = result.getOrNull() ?: false
                _uiStateFlow.update { it.copy(isSyncing = isSyncing) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: AttendanceUiEvent) {
        when (event) {
            AttendanceUiEvent.LoadAttendance -> _refreshTriggerFlow.value++
            is AttendanceUiEvent.SelectClass -> _uiStateFlow.update { it.copy(selectedClassId = event.classId) }
            is AttendanceUiEvent.UpdateSearchQuery -> _uiStateFlow.update { it.copy(searchQuery = event.query) }
            AttendanceUiEvent.Refresh -> _refreshTriggerFlow.value++

            is AttendanceUiEvent.DeleteRecord -> deleteRecord(event.record)
            is AttendanceUiEvent.UpdateRecordStatus -> updateRecordStatus(event.record, event.status)
            is AttendanceUiEvent.UpdateRecordClass -> updateRecordClass(event.record, event.classModel)
            is AttendanceUiEvent.ExportRecords -> exportRecords(event.records)
            AttendanceUiEvent.SyncHistory -> syncHistory()
        }
    }

    private fun syncHistory() {
        syncManager.enqueueSync() // Just trigger the worker, observation handles the rest
    }

    fun processManualAttendance(scannedStudentId: String, studentName: String, studentClasses: List<String>, status: AttendanceStatus, timestamp: Long, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val accountEmail = sessionManager.getAccountEmail()
            val currentSessionId = _uiStateFlow.value.selectedClassId

            val params = ProcessAttendanceParams(
                studentId = scannedStudentId,
                studentName = studentName,
                accountEmail = accountEmail,
                activeClassId = currentSessionId,
                studentClassIds = studentClasses,
                status = status,
                timestamp = timestamp,
            )

            attendanceRepository.processAttendance(params)
                .onSuccess { res ->
                    when (res) {
                        is AttendanceResult.Success -> onResult(true, res.message)
                        is AttendanceResult.Rejected -> onResult(false, res.reason)
                        is AttendanceResult.AlreadyCheckedIn -> onResult(true, "${res.name} already checked in.")
                        AttendanceResult.Unregistered -> onResult(false, "Unknown student")
                    }
                    _refreshTriggerFlow.value++
                }
                .onFailure { error ->
                    onResult(false, "❌ Error: ${error.message}")
                }
        }
    }

    private fun deleteRecord(record: AttendanceRecord) {
        viewModelScope.launch {
            val schoolId = sessionManager.getActiveSchoolId() ?: return@launch
            _uiStateFlow.update { it.copy(isLoading = true) }
            attendanceRepository.deleteRecord(record.recordId, schoolId)
                .onSuccess {
                    _uiEffectFlow.emit(AttendanceUiEffect.ShowToast("Log deleted successfully"))
                    _refreshTriggerFlow.value++
                }
                .onFailure { error ->
                    _uiStateFlow.update { it.copy(isLoading = false) }
                    _uiEffectFlow.emit(AttendanceUiEffect.ShowToast("Failed: ${error.message}"))
                }
        }
    }

    fun addRecord(record: AttendanceRecord) {
        viewModelScope.launch {
            attendanceRepository.saveRecord(record)
                .onSuccess { _refreshTriggerFlow.value++ }
        }
    }

    private fun updateRecordStatus(record: AttendanceRecord, newStatus: AttendanceStatus) {
        viewModelScope.launch {
            val schoolId = sessionManager.getActiveSchoolId() ?: return@launch
            attendanceRepository.updateRecordStatus(record.recordId, newStatus, schoolId)
                .onSuccess {
                    _uiEffectFlow.emit(AttendanceUiEffect.ShowToast("Status updated successfully"))
                    _refreshTriggerFlow.value++
                }
                .onFailure { error ->
                    _uiEffectFlow.emit(AttendanceUiEffect.ShowToast("Failed: ${error.message}"))
                }
        }
    }

    private fun updateRecordClass(record: AttendanceRecord, classModel: ClassModel) {
        viewModelScope.launch {
            attendanceRepository.updateRecord(record.recordId, classModel.id, classModel.name)
                .onSuccess {
                    _uiEffectFlow.emit(AttendanceUiEffect.ShowToast("Class updated successfully"))
                    _refreshTriggerFlow.value++
                }
                .onFailure { error ->
                    _uiEffectFlow.emit(AttendanceUiEffect.ShowToast("Failed: ${error.message}"))
                }
        }
    }

    private fun exportRecords(records: List<AttendanceRecord>) {
        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isExporting = true) }
            attendanceRepository.exportLogs(records)
                .onSuccess { path ->
                    _uiStateFlow.update { it.copy(isExporting = false) }
                    _uiEffectFlow.emit(AttendanceUiEffect.ExportSuccess(path))
                }
                .onFailure { error ->
                    _uiStateFlow.update { it.copy(isExporting = false) }
                    _uiEffectFlow.emit(AttendanceUiEffect.ShowToast("Export Failed: ${error.message}"))
                }
        }
    }
}
