package com.azuratech.azuratime.features.reporting.ui.daily

/**
 * ⚡ DAILY DETAIL UI EFFECT (v3.2.0-ai-native)
 */
sealed class DailyDetailUiEffect {
    data class ShowToast(val message: String) : DailyDetailUiEffect()
}
