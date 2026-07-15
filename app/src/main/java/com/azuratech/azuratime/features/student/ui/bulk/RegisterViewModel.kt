package com.azuratech.azuratime.features.student.ui.bulk

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.core.result.Result
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.student.domain.repository.StudentRegistrationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 📝 REGISTER VIEW MODEL (v3.2.0-ai-native)
 * Unified View Model for bulk student registration. Optimized with Effect-Driven MVI.
 * Orchestrates CSV import via StudentRegistrationRepository.
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registrationRepository: StudentRegistrationRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiStateFlow = MutableStateFlow(RegisterUiState())
    val uiStateFlow: StateFlow<RegisterUiState> = _uiStateFlow.asStateFlow()

    private val _uiEffectFlow = MutableSharedFlow<RegisterUiEffect>()
    val uiEffectFlow = _uiEffectFlow.asSharedFlow()

    fun onEvent(event: RegisterUiEvent) {
        when (event) {
            is RegisterUiEvent.ProcessCsv -> processCsvFile(event.uri)
            RegisterUiEvent.ResetState -> _uiStateFlow.value = RegisterUiState()
        }
    }

    private fun processCsvFile(uri: Uri) {
        viewModelScope.launch {
            val schoolId = sessionManager.getActiveSchoolId()
            if (schoolId == null) {
                _uiEffectFlow.emit(RegisterUiEffect.ShowToast("Active school not found."))
                return@launch
            }

            _uiStateFlow.update {
                it.copy(
                    isProcessing = true,
                    status = "Starting bulk import...",
                    results = emptyList(),
                    progress = 0f,
                )
            }

            registrationRepository.processCsvFlow(uri.toString(), schoolId)
                .onEach { result ->
                    when (result) {
                        is Result.Success -> {
                            val processResult = result.data
                            _uiStateFlow.update { state ->
                                state.copy(
                                    results = state.results + processResult,
                                    status = "Processing: ${processResult.name}",
                                    progress = if (state.results.isEmpty()) 0.1f else state.progress + 0.01f,
                                )
                            }
                        }
                        is Result.Failure -> {
                            _uiStateFlow.update { it.copy(isProcessing = false) }
                            _uiEffectFlow.emit(RegisterUiEffect.ShowToast("Failed: ${result.error.message}"))
                        }
                        Result.Loading -> {
                            _uiStateFlow.update { it.copy(status = "Preparing data...") }
                        }
                        Result.Network -> {}
                    }
                }
                .onCompletion {
                    _uiStateFlow.update {
                        it.copy(
                            isProcessing = false,
                            status = "Import Completed! ${it.results.size} data processed.",
                            progress = 1.0f,
                        )
                    }
                }
                .launchIn(viewModelScope)
        }
    }
}
