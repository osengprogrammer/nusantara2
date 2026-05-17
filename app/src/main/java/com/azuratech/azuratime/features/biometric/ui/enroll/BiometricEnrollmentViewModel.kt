package com.azuratech.azuratime.features.biometric.ui.enroll

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.onFailure
import com.azuratech.azuraengine.result.onSuccess
import com.azuratech.azuratime.core.ui.UiEvent
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
    private val biometricRepository: com.azuratech.azuratime.features.biometric.domain.repository.StudentBiometricRepository,
    private val sessionManager: com.azuratech.azuratime.core.session.SessionManager,
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _state = MutableStateFlow(BiometricEnrollmentUiState())
    val uiState: StateFlow<BiometricEnrollmentUiState> = _state.asStateFlow()

    val enrollmentList: StateFlow<List<BiometricEnrollmentProfile>> =
        biometricRepository.observeEnrollments()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val studentRosterFlow: StateFlow<List<com.azuratech.azuratime.core.data.local.StudentBiometricDetails>> = sessionManager.activeSchoolIdFlow
        .filterNotNull()
        .flatMapLatest { schoolId -> biometricRepository.getStudentsWithDetailsFlow(schoolId) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val enrolledStudentFlow: StateFlow<List<com.azuratech.azuratime.features.biometric.data.local.StudentBiometricEntity>> = sessionManager.activeSchoolIdFlow
        .filterNotNull()
        .flatMapLatest { schoolId -> biometricRepository.getEnrolledStudentsFlow(schoolId) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    fun onEvent(event: BiometricEnrollmentUiEvent) {
        when (event) {
            is BiometricEnrollmentUiEvent.StartCapture -> {
                _state.update {
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
                _state.update { it.copy(enrollmentStatus = EnrollmentStatus.CAPTURING, error = null) }
            }
            is BiometricEnrollmentUiEvent.ClearError -> {
                _state.update { it.copy(error = null) }
            }
            is BiometricEnrollmentUiEvent.NavigateBack -> {
                // Handled in Screen
            }
            is BiometricEnrollmentUiEvent.GrantCameraPermission -> {
                _state.update { it.copy(cameraPermissionGranted = event.granted) }
            }
        }
    }

    private fun enrollStudent(embedding: FloatArray) {
        val studentId = _state.value.studentId ?: return

        viewModelScope.launch {
            _state.update { it.copy(enrollmentStatus = EnrollmentStatus.PROCESSING, isScanning = false) }

            biometricRepository.enrollStudent(studentId, embedding)
                .onSuccess {
                    _state.update { it.copy(enrollmentStatus = EnrollmentStatus.SUCCESS, capturedEmbedding = embedding) }
                    _uiEvent.emit(UiEvent.ShowSnackbar("Pendaftaran biometrik berhasil!"))
                }
                .onFailure { error ->
                    _state.update { it.copy(enrollmentStatus = EnrollmentStatus.FAILURE, error = error.message) }
                }
        }
    }

    private fun deleteEnrollment(studentId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            biometricRepository.deleteEnrollment(studentId)
                .onSuccess {
                    _state.update { it.copy(isLoading = false) }
                    _uiEvent.emit(UiEvent.ShowSnackbar("Biometrik berhasil dihapus"))
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    private fun syncBiometric() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            biometricRepository.syncBiometrics()
                .onSuccess {
                    _state.update { it.copy(isLoading = false) }
                    _uiEvent.emit(UiEvent.ShowSnackbar("Sinkronisasi biometrik selesai"))
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun getStudentsInClassFlow(classId: String): kotlinx.coroutines.flow.Flow<List<com.azuratech.azuratime.features.biometric.data.local.StudentBiometricEntity>> =
        biometricRepository.getStudentsInClassFlow(classId, sessionManager.getActiveSchoolId() ?: "")

    fun updateStudentClass(studentId: String, classId: String?) {
        viewModelScope.launch {
            biometricRepository.updateStudentClass(studentId, classId)
        }
    }

    fun deleteStudentBiometric(studentId: String) {
        deleteEnrollment(studentId)
    }
}
