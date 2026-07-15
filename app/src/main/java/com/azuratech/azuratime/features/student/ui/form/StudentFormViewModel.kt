package com.azuratech.azuratime.features.student.ui.form

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.core.result.AppError
import com.azuratech.azuratime.core.result.Result
import com.azuratech.azuratime.core.result.onFailure
import com.azuratech.azuratime.core.result.onSuccess
import com.azuratech.azuratime.core.domain.media.PhotoStorageUtils
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import com.azuratech.azuratime.features.biometric.domain.repository.BiometricRepository
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import com.azuratech.azuratime.features.student.domain.repository.StudentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * 🚀 STUDENT FORM VIEW MODEL (v3.2.0-ai-native)
 * Optimized with Effect-Driven MVI pattern.
 */
@HiltViewModel
class StudentFormViewModel @Inject constructor(
    private val studentRepository: StudentRepository,
    private val biometricRepository: BiometricRepository,
    private val accountRepository: AccountRepository,
    private val schoolRepository: SchoolRepository,
    private val sessionManager: SessionManager,
    private val photoStorageUtils: PhotoStorageUtils,
    private val syncUseCase: com.azuratech.azuratime.features.student.domain.usecase.SyncPendingStudentDataUseCase,
) : ViewModel() {

    private val _uiStateFlow = MutableStateFlow(StudentFormUiState())
    val uiStateFlow: StateFlow<StudentFormUiState> = _uiStateFlow.asStateFlow()

    private val _uiEffectFlow = MutableSharedFlow<StudentFormUiEffect>()
    val uiEffectFlow: SharedFlow<StudentFormUiEffect> = _uiEffectFlow.asSharedFlow()

    init {
        observeClassesFlow()
    }

    private fun observeClassesFlow() {
        sessionManager.activeSchoolIdFlow
            .filterNotNull()
            .flatMapLatest { schoolId ->
                schoolRepository.observeClassesFlow(schoolId)
            }
            .onEach { result ->
                when (result) {
                    is com.azuratech.azuratime.core.result.Result.Success -> {
                        _uiStateFlow.update { it.copy(availableClasses = result.data) }
                    }
                    else -> {
                        _uiStateFlow.update { it.copy(availableClasses = emptyList()) }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: StudentFormUiEvent) {
        when (event) {
            is StudentFormUiEvent.UpdateField -> updateField(event.field, event.value)
            is StudentFormUiEvent.CapturePhoto -> _uiStateFlow.update { it.copy(isCapturingPhoto = true) }
            is StudentFormUiEvent.PhotoSelected -> updateField("photoUrl", event.uri)
            is StudentFormUiEvent.PhotoCaptured -> handlePhotoCaptured(event.bitmap)
            is StudentFormUiEvent.BiometricScanned -> handleBiometricScanned()
            is StudentFormUiEvent.FaceCaptured -> handleFaceCaptured(event.bitmap, event.embedding)
            is StudentFormUiEvent.ToggleClass -> toggleClass(event.classId)
            is StudentFormUiEvent.SubmitForm -> submitForm()
            is StudentFormUiEvent.Retry -> submitForm()
            is StudentFormUiEvent.ClearError -> { /* Handled via Effects in UI */ }
            is StudentFormUiEvent.NavigateBack -> viewModelScope.launch { _uiEffectFlow.emit(StudentFormUiEffect.NavigateBack) }
        }
    }

    private fun updateField(field: String, value: Any) {
        _uiStateFlow.update { state ->
            val updatedProfile = when (field) {
                "name" -> state.profile.copy(name = value as String)
                "studentId" -> state.profile.copy(studentId = value as String, faceId = value)
                "studentCode" -> state.profile.copy(studentCode = value as String)
                "photoUrl" -> state.profile.copy(photoUrl = value as String)
                "embedding" -> state.profile.copy(embedding = value as FloatArray)
                else -> state.profile
            }
            state.copy(profile = updatedProfile, validationErrors = state.validationErrors - field)
        }
    }

    private fun toggleClass(classId: String) {
        _uiStateFlow.update { state ->
            val currentClasses = state.profile.classIds
            val updatedClasses = if (currentClasses.contains(classId)) {
                currentClasses - classId
            } else {
                currentClasses + classId
            }
            state.copy(
                profile = state.profile.copy(classIds = updatedClasses),
                validationErrors = state.validationErrors - "classId",
            )
        }
    }

    private fun handlePhotoCaptured(bitmap: Bitmap) {
        _uiStateFlow.update { it.copy(capturedBitmap = bitmap, isCapturingPhoto = false) }
    }

    private fun handleBiometricScanned() {
        _uiStateFlow.update { it.copy(biometricStatus = BiometricStatus.Success) }
    }

    private fun handleFaceCaptured(bitmap: Bitmap, embedding: FloatArray) {
        _uiStateFlow.update {
            it.copy(
                capturedBitmap = bitmap,
                profile = it.profile.copy(embedding = embedding),
                biometricStatus = BiometricStatus.Success,
            )
        }
    }

    private fun submitForm() {
        val state = _uiStateFlow.value
        val validationErrors = validateForm(state)
        if (validationErrors.isNotEmpty()) {
            _uiStateFlow.update { it.copy(validationErrors = validationErrors) }
            return
        }

        _uiStateFlow.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            val schoolId = sessionManager.getActiveSchoolId() ?: ""
            val profile = state.profile.copy(schoolId = schoolId)

            // 1. Save Profile
            when (val result = studentRepository.saveProfile(profile)) {
                is Result.Success -> {
                    // ✅ FIX: TRIGGER PUSH TO FIRESTORE IMMEDIATELY
                    syncUseCase.pushAll(schoolId)
                        .onSuccess { android.util.Log.d("STUDENT_FORM", "✅ Student & Classes pushed to Firestore") }
                        .onFailure { err: AppError -> android.util.Log.e("STUDENT_FORM", "❌ Push failed: ${err.message}") }

                    _uiStateFlow.update { it.copy(isSubmitting = false, isSubmitted = true) }
                    _uiEffectFlow.emit(StudentFormUiEffect.ShowSnackbar("Student saved successfully"))
                    _uiEffectFlow.emit(StudentFormUiEffect.NavigateBack)
                }
                is Result.Failure -> {
                    _uiStateFlow.update { it.copy(isSubmitting = false) }
                    _uiEffectFlow.emit(StudentFormUiEffect.ShowToast("Failed: ${result.error.message}"))
                }
                is Result.Loading -> {}
                Result.Network -> {}
            }
        }
    }

    private fun validateForm(state: StudentFormUiState): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (state.profile.name.isBlank()) errors["name"] = "Name cannot be empty"
        if (state.profile.studentId.isBlank()) errors["studentId"] = "Student ID cannot be empty"
        if (state.profile.classIds.isEmpty()) errors["classId"] = "Select at least one class"
        if (state.profile.embedding == null) errors["biometric"] = "Face biometric is required"
        return errors
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        return stream.toByteArray()
    }

    fun loadStudentForEdit(studentId: String) {
        viewModelScope.launch {
            when (val result = studentRepository.getProfileById(studentId)) {
                is Result.Success -> {
                    result.data?.let { profile ->
                        _uiStateFlow.update {
                            it.copy(
                                profile = profile,
                                isEditMode = true,
                                pageTitle = "Edit Student Profile",
                            )
                        }
                    } ?: run {
                        _uiEffectFlow.emit(StudentFormUiEffect.ShowToast("Student not found"))
                    }
                }
                is Result.Failure -> {
                    _uiEffectFlow.emit(StudentFormUiEffect.ShowToast("Error: ${result.error.message}"))
                }
                else -> {}
            }
        }
    }
}
