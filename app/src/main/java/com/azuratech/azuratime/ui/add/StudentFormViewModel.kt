package com.azuratech.azuratime.ui.add

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.domain.face.usecase.GetFaceWithDetailsUseCase
import com.azuratech.azuratime.domain.student.usecase.SaveStudentProfileUseCase
import com.azuratech.azuratime.domain.model.StudentProfile
import com.azuratech.azuratime.domain.model.SyncStatus
import com.azuratech.azuratime.domain.media.PhotoStorageUtils
import com.azuratech.azuratime.domain.assignment.usecase.AssignStudentToClassUseCase
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.ui.core.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class StudentFormViewModel @Inject constructor(
    private val saveStudentProfileUseCase: SaveStudentProfileUseCase,
    private val getFaceWithDetailsUseCase: GetFaceWithDetailsUseCase,
    private val schoolRepository: com.azuratech.azuratime.data.repo.SchoolRepository,
    private val assignStudentToClassUseCase: AssignStudentToClassUseCase,
    private val getUserByIdUseCase: com.azuratech.azuratime.domain.user.usecase.GetUserByIdUseCase,
    private val sessionManager: com.azuratech.azuratime.core.session.SessionManager,
    private val photoStorageUtils: PhotoStorageUtils
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentFormUiState())
    val uiState: StateFlow<StudentFormUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    // 🎓 REACTIVE CLASSES FLOW
    private val _classesFlow = sessionManager.activeSchoolIdFlow
        .filterNotNull()
        .flatMapLatest { schoolId ->
            schoolRepository.observeClasses(schoolId)
        }
        .map { result ->
            when (result) {
                is Result.Success -> result.data
                else -> emptyList()
            }
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
                                photoUrl = faceWithDetails.face.photoUrl,
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
        updateState { it.copy(capturedBitmap = bitmap) }
    }

    fun onPhotoUploaded(bitmap: Bitmap) {
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

            user?.let { println("🔍 StudentForm: Fetched user ${it.userId} for save") }

            if (activeSchoolId == null && user?.role == "SUPER_ADMIN") {
                updateState { it.copy(isSubmitting = false) }
                _uiEvent.emit(UiEvent.ShowSnackbar("Please select a school first"))
                return@launch
            }

            val resolvedSchoolId = activeSchoolId ?: ""
            if (resolvedSchoolId.isBlank()) {
                updateState { it.copy(isSubmitting = false) }
                _uiEvent.emit(UiEvent.ShowSnackbar("School context required"))
                return@launch
            }

            // Determine IDs
            val studentId = if (currentState.isEditMode) currentState.studentId else "STU-${UUID.randomUUID().toString().take(8)}"
            val faceId = if (currentState.isEditMode) {
                currentState.studentId 
            } else {
                "FACE-${studentId}-${System.currentTimeMillis()}"
            }

            // 1. Construct StudentProfile
            val profile = StudentProfile(
                studentId = studentId,
                studentCode = currentState.studentCode,
                name = currentState.name,
                schoolId = resolvedSchoolId,
                classIds = listOfNotNull(selectedClassId),
                faceId = faceId,
                embedding = currentState.embedding,
                photoUrl = currentState.photoUrl,
                syncStatus = SyncStatus.PENDING_UPDATE
            )

            // 2. Save via Modernized UseCase (now handles photoBytes)
            when (val result = saveStudentProfileUseCase(profile, photoBytes)) {
                is Result.Success -> {
                    val message = if (currentState.isEditMode) "Berhasil diperbarui" else "Siswa berhasil didaftarkan"
                    _uiEvent.emit(UiEvent.ShowSnackbar(message))
                    _uiEvent.emit(UiEvent.NavigateUp)
                }
                is Result.Failure -> {
                    val errorMsg = result.error.message ?: "Gagal menyimpan"
                    updateState { it.copy(isSubmitting = false, formError = errorMsg) }
                    _uiEvent.emit(UiEvent.ShowSnackbar("Gagal menyimpan: $errorMsg"))
                }
                is Result.Loading -> {}
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
}
