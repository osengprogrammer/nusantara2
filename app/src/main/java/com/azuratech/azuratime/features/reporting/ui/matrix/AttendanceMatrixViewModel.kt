package com.azuratech.azuratime.features.reporting.ui.matrix

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AttendanceMatrixViewModel @Inject constructor() : ViewModel() {
    private val _uiStateFlow = MutableStateFlow<AttendanceMatrixUiState>(AttendanceMatrixUiState.Loading)
    val uiStateFlow: StateFlow<AttendanceMatrixUiState> = _uiStateFlow
}
