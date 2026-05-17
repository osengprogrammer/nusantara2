package com.azuratech.azuratime.features.reporting.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.reporting.data.repo.ExportRepository
import com.azuratech.azuratime.features.reporting.data.repo.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val exportRepository: ExportRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = combine(
        _uiState,
        sessionManager.currentUserIdFlow.filterNotNull(),
        sessionManager.activeSchoolIdFlow.filterNotNull(),
    ) { state, userId, schoolId ->
        // We use combine to react to userId/schoolId changes if needed,
        // but core data is loaded via onEvent/init
        state
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportUiState())

    init {
        observeExportJobs()
        refreshData()
    }

    private fun observeExportJobs() {
        viewModelScope.launch {
            sessionManager.currentUserIdFlow
                .filterNotNull()
                .flatMapLatest { userId ->
                    exportRepository.observeExportJobs(userId)
                }
                .collect { jobs ->
                    _uiState.update { it.copy(exportJobs = jobs) }
                }
        }
    }

    fun onEvent(event: ReportUiEvent) {
        when (event) {
            is ReportUiEvent.SetDateRange -> {
                _uiState.update { it.copy(startDate = event.start, endDate = event.end) }
                refreshData()
            }
            ReportUiEvent.RefreshData -> refreshData()
            is ReportUiEvent.StartExport -> startExport(event.format)
            ReportUiEvent.ClearError -> _uiState.update { it.copy(error = null) }
            is ReportUiEvent.SelectTab -> _uiState.update { it.copy(selectedTab = event.tab) }
            ReportUiEvent.NavigateToDetail -> { /* Handled by screen navigation */ }
            ReportUiEvent.ClearExportJobs -> clearExportJobs()
        }
    }

    private fun refreshData() {
        val currentState = _uiState.value
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val schoolId = sessionManager.getActiveSchoolId() ?: ""
            when (val result = reportRepository.getAuditLogs(currentState.startDate, currentState.endDate, schoolId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, auditLogs = result.data) }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(isLoading = false, error = result.error.message) }
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun startExport(format: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val userId = sessionManager.getCurrentUserId() ?: ""
            val schoolId = sessionManager.getActiveSchoolId() ?: ""

            when (val result = exportRepository.startExport(format, userId, schoolId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    // UI will update via observeExportJobs flow
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(isLoading = false, error = result.error.message) }
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun clearExportJobs() {
        viewModelScope.launch {
            val userId = sessionManager.getCurrentUserId() ?: ""
            exportRepository.clearCompletedJobs(userId)
        }
    }
}
