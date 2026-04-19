package com.azuratech.azuratime.ui.add

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.domain.face.RegisterResult
import com.azuratech.azuratime.domain.face.usecase.GetFaceWithDetailsUseCase
import com.azuratech.azuratime.domain.face.usecase.RegisterFaceUseCase
import com.azuratech.azuratime.domain.face.usecase.UpdateFaceWithPhotoUseCase
import com.azuratech.azuratime.domain.classes.usecase.GetClassesUseCase
import com.azuratech.azuratime.domain.assignment.usecase.AssignStudentToClassUseCase
import com.azuratech.azuratime.domain.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentFormViewModel @Inject constructor(
    private val registerFaceUseCase: RegisterFaceUseCase,
    private val updateFaceWithPhotoUseCase: UpdateFaceWithPhotoUseCase,
    private val getFaceWithDetailsUseCase: GetFaceWithDetailsUseCase,
    private val getClassesUseCase: GetClassesUseCase,
    private val assignStudentToClassUseCase: AssignStudentToClassUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentFormUiState())
    val uiState: StateFlow<StudentFormUiState> = _uiState.asStateFlow()

    init {
        // Load available classes using GetClassesUseCase
        getClassesUseCase().onEach { result ->
            if (result is Result.Success) {
                updateState { it.copy(availableClasses = result.data) }
            }
        }.launchIn(viewModelScope)
    }

    fun loadStudentForEdit(faceId: String) {
        viewModelScope.launch {
            when (val result = getFaceWithDetailsUseCase(faceId)) {
                is Result.Success -> {
                    result.data?.let { faceWithDetails ->
                        updateState {
                            it.copy(
                                name = faceWithDetails.face.name,
                                studentId = faceWithDetails.face.faceId,
                                selectedClassId = faceWithDetails.classId,
                                embedding = faceWithDetails.face.embedding,
                                isEditMode = true,
                                pageTitle = "Edit Profil Siswa"
                            )
                        }
                    }
                }
                is Result.Failure -> {
                    updateState { it.copy(formError = result.error.message ?: "Gagal memuat data") }
                }
                is Result.Loading -> {
                    updateState { it.copy(isSubmitting = true) }
                }
            }
        }
    }

    // --- Event Handlers for Form Fields ---

    fun onNameChange(name: String) {
        updateState { it.copy(name = name) }
    }

    fun onStudentIdChange(studentId: String) {
        updateState { it.copy(studentId = studentId) }
    }

    fun onClassSelected(classId: String) {
        updateState { it.copy(selectedClassId = classId) }
    }

    fun onPhotoCaptured(bitmap: Bitmap) {
        // In a real implementation, you would process the bitmap to get an embedding here.
        // For now, we just store the bitmap.
        updateState { it.copy(capturedBitmap = bitmap) }
    }

    fun onFaceCaptured(bitmap: Bitmap, embedding: FloatArray) {
        updateState { it.copy(capturedBitmap = bitmap, embedding = embedding) }
    }

    fun onEmbeddingCaptured(embedding: FloatArray) {
        updateState { it.copy(embedding = embedding) }
    }

    // --- Main Action ---

    fun saveStudent(onSuccess: (String) -> Unit) {
        val currentState = _uiState.value
        if (!currentState.isFormValid) return

        updateState { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            val registerResult: Result<RegisterResult> = if (currentState.isEditMode) {
                // Update existing student
                updateFaceWithPhotoUseCase(
                    face = currentState.toFaceEntity(),
                    photoBitmap = currentState.capturedBitmap,
                    embedding = currentState.embedding!!
                ).map { RegisterResult.Success }
            } else {
                // Register new student
                registerFaceUseCase(
                    inputId = currentState.studentId,
                    classId = currentState.selectedClassId!!,
                    name = currentState.name,
                    embedding = currentState.embedding!!,
                    photoBitmap = currentState.capturedBitmap
                )
            }

            // After registration/update, assign to class if needed
            if (registerResult is Result.Success && currentState.selectedClassId != null) {
                assignStudentToClassUseCase(currentState.studentId, currentState.selectedClassId!!)
            }

            when (registerResult) {
                is Result.Success -> {
                    when (val res = registerResult.data) {
                        is RegisterResult.Success -> {
                            val successMessage = if (currentState.isEditMode) "Berhasil diperbarui" else "Berhasil didaftarkan"
                            onSuccess(successMessage)
                        }
                        is RegisterResult.Duplicate -> {
                            updateState { it.copy(isSubmitting = false, formError = "Wajah ini mirip dengan ${res.name}") }
                        }
                        is RegisterResult.Error -> {
                            updateState { it.copy(isSubmitting = false, formError = res.message) }
                        }
                    }
                }
                is Result.Failure -> {
                    updateState { it.copy(isSubmitting = false, formError = registerResult.error.message ?: "Unknown error") }
                }
                is Result.Loading -> {}
            }
        }
    }

    // --- Private Helper ---

    private fun updateState(update: (StudentFormUiState) -> StudentFormUiState) {
        val newState = update(_uiState.value)
        _uiState.value = newState.copy(isFormValid = validateForm(newState))
    }

    private fun validateForm(state: StudentFormUiState): Boolean {
        return state.name.isNotBlank() &&
                state.studentId.isNotBlank() &&
                state.selectedClassId != null &&
                state.embedding != null
    }

    private fun StudentFormUiState.toFaceEntity() = com.azuratech.azuratime.data.local.FaceEntity(
        faceId = this.studentId,
        name = this.name,
        embedding = this.embedding,
        photoUrl = null // Photo path is handled by the UseCase
    )
}
