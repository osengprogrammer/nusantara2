package com.azuratech.azuratime.features.reporting.ui.daily

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * 📊 DAILY DETAIL VIEW MODEL (v3.2.0-ai-native)
 */
@HiltViewModel
class DailyDetailViewModel @Inject constructor() : ViewModel() {
    private val _uiStateFlow = MutableStateFlow(DailyDetailUiState())
    val uiStateFlow: StateFlow<DailyDetailUiState> = _uiStateFlow.asStateFlow()

    fun onEvent(event: DailyDetailUiEvent) {
        when (event) {
            DailyDetailUiEvent.LoadData -> { /* Placeholder */ }
        }
    }
}

data class DailyDetailUiState(
    val isLoading: Boolean = false,
)

sealed class DailyDetailUiEvent {
    data object LoadData : DailyDetailUiEvent()
}
