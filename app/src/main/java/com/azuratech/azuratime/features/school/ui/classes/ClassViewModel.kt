package com.azuratech.azuratime.features.school.ui.classes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuraengine.result.onFailure
import com.azuratech.azuraengine.result.onSuccess
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.ui.UiEvent
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import com.azuratech.azuratime.features.student.domain.model.StudentProfile
import com.azuratech.azuratime.features.student.domain.repository.StudentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClassViewModel @Inject constructor(
    private val schoolRepository: SchoolRepository,
    private val accountRepository: AccountRepository,
    private val studentRepository: StudentRepository, // ✅ ADDED
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiEventFlow = MutableSharedFlow<UiEvent>()
    val uiEventFlow = _uiEventFlow.asSharedFlow()

    private val _stateFlow = MutableStateFlow(ClassUiState())
    val uiStateFlow: StateFlow<ClassUiState> = _stateFlow.asStateFlow()

    private val _availableClasses = listOf(
        "10-IPA-1", "10-IPA-2", "10-IPA-3",
        "10-IPS-1", "10-IPS-2", "10-IPS-3",
        "11-IPA-1", "11-IPA-2", "11-IPA-3",
        "11-IPS-1", "11-IPS-2", "11-IPS-3",
        "12-IPA-1", "12-IPA-2", "12-IPA-3",
        "12-IPS-1", "12-IPS-2", "12-IPS-3",
    )

    init {
        _stateFlow.update { it.copy(availableClasses = _availableClasses) }
        loadClasses()
        observeClassesReactive()
    }

    fun onEvent(event: ClassUiEvent) {
        when (event) {
            is ClassUiEvent.LoadClasses -> loadClasses()
            is ClassUiEvent.CreateClass -> createClass(event.name)
            is ClassUiEvent.UpdateClass -> updateClass(event.id, event.newName)
            is ClassUiEvent.RequestDeleteClass -> {
                _stateFlow.update { it.copy(classToDelete = event.classModel) }
            }
            is ClassUiEvent.ConfirmDeleteClass -> {
                _stateFlow.value.classToDelete?.let { deleteClass(it.id) }
            }
            is ClassUiEvent.CancelDeleteClass -> {
                _stateFlow.update { it.copy(classToDelete = null) }
            }
            is ClassUiEvent.RequestEditClass -> {
                _stateFlow.update { it.copy(classToEdit = event.classModel) }
            }
            is ClassUiEvent.CancelEditClass -> {
                _stateFlow.update { it.copy(classToEdit = null) }
            }
            is ClassUiEvent.ShowAddDialog -> {
                _stateFlow.update { it.copy(isAddDialogVisible = true) }
            }
            is ClassUiEvent.DismissAddDialog -> {
                _stateFlow.update { it.copy(isAddDialogVisible = false) }
            }
            is ClassUiEvent.ClearError -> {
                _stateFlow.update { it.copy(error = null) }
            }
            is ClassUiEvent.SyncClasses -> syncClasses()
            is ClassUiEvent.SelectClass -> {
                _stateFlow.update { it.copy(selectedClassId = event.classId) }
                event.classId?.let { observeStudentsInClassInternal(it) }
            }
            ClassUiEvent.ShowAddStudentDialog -> {
                _stateFlow.update { it.copy(isAddStudentDialogVisible = true) }
            }
            ClassUiEvent.DismissAddStudentDialog -> {
                _stateFlow.update { it.copy(isAddStudentDialogVisible = false) }
            }
            is ClassUiEvent.AddStudentToClass -> addStudentToClass(event.classId, event.studentId)
        }
    }

    private fun observeStudentsInClassInternal(classId: String) {
        studentRepository.getStudentProfiles()
            .onEach { result ->
                result.onSuccess { students ->
                    val filtered = students.filter { it.classIds.contains(classId) }
                    _stateFlow.update { it.copy(studentsInClass = filtered) }
                }
            }
            .launchIn(viewModelScope)
    }

    // ✅ FUNGSI BARU: Mengamati siswa di kelas tertentu
    fun observeStudentsInClass(classId: String): Flow<List<StudentProfile>> {
        return studentRepository.getStudentProfiles()
            .map { result ->
                if (result is com.azuratech.azuraengine.result.Result.Success) {
                    result.data.filter { it.classIds.contains(classId) }
                } else {
                    emptyList()
                }
            }
    }

    private fun loadClasses() {
        viewModelScope.launch {
            val schoolId = sessionManager.getActiveSchoolId() ?: return@launch
            _stateFlow.update { it.copy(isLoading = true, error = null) }
            schoolRepository.getClasses(schoolId)
                .onSuccess { _stateFlow.update { it.copy(isLoading = false) } }
                .onFailure { error -> _stateFlow.update { it.copy(isLoading = false, error = error.message) } }
        }
    }

    private fun observeClassesReactive() {
        val activeSchoolIdFlow = sessionManager.activeSchoolIdFlow.filterNotNull()

        combine(
            activeSchoolIdFlow.flatMapLatest { schoolId -> schoolRepository.observeClasses(schoolId) },
            studentRepository.getStudentProfiles(),
        ) { classResult, studentResult ->
            if (classResult is com.azuratech.azuraengine.result.Result.Success && studentResult is com.azuratech.azuraengine.result.Result.Success) {
                val classes = classResult.data
                val students = studentResult.data

                val counts = classes.associate { classModel ->
                    classModel.id to students.count { it.classIds.contains(classModel.id) }
                }

                _stateFlow.update { it.copy(classes = classes, studentCountsByClassId = counts) }
            } else if (classResult is com.azuratech.azuraengine.result.Result.Failure) {
                _stateFlow.update { it.copy(error = classResult.error.message) }
            }
        }.launchIn(viewModelScope)
    }

    private fun createClass(name: String) {
        viewModelScope.launch {
            val schoolId = sessionManager.getActiveSchoolId() ?: return@launch
            val accountId = sessionManager.getCurrentAccountId() ?: return@launch

            _stateFlow.update { it.copy(isLoading = true, isAddDialogVisible = false) }

            val classModel = ClassModel(
                id = "cls_${System.currentTimeMillis()}",
                name = name,
                schoolId = schoolId,
                grade = "",
                accountId = null,
                studentCount = 0,
                createdAt = System.currentTimeMillis(),
            )

            schoolRepository.saveClass(accountId, schoolId, classModel)
                .onSuccess {
                    _stateFlow.update { it.copy(isLoading = false) }
                    _uiEventFlow.emit(UiEvent.ShowSnackbar("Kelas '$name' berhasil dibuat!"))
                }
                .onFailure { error ->
                    _stateFlow.update { it.copy(isLoading = false, error = error.message) }
                    _uiEventFlow.emit(UiEvent.ShowSnackbar("Gagal membuat kelas: ${error.message}"))
                }
        }
    }

    private fun updateClass(classId: String, newName: String) {
        viewModelScope.launch {
            val accountId = sessionManager.getCurrentAccountId() ?: return@launch
            val existing = _stateFlow.value.classes.find { it.id == classId } ?: return@launch

            _stateFlow.update { it.copy(isLoading = true, classToEdit = null) }

            val updated = existing.copy(name = newName)
            schoolRepository.saveClass(accountId, existing.schoolId, updated)
                .onSuccess {
                    _stateFlow.update { it.copy(isLoading = false) }
                }
                .onFailure { error ->
                    _stateFlow.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    private fun deleteClass(classId: String) {
        viewModelScope.launch {
            val accountId = sessionManager.getCurrentAccountId() ?: return@launch
            val schoolId = sessionManager.getActiveSchoolId() ?: return@launch

            _stateFlow.update { it.copy(isLoading = true, classToDelete = null) }

            schoolRepository.deleteClass(accountId, schoolId, classId)
                .onSuccess {
                    _stateFlow.update { it.copy(isLoading = false) }
                }
                .onFailure { error ->
                    _stateFlow.update { it.copy(isLoading = false, error = error.message) }
                    _uiEventFlow.emit(UiEvent.ShowSnackbar(error.message ?: "Gagal menghapus kelas"))
                }
        }
    }

    private fun syncClasses() {
        viewModelScope.launch(Dispatchers.IO) {
            val uid = sessionManager.getCurrentAccountId() ?: return@launch
            val sid = sessionManager.getActiveSchoolId() ?: return@launch

            _stateFlow.update { it.copy(isLoading = true) }
            _uiEventFlow.emit(UiEvent.ShowSnackbar("Sedang menyinkronkan data kelas..."))

            schoolRepository.syncClasses(uid, sid)
                .onSuccess {
                    _stateFlow.update { it.copy(isLoading = false) }
                    _uiEventFlow.emit(UiEvent.ShowSnackbar("Data kelas berhasil diperbarui!"))
                }
                .onFailure { error ->
                    _stateFlow.update { it.copy(isLoading = false, error = error.message) }
                    _uiEventFlow.emit(UiEvent.ShowSnackbar("Gagal sinkron kelas: ${error.message}"))
                }
        }
    }

    private fun addStudentToClass(classId: String, studentId: String) {
        viewModelScope.launch {
            val schoolId = sessionManager.getActiveSchoolId() ?: return@launch
            _stateFlow.update { it.copy(isLoading = true) }

            android.util.Log.d("DATA_HUNT", "📱 UI: Adding Student $studentId to Class $classId")

            schoolRepository.addStudentToClass(schoolId, classId, studentId)
                .onSuccess {
                    android.util.Log.d("DATA_HUNT", "☁ UI: Firebase Update Success? true")

                    // ✅ TRIGGER PUSH TO UPDATE 'classIds' IN STUDENT DOCUMENT
                    viewModelScope.launch {
                        studentRepository.pushPendingProfiles()
                            .onSuccess { android.util.Log.d("CLASS_SYNC", "✅ Profile pushed with full class list") }
                            .onFailure { err -> android.util.Log.e("CLASS_SYNC", "❌ Push failed: ${err.message}") }
                    }

                    _stateFlow.update { it.copy(isLoading = false) }
                    _uiEventFlow.emit(UiEvent.ShowSnackbar("Siswa berhasil ditambahkan ke kelas!"))
                    loadClasses()
                }
                .onFailure { error ->
                    android.util.Log.d("DATA_HUNT", "☁ UI: Firebase Update Success? false, Error: ${error.message}")
                    _stateFlow.update { it.copy(isLoading = false, error = error.message) }
                    _uiEventFlow.emit(UiEvent.ShowSnackbar("Gagal menambahkan siswa: ${error.message}"))
                }
        }
    }
}
