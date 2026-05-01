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

    // 🎓 REACTIVE CLASSES FLOW
    private val _classesFlow = sessionManager.activeSchoolIdFlow
        .onStart { 
            val id = sessionManager.getActiveSchoolId()
            println("📚 DEBUG: StudentFormVM _classesFlow onStart -> $id")
            emit(id) 
        }
        .onEach { println("📚 DEBUG: StudentFormVM SchoolId flow emitted: $it") }
        .filter { id -> 
            val isValid = !id.isNullOrBlank()
            if (!isValid) println("⚠️ DEBUG: StudentFormVM - Filtering out null/blank schoolId")
            isValid
        }
        .filterNotNull()
        .flatMapLatest { schoolId ->
            println("📚 DEBUG: StudentFormVM - flatMapLatest loading for $schoolId")
            getClassesUseCase(schoolId)
        }
        .onEach { println("📚 DEBUG: StudentFormVM - UseCase result received: $it") }
        .map { result ->
            when (result) {
                is Result.Success -> {
                    println("✅ DEBUG: StudentFormVM - Successfully loaded ${result.data.size} classes")
                    result.data
                }
                is Result.Failure -> {
                    println("❌ DEBUG: StudentFormVM - Failed to load classes: ${result.error.message}")
                    emptyList()
                }
                is Result.Loading -> {
                    println("⏳ DEBUG: StudentFormVM - Classes are loading...")
                    emptyList()
                }
            }
        }
        .catch { e ->
            println("❌ Classes load exception: ${e.message}")
            e.printStackTrace()
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    
    val classes: StateFlow<List<com.azuratech.azuraengine.model.ClassModel>> = _classesFlow

    private var selectedClassId: String? = null
    private var selectedClassName: String? = null

    init {
        // Keep UI state synced with classes flow for AddUserContent compatibility
        _classesFlow.onEach { classes ->
            updateState { it.copy(availableClasses = classes) }
        }.launchIn(viewModelScope)
    }

    fun loadStudentForEdit(faceId: String) {
        viewModelScope.launch {
            when (val result = getFaceWithDetailsUseCase(faceId)) {
                is Result.Success -> {
                    result.data?.let { faceWithDetails ->
                        selectedClassId = faceWithDetails.classId
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

    fun onClassSelected(classId: String, className: String) {
        selectedClassId = classId
        selectedClassName = className
        println("🎓 Class selected: $className ($classId)")
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

        println("🎓 ViewModel: Saving with classId=$selectedClassId")
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
                        val errorMsg = result.error.message ?: "Update gagal"
                        updateState { it.copy(isSubmitting = false, formError = errorMsg) }
                        _uiEvent.emit(UiEvent.ShowSnackbar("Gagal menyimpan: $errorMsg"))
                    }
                    else -> {}
                }
            } else {
                // Create new student identity & biometric
                val result = createStudentUseCase(
                    schoolId = activeSchoolId,
                    name = currentState.name,
                    studentCode = currentState.studentCode,
                    classId = selectedClassId, // Use the variable
                    faceEmbedding = currentState.embedding,
                    photoBytes = photoBytes
                )

                when (result) {
                    is Result.Success -> {
                        _uiEvent.emit(UiEvent.ShowSnackbar("Siswa berhasil didaftarkan"))
                        _uiEvent.emit(UiEvent.NavigateUp)
                    }
                    is Result.Failure -> {
                        val errorMsg = result.error.message ?: "Pendaftaran gagal"
                        updateState { it.copy(isSubmitting = false, formError = errorMsg) }
                        _uiEvent.emit(UiEvent.ShowSnackbar("Gagal menyimpan: $errorMsg"))
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
