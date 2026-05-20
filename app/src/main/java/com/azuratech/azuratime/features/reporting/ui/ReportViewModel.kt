package com.azuratech.azuratime.features.reporting.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuraengine.result.onSuccess
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.reporting.domain.repository.ExportRepository
import com.azuratech.azuratime.features.reporting.domain.repository.ReportRepository
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

/**
 * 🚀 REPORT VIEW MODEL (v3.2.0-ai-native)
 * Manages report dashboards, audit logs, and export tasks.
 */
@HiltViewModel
class ReportViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val exportRepository: ExportRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiStateFlow = MutableStateFlow(ReportUiState())
    val uiStateFlow: StateFlow<ReportUiState> = combine(
        _uiStateFlow,
        sessionManager.currentAccountIdFlow.filterNotNull(),
        sessionManager.activeSchoolIdFlow.filterNotNull(),
    ) { state, _, _ ->
        // We use combine to react to accountId/schoolId changes if needed,
        // but core data is loaded via onEvent/init
        state
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportUiState())

    init {
        observeExportJobs()
        refreshData()
    }

    private fun observeExportJobs() {
        viewModelScope.launch {
            sessionManager.currentAccountIdFlow
                .filterNotNull()
                .flatMapLatest { accountId ->
                    exportRepository.observeExportJobs(accountId)
                }
                .collect { result ->
                    result.onSuccess { jobs ->
                        _uiStateFlow.update { it.copy(exportJobs = jobs) }
                    }
                }
        }
    }

    fun onEvent(event: ReportUiEvent) {
        when (event) {
            is ReportUiEvent.SetDateRange -> {
                _uiStateFlow.update { it.copy(startDate = event.start, endDate = event.end) }
                refreshData()
            }
            ReportUiEvent.RefreshData -> refreshData()
            is ReportUiEvent.StartExport -> startExport(event.format)
            ReportUiEvent.ClearError -> _uiStateFlow.update { it.copy(error = null) }
            is ReportUiEvent.SelectTab -> _uiStateFlow.update { it.copy(selectedTab = event.tab) }
            ReportUiEvent.NavigateToDetail -> { /* Handled by screen navigation */ }
            ReportUiEvent.ClearExportJobs -> clearExportJobs()
        }
    }

    private fun refreshData() {
        val currentState = _uiStateFlow.value
        _uiStateFlow.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val schoolId = sessionManager.getActiveSchoolId() ?: ""
            when (val result = reportRepository.getAuditLogs(currentState.startDate, currentState.endDate, schoolId)) {
                is Result.Success -> {
                    _uiStateFlow.update { it.copy(isLoading = false, auditLogs = result.data) }
                }
                is Result.Failure -> {
                    _uiStateFlow.update { it.copy(isLoading = false, error = result.error.message) }
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun startExport(format: String) {
        _uiStateFlow.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val accountId = sessionManager.getCurrentAccountId() ?: ""
            val schoolId = sessionManager.getActiveSchoolId() ?: ""

            when (val result = exportRepository.startExport(format, accountId, schoolId)) {
                is Result.Success -> {
                    _uiStateFlow.update { it.copy(isLoading = false) }
                    // UI will update via observeExportJobs flow
                }
                is Result.Failure -> {
                    _uiStateFlow.update { it.copy(isLoading = false, error = result.error.message) }
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun clearExportJobs() {
        viewModelScope.launch {
            val accountId = sessionManager.getCurrentAccountId() ?: ""
            exportRepository.clearCompletedJobs(accountId)
        }
    }
}
