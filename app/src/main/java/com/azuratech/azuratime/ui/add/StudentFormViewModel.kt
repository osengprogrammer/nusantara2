package com.azuratech.azuratime.ui.add

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.data.repository.ClassRepository
import com.azuratech.azuratime.data.repository.FaceRepository
import com.azuratech.azuratime.data.repository.RegisterResult
import com.azuratech.azuratime.domain.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentFormViewModel @Inject constructor(
    private val faceRepository: FaceRepository,
    private val classRepository: ClassRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentFormUiState())
    val uiState: StateFlow<StudentFormUiState> = _uiState.asStateFlow()

    init {
        // Load available classes once
        viewModelScope.launch {
            classRepository.allClasses.collect { classes ->
                updateState { it.copy(availableClasses = classes) }
            }
        }
    }

    fun loadStudentForEdit(faceId: String) {
        viewModelScope.launch {
            val result = faceRepository.getFaceWithDetails(faceId)
            result.getOrNull()?.let { faceWithDetails ->
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
                faceRepository.updateFaceWithPhoto(
                    face = currentState.toFaceEntity(),
                    photoBitmap = currentState.capturedBitmap,
                    embedding = currentState.embedding!!
                ).fold(
                    onSuccess = { Result.Success(RegisterResult.Success) },
                    onFailure = { Result.Failure(it) }
                )
            } else {
                // Register new student
                faceRepository.registerFace(
                    inputId = currentState.studentId,
                    classId = currentState.selectedClassId!!,
                    name = currentState.name,
                    embedding = currentState.embedding!!,
                    photoBitmap = currentState.capturedBitmap
                )
            }

            registerResult.fold(
                onSuccess = { res ->
                    when (res) {
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
                },
                onFailure = { error ->
                    updateState { it.copy(isSubmitting = false, formError = error.message ?: "Unknown error") }
                }
            )
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
        photoUrl = null // Photo path is handled by the repository
    )
}
