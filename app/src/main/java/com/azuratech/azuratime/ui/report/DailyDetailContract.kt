package com.azuratech.azuratime.ui.report

import com.azuratech.azuratime.data.local.CheckInRecordEntity
import com.azuratech.azuratime.data.local.ClassEntity

data class DailyDetailData(
    val filteredLogs: List<CheckInRecordEntity> = emptyList(),
    val globalClasses: List<ClassEntity> = emptyList(),
    val assignedIds: List<String> = emptyList(),
    val isAdmin: Boolean = false
)

sealed class DailyDetailUiState {
    object Loading : DailyDetailUiState()
    data class Success(val data: DailyDetailData) : DailyDetailUiState()
    data class Error(val message: String) : DailyDetailUiState()
}
