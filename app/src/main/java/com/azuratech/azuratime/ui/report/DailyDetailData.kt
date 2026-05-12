package com.azuratech.azuratime.ui.report

import com.azuratech.azuratime.domain.checkin.model.CheckInRecord
import com.azuratech.azuraengine.model.ClassModel

data class DailyDetailData(
    val filteredLogs: List<CheckInRecord> = emptyList(),
    val globalClasses: List<ClassModel> = emptyList(),
    val assignedIds: List<String> = emptyList(),
    val isAdmin: Boolean = false
)

sealed class DailyDetailUiState {
    object Loading : DailyDetailUiState()
    data class Success(val data: DailyDetailData) : DailyDetailUiState()
    data class Error(val message: String) : DailyDetailUiState()
}
