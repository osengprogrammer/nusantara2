package com.azuratech.azuratime.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.toProfile
import com.azuratech.azuratime.data.repo.ReportRepository
import com.azuratech.azuratime.domain.model.SchoolAnalyticsSummary
import com.azuratech.azuratime.ui.core.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 📊 REPORT VIEW MODEL - Phase 7.10 SSOT Migration
 * Observes SchoolAnalyticsSummary stream directly from Room.
 */
@HiltViewModel
class ReportViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val reportList: StateFlow<List<SchoolAnalyticsSummary>> = 
        sessionManager.activeSchoolIdFlow.filterNotNull()
            .flatMapLatest { schoolId -> 
                reportRepository.observeReportsByDateRange(schoolId)
                    .map { entities -> entities.map { it.toProfile() } }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun generateReport(startDate: Long, endDate: Long) {
        viewModelScope.launch {
            val result = reportRepository.generateReport(startDate, endDate)
            if (result is Result.Success) {
                _uiEvent.emit(UiEvent.ShowSnackbar("Report generated"))
            } else if (result is Result.Failure) {
                _uiEvent.emit(UiEvent.ShowSnackbar("Gagal: ${result.error.message}"))
            }
        }
    }
}
