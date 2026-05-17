package com.azuratech.azuratime.features.student.ui.bulk

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.azuratech.azuraengine.model.ProcessResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor() : ViewModel() {
    private val _stateFlow = MutableStateFlow(RegisterUiState())
    val uiStateFlow: StateFlow<RegisterUiState> = _stateFlow.asStateFlow()

    fun resetState() {
        _stateFlow.value = RegisterUiState()
    }

    fun processCsvFile(@Suppress("UNUSED_PARAMETER") uri: Uri, @Suppress("UNUSED_PARAMETER") mode: String) {
        // Placeholder
    }
}

data class RegisterUiState(
    val isProcessing: Boolean = false,
    val progress: Float = 0f,
    val status: String = "",
    val results: List<ProcessResult> = emptyList(),
)
