package com.azuratech.azuratime.features.biometric.ui.enroll

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.core.result.onFailure
import com.azuratech.azuratime.core.result.onSuccess
import com.azuratech.azuratime.core.data.local.StudentBiometricDetails
import com.azuratech.azuratime.core.data.local.StudentBiometricEntity
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.biometric.domain.model.BiometricEnrollmentProfile
import com.azuratech.azuratime.features.biometric.domain.usecase.ObserveEnrollmentsUseCase
import com.azuratech.azuratime.features.biometric.domain.usecase.ObserveStudentsWithDetailsUseCase
import com.azuratech.azuratime.features.biometric.domain.usecase.ObserveEnrolledStudentsUseCase
import com.azuratech.azuratime.features.biometric.domain.usecase.EnrollStudentUseCase
import com.azuratech.azuratime.features.biometric.domain.usecase.DeleteEnrollmentUseCase
import com.azuratech.azuratime.features.biometric.domain.usecase.SyncBiometricsUseCase
import com.azuratech.azuratime.features.biometric.domain.usecase.ObserveStudentsInClassUseCase
import com.azuratech.azuratime.features.biometric.domain.usecase.UpdateStudentClassUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🧬 BIOMETRIC ENROLLMENT VIEW MODEL (v3.2.0-ai-native)
 * Unified ViewModel for student biometric management and enrollment.
 */
@HiltViewModel
class BiometricEnrollmentViewModel @Inject constructor(
    private val observeEnrollmentsUseCase: ObserveEnrollmentsUseCase,
    private val observeStudentsWithDetailsUseCase: ObserveStudentsWithDetailsUseCase,
    private val observeEnrolledStudentsUseCase: ObserveEnrolledStudentsUseCase,
    private val enrollStudentUseCase: EnrollStudentUseCase,
    private val deleteEnrollmentUseCase: DeleteEnrollmentUseCase,
    private val syncBiometricsUseCase: SyncBiometricsUseCase,
    private val observeStudentsInClassUseCase: ObserveStudentsInClassUseCase,
    private val updateStudentClassUseCase: UpdateStudentClassUseCase,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiEffectFlow = MutableSharedFlow<BiometricUiEffect>()
    val uiEffectFlow = _uiEffectFlow.asSharedFlow()

    private val _stateFlow = MutableStateFlow(BiometricEnrollmentUiState())
    val uiStateFlow: StateFlow<BiometricEnrollmentUiState> = _stateFlow.asStateFlow()

    val enrollmentListFlow: StateFlow<List<BiometricEnrollmentProfile>> =
        observeEnrollmentsUseCase()
            .map { result -> result.getOrNull() ?: emptyList() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val studentRosterFlow: StateFlow<List<StudentBiometricDetails>> = sessionManager.activeSchoolIdFlow
        .filterNotNull()
        .flatMapLatest { schoolId -> observeStudentsWithDetailsUseCase(schoolId) }
        .map { result -> result.getOrNull() ?: emptyList() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val enrolledStudentFlow: StateFlow<List<StudentBiometricEntity>> = sessionManager.activeSchoolIdFlow
        .filterNotNull()
        .flatMapLatest { schoolId -> observeEnrolledStudentsUseCase(schoolId) }
        .map { result -> result.getOrNull() ?: emptyList() }
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

            enrollStudentUseCase(studentId, embedding)
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
            deleteEnrollmentUseCase(studentId)
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
            syncBiometricsUseCase()
                .onSuccess {
                    _stateFlow.update { it.copy(isLoading = false) }
                    _uiEffectFlow.emit(BiometricUiEffect.ShowSnackbar("Biometric sync complete"))
                }
                .onFailure { error ->
                    _stateFlow.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun getStudentsInClassFlow(classId: String): kotlinx.coroutines.flow.Flow<List<StudentBiometricEntity>> =
        observeStudentsInClassUseCase(classId, sessionManager.getActiveSchoolId() ?: "")
            .map { result -> result.getOrNull() ?: emptyList() }

    fun updateStudentClass(studentId: String, classId: String?) {
        viewModelScope.launch {
            updateStudentClassUseCase(studentId, classId)
        }
    }

    fun deleteStudentBiometric(studentId: String) {
        deleteEnrollment(studentId)
    }
}
