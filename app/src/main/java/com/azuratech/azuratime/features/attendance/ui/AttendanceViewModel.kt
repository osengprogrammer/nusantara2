package com.azuratech.azuratime.features.attendance.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.core.domain.model.ClassModel
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceRecord
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceStatus
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceResult
import com.azuratech.azuratime.features.attendance.domain.repository.ProcessAttendanceParams
import com.azuratech.azuratime.features.attendance.domain.usecase.DeleteAttendanceRecordUseCase
import com.azuratech.azuratime.features.attendance.domain.usecase.ExportAttendanceLogsUseCase
import com.azuratech.azuratime.features.attendance.domain.usecase.ObserveAttendanceDataUseCase
import com.azuratech.azuratime.features.attendance.domain.usecase.ProcessAttendanceUseCase
import com.azuratech.azuratime.features.attendance.domain.usecase.SaveAttendanceRecordUseCase
import com.azuratech.azuratime.features.attendance.domain.usecase.UpdateAttendanceRecordDetailsUseCase
import com.azuratech.azuratime.features.attendance.domain.usecase.UpdateAttendanceRecordStatusUseCase
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.result.onFailure
import com.azuratech.azuratime.core.result.onSuccess
import com.azuratech.azuratime.core.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 📝 ATTENDANCE VIEW MODEL (v3.2.0-ai-native)
 * Unified View Model for Attendance History and Management.
 * Fully VSA compliant — all repository calls go through UseCases.
 */
@HiltViewModel
class AttendanceViewModel @Inject constructor(
    application: Application,
    private val sessionManager: SessionManager,
    private val syncManager: SyncManager,
    private val observeAttendanceDataUseCase: ObserveAttendanceDataUseCase,
    private val processAttendanceUseCase: ProcessAttendanceUseCase,
    private val deleteAttendanceRecordUseCase: DeleteAttendanceRecordUseCase,
    private val saveAttendanceRecordUseCase: SaveAttendanceRecordUseCase,
    private val updateAttendanceRecordStatusUseCase: UpdateAttendanceRecordStatusUseCase,
    private val updateAttendanceRecordDetailsUseCase: UpdateAttendanceRecordDetailsUseCase,
    private val exportAttendanceLogsUseCase: ExportAttendanceLogsUseCase,
) : AndroidViewModel(application) {

    private val _uiStateFlow = MutableStateFlow(AttendanceUiState())
    val uiStateFlow: StateFlow<AttendanceUiState> = _uiStateFlow.asStateFlow()

    private val _uiEffectFlow = MutableSharedFlow<AttendanceUiEffect>()
    val uiEffectFlow = _uiEffectFlow.asSharedFlow()

    private val _refreshTriggerFlow = MutableStateFlow(0)

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            observeAttendanceDataUseCase(
                schoolIdFlow = sessionManager.activeSchoolIdFlow,
                selectedClassIdFlow = _uiStateFlow.map { it.selectedClassId }.distinctUntilChanged(),
                searchQueryFlow = _uiStateFlow.map { it.searchQuery }.distinctUntilChanged(),
                refreshTriggerFlow = _refreshTriggerFlow,
            ).collect { data ->
                _uiStateFlow.update {
                    it.copy(
                        classes = data.classes,
                        records = data.recordsResult.getOrNull() ?: emptyList(),
                        isSyncing = data.isSyncing,
                    )
                }
                data.recordsResult.onFailure { error ->
                    _uiEffectFlow.emit(AttendanceUiEffect.ShowToast("Failed to load data: ${error.message}"))
                }
            }
        }
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

            processAttendanceUseCase(params)
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
            deleteAttendanceRecordUseCase(record.recordId, schoolId)
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
            saveAttendanceRecordUseCase(record)
                .onSuccess { _refreshTriggerFlow.value++ }
        }
    }

    private fun updateRecordStatus(record: AttendanceRecord, newStatus: AttendanceStatus) {
        viewModelScope.launch {
            val schoolId = sessionManager.getActiveSchoolId() ?: return@launch
            updateAttendanceRecordStatusUseCase(record.recordId, newStatus, schoolId)
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
            updateAttendanceRecordDetailsUseCase(record.recordId, classModel.id, classModel.name)
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
            exportAttendanceLogsUseCase(records)
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
