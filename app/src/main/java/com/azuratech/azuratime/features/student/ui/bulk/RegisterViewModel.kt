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
    private val _state = MutableStateFlow(RegisterUiState())
    val stateStateFlow: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun resetState() {
        _state.value = RegisterUiState()
    }

    fun processCsvFile(uri: Uri, mode: String) {
        // Placeholder
    }
}

data class RegisterUiState(
    val isProcessing: Boolean = false,
    val progress: Float = 0f,
    val status: String = "",
    val results: List<ProcessResult> = emptyList()
)
