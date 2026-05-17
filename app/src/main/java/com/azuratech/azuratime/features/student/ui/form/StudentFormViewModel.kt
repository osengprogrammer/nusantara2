package com.azuratech.azuratime.features.student.ui.form

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.domain.media.PhotoStorageUtils
import com.azuratech.azuratime.core.domain.model.SyncStatus
import com.azuratech.azuratime.core.ui.UiEvent
import com.azuratech.azuratime.features.account.data.repo.AccountRepository
import com.azuratech.azuratime.features.biometric.domain.repository.StudentBiometricRepository
import com.azuratech.azuratime.features.student.domain.model.StudentProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class StudentFormViewModel @Inject constructor(
    private val biometricRepository: StudentBiometricRepository,
    private val accountRepository: AccountRepository,
    private val schoolRepository: com.azuratech.azuratime.features.school.data.repo.SchoolRepository,
    private val sessionManager: com.azuratech.azuratime.core.session.SessionManager,
    private val photoStorageUtils: PhotoStorageUtils
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentFormUiState())
    val uiStateStateFlow: StateFlow<StudentFormUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEventFlow: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    // 🎓 REACTIVE CLASSES FLOW
    private val _classesFlow = sessionManager.activeSchoolIdStateFlow
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
    
    val classesStateFlow: StateFlow<List<com.azuratech.azuraengine.model.ClassModel>> = _classesFlow

    private var selectedClassId: String? = null
    private var selectedClassName: String? = null

    init {
        // Keep UI state synced with classes flow for AddStudentContent compatibility
        _classesFlow.onEach { classes ->
            updateState { it.copy(availableClasses = classes) }
        }.launchIn(viewModelScope)
    }

    fun loadStudentForEdit(studentId: String) {
        viewModelScope.launch {
            val schoolId = sessionManager.getActiveSchoolId() ?: ""
            val studentBiometricDetails = biometricRepository.getStudentWithDetails(studentId, schoolId)
            
            if (studentBiometricDetails != null) {
                selectedClassId = studentBiometricDetails.classId
                updateState {
                    it.copy(
                        name = studentBiometricDetails.biometric.name,
                        studentId = studentBiometricDetails.biometric.studentId,
                        selectedClassId = studentBiometricDetails.classId,
                        embedding = studentBiometricDetails.biometric.embedding,
                        photoUrl = studentBiometricDetails.biometric.photoUrl,
                        isEditMode = true,
                        pageTitle = "Edit Profil Siswa"
                    )
                }
            } else {
                updateState { it.copy(formError = "Gagal memuat data") }
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
            val currentAccountId = sessionManager.getCurrentUserId()
            val account = currentAccountId?.let { accountRepository.getAccountById(it) }

            account?.let { println("🔍 StudentForm: Fetched account ${it.accountId} for save") }

            if (activeSchoolId == null && account?.role == "SUPER_ADMIN") {
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
            val studentId = if (currentState.studentId.isNotBlank()) {
                currentState.studentId
            } else {
                "STU-${UUID.randomUUID().toString().take(8)}"
            }
            
            // 🔥 AI Friendly: Face ID must match Student ID to ensure unified identity
            val faceId = studentId 

            // 1. Construct StudentProfile
            val profile = StudentProfile(
                studentId = studentId,
                studentCode = currentState.studentCode,
                name = currentState.name,
                schoolId = resolvedSchoolId,
                classIds = listOfNotNull(selectedClassId),
                faceId = faceId, // Explicitly unified
                embedding = currentState.embedding,
                photoUrl = currentState.photoUrl,
                syncStatus = SyncStatus.PENDING_UPDATE
            )

            // 2. Save via Repository
            when (val result = biometricRepository.saveStudentProfile(profile, photoBytes)) {
                is Result.Success<Unit> -> {
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
