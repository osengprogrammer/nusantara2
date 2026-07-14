package com.azuratech.azuratime.features.biometric.ui.enroll

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.onFailure
import com.azuratech.azuraengine.result.onSuccess
import com.azuratech.azuratime.features.biometric.domain.model.BiometricEnrollmentProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🧬 BIOMETRIC ENROLLMENT VIEW MODEL (v3.2.0-ai-native)
 * Unified ViewModel for student biometric management and enrollment.
 */
@HiltViewModel
class BiometricEnrollmentViewModel @Inject constructor(
    private val biometricRepository: com.azuratech.azuratime.features.biometric.domain.repository.BiometricRepository,
    private val sessionManager: com.azuratech.azuratime.core.session.SessionManager,
) : ViewModel() {

    private val _uiEffectFlow = MutableSharedFlow<BiometricUiEffect>()
    val uiEffectFlow = _uiEffectFlow.asSharedFlow()

    private val _stateFlow = MutableStateFlow(BiometricEnrollmentUiState())
    val uiStateFlow: StateFlow<BiometricEnrollmentUiState> = _stateFlow.asStateFlow()

    val enrollmentListFlow: StateFlow<List<BiometricEnrollmentProfile>> =
        biometricRepository.observeEnrollmentsFlow()
            .map { result: com.azuratech.azuraengine.result.Result<List<BiometricEnrollmentProfile>> ->
                result.getOrNull() ?: emptyList()
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val studentRosterFlow: StateFlow<List<com.azuratech.azuratime.core.data.local.StudentBiometricDetails>> = sessionManager.activeSchoolIdFlow
        .filterNotNull()
        .flatMapLatest { schoolId -> biometricRepository.getStudentsWithDetailsFlow(schoolId) }
        .map { result: com.azuratech.azuraengine.result.Result<List<com.azuratech.azuratime.core.data.local.StudentBiometricDetails>> ->
            result.getOrNull() ?: emptyList()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val enrolledStudentFlow: StateFlow<List<com.azuratech.azuratime.core.data.local.StudentBiometricEntity>> = sessionManager.activeSchoolIdFlow
        .filterNotNull()
        .flatMapLatest { schoolId -> biometricRepository.getEnrolledStudentsFlow(schoolId) }
        .map { result: com.azuratech.azuraengine.result.Result<List<com.azuratech.azuratime.core.data.local.StudentBiometricEntity>> ->
            result.getOrNull() ?: emptyList()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    fun onEvent(event: BiometricEnrollmentUiEvent) {
        when (event) {
            is BiometricEnrollmentUiEvent.StartCapture -> {
                _stateFlow.update {
                    it.copy(
                        studentId = event.studentId,
                        enrollmentStatus = EnrollmentStatus.CAPTURING,
                        isScanning = true,
                    )
                }
            }
            is BiometricEnrollmentUiEvent.FaceCaptured -> {
                enrollStudent(event.embedding)
            }
            is BiometricEnrollmentUiEvent.SyncBiometric -> syncBiometric()
            is BiometricEnrollmentUiEvent.DeleteBiometric -> deleteEnrollment(event.studentId)
            is BiometricEnrollmentUiEvent.Retry -> {
                _stateFlow.update { it.copy(enrollmentStatus = EnrollmentStatus.CAPTURING, error = null) }
            }
            is BiometricEnrollmentUiEvent.ClearError -> {
                _stateFlow.update { it.copy(error = null) }
            }
            is BiometricEnrollmentUiEvent.NavigateBack -> {
                // Handled in Screen
            }
            is BiometricEnrollmentUiEvent.GrantCameraPermission -> {
                _stateFlow.update { it.copy(cameraPermissionGranted = event.granted) }
            }
        }
    }

    private fun enrollStudent(embedding: FloatArray) {
        val studentId = _stateFlow.value.studentId ?: return

        viewModelScope.launch {
            _stateFlow.update { it.copy(enrollmentStatus = EnrollmentStatus.PROCESSING, isScanning = false) }

            biometricRepository.enrollStudent(studentId, embedding)
                .onSuccess {
                    _stateFlow.update { it.copy(enrollmentStatus = EnrollmentStatus.SUCCESS, capturedEmbedding = embedding) }
                    _uiEffectFlow.emit(BiometricUiEffect.ShowSnackbar("Biometric registration successful!"))
                }
                .onFailure { error ->
                    _stateFlow.update { it.copy(enrollmentStatus = EnrollmentStatus.FAILURE, error = error.message) }
                }
        }
    }

    private fun deleteEnrollment(studentId: String) {
        viewModelScope.launch {
            _stateFlow.update { it.copy(isLoading = true) }
            biometricRepository.deleteEnrollment(studentId)
                .onSuccess {
                    _stateFlow.update { it.copy(isLoading = false) }
                    _uiEffectFlow.emit(BiometricUiEffect.ShowSnackbar("Biometric deleted successfully"))
                }
                .onFailure { error ->
                    _stateFlow.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    private fun syncBiometric() {
        viewModelScope.launch {
            _stateFlow.update { it.copy(isLoading = true) }
            biometricRepository.syncBiometrics()
                .onSuccess {
                    _stateFlow.update { it.copy(isLoading = false) }
                    _uiEffectFlow.emit(BiometricUiEffect.ShowSnackbar("Biometric sync complete"))
                }
                .onFailure { error ->
                    _stateFlow.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun getStudentsInClassFlow(classId: String): kotlinx.coroutines.flow.Flow<List<com.azuratech.azuratime.core.data.local.StudentBiometricEntity>> =
        biometricRepository.getStudentsInClassFlow(classId, sessionManager.getActiveSchoolId() ?: "")
            .map { result: com.azuratech.azuraengine.result.Result<List<com.azuratech.azuratime.core.data.local.StudentBiometricEntity>> ->
                result.getOrNull() ?: emptyList()
            }

    fun updateStudentClass(studentId: String, classId: String?) {
        viewModelScope.launch {
            biometricRepository.updateStudentClass(studentId, classId)
        }
    }

    fun deleteStudentBiometric(studentId: String) {
        deleteEnrollment(studentId)
    }
}
