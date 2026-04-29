package com.azuratech.azuratime.ui.add

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.domain.face.usecase.GetFaceWithDetailsUseCase
import com.azuratech.azuratime.domain.student.usecase.CreateStudentUseCase
import com.azuratech.azuratime.domain.face.usecase.UpdateFaceWithPhotoUseCase
import com.azuratech.azuratime.domain.classes.usecase.GetClassesUseCase
import com.azuratech.azuratime.domain.assignment.usecase.AssignStudentToClassUseCase
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.ui.core.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentFormViewModel @Inject constructor(
    private val createStudentUseCase: CreateStudentUseCase,
    private val updateFaceWithPhotoUseCase: UpdateFaceWithPhotoUseCase,
    private val getFaceWithDetailsUseCase: GetFaceWithDetailsUseCase,
    private val getClassesUseCase: GetClassesUseCase,
    private val assignStudentToClassUseCase: AssignStudentToClassUseCase,
    private val getUserByIdUseCase: com.azuratech.azuratime.domain.user.usecase.GetUserByIdUseCase,
    private val sessionManager: com.azuratech.azuratime.core.session.SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentFormUiState())
    val uiState: StateFlow<StudentFormUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        // Reactive collection of active school classes
        sessionManager.activeSchoolIdFlow
            .filterNotNull()
            .flatMapLatest { schoolId ->
                println("📚 DEBUG: Loading classes for active school $schoolId")
                getClassesUseCase(schoolId)
            }
            .onEach { result ->
                if (result is Result.Success) {
                    println("📦 DEBUG: Fetched ${result.data.size} classes for reactive collection")
                    updateState { it.copy(availableClasses = result.data) }
                }
            }
            .launchIn(viewModelScope)
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

    fun onStudentCodeChange(studentCode: String) {
        updateState { it.copy(studentCode = studentCode) }
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

    fun saveStudent() {
        val currentState = _uiState.value
        if (!currentState.isFormValid) return

        updateState { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            val photoBytes = currentState.capturedBitmap?.let { bitmapToByteArray(it) }
            val activeSchoolId = sessionManager.getActiveSchoolId()
            val currentUserId = sessionManager.getCurrentUserId()
            val user = currentUserId?.let { getUserByIdUseCase(it) }

            user?.let { println("🔍 StudentForm: Fetched user ${it.userId} for edit") }

            if (activeSchoolId == null && user?.role == "SUPER_ADMIN") {
                updateState { it.copy(isSubmitting = false) }
                _uiEvent.emit(UiEvent.ShowSnackbar("Please select a school first"))
                return@launch
            }

            if (currentState.isEditMode) {
                // Update existing student/face
                val result = updateFaceWithPhotoUseCase(
                    face = currentState.toFaceEntity(),
                    photoBytes = photoBytes,
                    embedding = currentState.embedding!!
                )
                
                when (result) {
                    is Result.Success -> {
                        _uiEvent.emit(UiEvent.ShowSnackbar("Berhasil diperbarui"))
                        _uiEvent.emit(UiEvent.NavigateUp)
                    }
                    is Result.Failure -> {
                        updateState { it.copy(isSubmitting = false, formError = result.error.message ?: "Update gagal") }
                    }
                    else -> {}
                }
            } else {
                // Create new student identity & biometric
                val result = createStudentUseCase(
                    schoolId = activeSchoolId,
                    name = currentState.name,
                    studentCode = currentState.studentCode,
                    classId = currentState.selectedClassId,
                    faceEmbedding = currentState.embedding,
                    photoBytes = photoBytes
                )

                when (result) {
                    is Result.Success -> {
                        _uiEvent.emit(UiEvent.ShowSnackbar("Siswa berhasil didaftarkan"))
                        _uiEvent.emit(UiEvent.NavigateUp)
                    }
                    is Result.Failure -> {
                        updateState { it.copy(isSubmitting = false, formError = result.error.message ?: "Pendaftaran gagal") }
                    }
                    is Result.Loading -> {}
                }
            }
        }
    }

    // --- Private Helper ---

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        return stream.toByteArray()
    }

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
