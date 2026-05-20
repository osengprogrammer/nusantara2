package com.azuratech.azuratime.features.reporting.ui.daily

/**
 * 📝 DAILY DETAIL UI EVENT (v3.2.0-ai-native)
 */
sealed class DailyDetailUiEvent {
    data class LoadData(val studentId: String, val date: String) : DailyDetailUiEvent()
}
