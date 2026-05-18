package com.azuratech.azuratime.features.student.ui.form

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.domain.media.PhotoStorageUtils
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.ui.UiEvent
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import com.azuratech.azuratime.features.biometric.domain.repository.BiometricRepository
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import com.azuratech.azuratime.features.student.domain.model.StudentProfile
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
 * Manages student registration and biometric enrollment.
 */
@HiltViewModel
class StudentFormViewModel @Inject constructor(
    private val studentRepository: StudentRepository,
    private val biometricRepository: BiometricRepository,
    private val accountRepository: AccountRepository,
    private val schoolRepository: SchoolRepository,
    private val sessionManager: SessionManager,
    private val photoStorageUtils: PhotoStorageUtils,
) : ViewModel() {

    private val _uiStateFlow = MutableStateFlow(StudentFormUiState())
    val uiStateFlow: StateFlow<StudentFormUiState> = _uiStateFlow.asStateFlow()

    private val _uiEventFlow = MutableSharedFlow<UiEvent>()
    val uiEventFlow: SharedFlow<UiEvent> = _uiEventFlow.asSharedFlow()

    init {
        observeClasses()
    }

    private fun observeClasses() {
        sessionManager.activeSchoolIdFlow
            .filterNotNull()
            .flatMapLatest { schoolId ->
                schoolRepository.observeClasses(schoolId)
            }
            .onEach { result ->
                when (result) {
                    is Result.Success -> {
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
            is StudentFormUiEvent.BiometricScanned -> handleBiometricScanned(event.encoding)
            is StudentFormUiEvent.FaceCaptured -> handleFaceCaptured(event.bitmap, event.embedding)
            is StudentFormUiEvent.SubmitForm -> submitForm()
            is StudentFormUiEvent.Retry -> submitForm()
            is StudentFormUiEvent.ClearError -> _uiStateFlow.update { it.copy(error = null) }
            is StudentFormUiEvent.NavigateBack -> viewModelScope.launch { _uiEventFlow.emit(UiEvent.NavigateUp) }
        }
    }

    private fun updateField(field: String, value: Any) {
        _uiStateFlow.update { state ->
            val updatedProfile = when (field) {
                "name" -> state.profile.copy(name = value as String)
                "studentId" -> state.profile.copy(studentId = value as String, faceId = value as String)
                "studentCode" -> state.profile.copy(studentCode = value as String)
                "classId" -> state.profile.copy(classIds = listOf(value as String))
                "photoUrl" -> state.profile.copy(photoUrl = value as String)
                "embedding" -> state.profile.copy(embedding = value as FloatArray)
                else -> state.profile
            }
            state.copy(profile = updatedProfile, validationErrors = state.validationErrors - field)
        }
    }

    private fun handlePhotoCaptured(bitmap: Bitmap) {
        _uiStateFlow.update { it.copy(capturedBitmap = bitmap, isCapturingPhoto = false) }
    }

    private fun handleBiometricScanned(encoding: ByteArray) {
        // Map encoding to embedding if needed, or store as is
        // For now, assuming encoding is the embedding as ByteArray
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

        _uiStateFlow.update { it.copy(isSubmitting = true, error = null) }

        viewModelScope.launch {
            val schoolId = sessionManager.getActiveSchoolId() ?: ""
            val profile = state.profile.copy(schoolId = schoolId)

            // 1. Save Profile
            when (val result = studentRepository.saveProfile(profile)) {
                is Result.Success -> {
                    _uiStateFlow.update { it.copy(isSubmitting = false, isSubmitted = true) }
                    _uiEventFlow.emit(UiEvent.ShowSnackbar("Siswa berhasil disimpan"))
                    _uiEventFlow.emit(UiEvent.NavigateUp)
                }
                is Result.Failure -> {
                    _uiStateFlow.update { it.copy(isSubmitting = false, error = result.error.message) }
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun validateForm(state: StudentFormUiState): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (state.profile.name.isBlank()) errors["name"] = "Nama tidak boleh kosong"
        if (state.profile.studentId.isBlank()) errors["studentId"] = "ID Siswa tidak boleh kosong"
        if (state.profile.classIds.isEmpty()) errors["classId"] = "Pilih kelas terlebih dahulu"
        if (state.profile.embedding == null) errors["biometric"] = "Biometrik wajah diperlukan"
        return errors
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        return stream.toByteArray()
    }

    fun loadStudentForEdit(studentId: String) {
        viewModelScope.launch {
            val schoolId = sessionManager.getActiveSchoolId() ?: ""
            when (val result = biometricRepository.getStudentWithDetails(studentId, schoolId)) {
                is Result.Success -> {
                    result.data?.let { details ->
                        _uiStateFlow.update {
                            it.copy(
                                profile = StudentProfile(
                                    studentId = details.biometric.studentId,
                                    name = details.biometric.name,
                                    schoolId = schoolId,
                                    classIds = listOfNotNull(details.classId),
                                    faceId = details.biometric.studentId,
                                    embedding = details.biometric.embedding,
                                    photoUrl = details.biometric.photoUrl,
                                ),
                                isEditMode = true,
                                pageTitle = "Edit Profil Siswa",
                            )
                        }
                    } ?: run {
                        _uiStateFlow.update { it.copy(error = "Siswa tidak ditemukan") }
                    }
                }
                is Result.Failure -> {
                    _uiStateFlow.update { it.copy(error = result.error.message) }
                }
                else -> {}
            }
        }
    }
}
