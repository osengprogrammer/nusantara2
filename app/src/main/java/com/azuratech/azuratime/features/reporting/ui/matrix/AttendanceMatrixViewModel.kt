package com.azuratech.azuratime.features.reporting.ui.matrix

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * 📊 ATTENDANCE MATRIX VIEW MODEL (v3.2.0-ai-native)
 */
@HiltViewModel
class AttendanceMatrixViewModel @Inject constructor() : ViewModel() {
    private val _uiStateFlow = MutableStateFlow<AttendanceMatrixUiState>(AttendanceMatrixUiState.Loading)
    val uiStateFlow: StateFlow<AttendanceMatrixUiState> = _uiStateFlow.asStateFlow()

    fun onEvent(event: AttendanceMatrixUiEvent) {
        when (event) {
            AttendanceMatrixUiEvent.LoadData -> { /* Placeholder */ }
        }
    }
}

sealed class AttendanceMatrixUiEvent {
    data object LoadData : AttendanceMatrixUiEvent()
}
