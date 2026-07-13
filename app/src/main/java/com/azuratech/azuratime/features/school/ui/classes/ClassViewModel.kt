package com.azuratech.azuratime.features.school.ui.classes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuraengine.result.onFailure
import com.azuratech.azuraengine.result.onSuccess
import com.azuratech.azuratime.core.session.SessionManager
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
    private val studentRepository: StudentRepository,
    private val sessionManager: SessionManager,
    private val templateRepository: com.azuratech.azuratime.features.template.domain.repository.TemplateRepository,
) : ViewModel() {

    private val _uiEffectFlow = MutableSharedFlow<ClassUiEffect>()
    val uiEffectFlow = _uiEffectFlow.asSharedFlow()

    private val _stateFlow = MutableStateFlow(ClassUiState())
    val uiStateFlow: StateFlow<ClassUiState> = _stateFlow.asStateFlow()

    init {
        loadClasses()
        loadAvailableClassesFromTemplate()
        observeClassesReactive()
    }

    private fun loadAvailableClassesFromTemplate() {
        viewModelScope.launch {
            templateRepository.fetchAllGlobalClasses()
                .onSuccess { globalClasses ->
                    val names = globalClasses.map { it.name }.distinct().sorted()
                    val categories = globalClasses.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
                    val majors = globalClasses.map { it.major }.filter { it.isNotBlank() }.distinct().sorted()
                    _stateFlow.update {
                        it.copy(
                            availableClasses = names,
                            availableCategories = categories,
                            availableMajors = majors,
                        )
                    }
                }
                .onFailure {
                    val fallback = listOf(
                        "10-IPA-1", "10-IPA-2", "10-IPA-3",
                        "10-IPS-1", "10-IPS-2", "10-IPS-3",
                        "11-IPA-1", "11-IPA-2", "11-IPA-3",
                        "11-IPS-1", "11-IPS-2", "11-IPS-3",
                        "12-IPA-1", "12-IPA-2", "12-IPA-3",
                        "12-IPS-1", "12-IPS-2", "12-IPS-3",
                    )
                    _stateFlow.update {
                        it.copy(
                            availableClasses = fallback,
                            availableCategories = listOf("SD", "SMP", "SMA", "SMK"),
                            availableMajors = listOf("UMUM", "IPA", "IPS", "BAHASA"),
                        )
                    }
                }
        }
    }

    fun onEvent(event: ClassUiEvent) {
        when (event) {
            is ClassUiEvent.LoadClasses -> loadClasses()
            is ClassUiEvent.CreateClass -> createClass(event.name, event.level, event.category, event.major, event.section)
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
            ClassUiEvent.ToggleInputMode -> _stateFlow.update { it.copy(isStructuredMode = !it.isStructuredMode) }
            is ClassUiEvent.SetSelectedLevel -> _stateFlow.update { it.copy(selectedLevel = event.level) }
            is ClassUiEvent.SetSelectedCategory -> _stateFlow.update { it.copy(selectedCategory = event.category) }
            is ClassUiEvent.SetSelectedMajor -> _stateFlow.update { it.copy(selectedMajor = event.major) }
            is ClassUiEvent.SetSelectedSection -> _stateFlow.update { it.copy(selectedSection = event.section) }
        }
    }

    private fun observeStudentsInClassInternal(classId: String) {
        studentRepository.getStudentProfilesFlow()
            .onEach { result ->
                result.onSuccess { students ->
                    val filtered = students.filter { it.classIds.contains(classId) }
                    _stateFlow.update { it.copy(studentsInClass = filtered) }
                }
            }
            .launchIn(viewModelScope)
    }

    // ✅ NEW FUNCTION: Observe students in a specific class
    fun observeStudentsInClassFlow(classId: String): Flow<List<StudentProfile>> {
        return studentRepository.getStudentProfilesFlow()
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
            activeSchoolIdFlow.flatMapLatest { schoolId -> schoolRepository.observeClassesFlow(schoolId) },
            studentRepository.getStudentProfilesFlow(), // Fetch all students here
        ) { classResult, studentResult ->
            if (classResult is com.azuratech.azuraengine.result.Result.Success && studentResult is com.azuratech.azuraengine.result.Result.Success) {
                val classes = classResult.data
                val allStudents = studentResult.data // Get all students

                val counts = classes.associate { classModel ->
                    classModel.id to allStudents.count { it.classIds.contains(classModel.id) }
                }

                _stateFlow.update { it.copy(classes = classes, studentCountsByClassId = counts, allStudents = allStudents) }
            } else if (classResult is com.azuratech.azuraengine.result.Result.Failure) {
                _stateFlow.update { it.copy(error = classResult.error.message) }
            }
        }.launchIn(viewModelScope)
    }

    private fun createClass(name: String, level: Int, category: String, major: String, section: String) {
        viewModelScope.launch {
            val schoolId = sessionManager.getActiveSchoolId() ?: return@launch
            val accountId = sessionManager.getCurrentAccountId() ?: return@launch

            _stateFlow.update { it.copy(isLoading = true, isAddDialogVisible = false) }

            val classModel = ClassModel(
                id = "cls_${System.currentTimeMillis()}",
                name = name,
                schoolId = schoolId,
                grade = level.toString(), // Use level as grade for backward compatibility
                accountId = null,
                studentCount = 0,
                createdAt = System.currentTimeMillis(),
                // Blueprint fields
                level = level,
                category = category,
                major = major,
                section = section,
            )

            schoolRepository.saveClass(accountId, schoolId, classModel)
                .onSuccess {
                    _stateFlow.update { it.copy(isLoading = false) }
                    _uiEffectFlow.emit(ClassUiEffect.ShowSnackbar("Class '$name' created successfully!"))
                }
                .onFailure { error ->
                    _stateFlow.update { it.copy(isLoading = false, error = error.message) }
                    _uiEffectFlow.emit(ClassUiEffect.ShowSnackbar("Failed to create class: ${error.message}"))
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
                    _uiEffectFlow.emit(ClassUiEffect.ShowSnackbar(error.message ?: "Failed to delete class"))
                }
        }
    }

    private fun syncClasses() {
        viewModelScope.launch(Dispatchers.IO) {
            val uid = sessionManager.getCurrentAccountId() ?: return@launch
            val sid = sessionManager.getActiveSchoolId() ?: return@launch

            _stateFlow.update { it.copy(isLoading = true) }
            _uiEffectFlow.emit(ClassUiEffect.ShowSnackbar("Syncing class data..."))

            schoolRepository.syncClasses(uid, sid)
                .onSuccess {
                    _stateFlow.update { it.copy(isLoading = false) }
                    _uiEffectFlow.emit(ClassUiEffect.ShowSnackbar("Class data updated successfully!"))
                }
                .onFailure { error ->
                    _stateFlow.update { it.copy(isLoading = false, error = error.message) }
                    _uiEffectFlow.emit(ClassUiEffect.ShowSnackbar("Class sync failed: ${error.message}"))
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
                    _uiEffectFlow.emit(ClassUiEffect.ShowSnackbar("Student successfully added to class!"))
                    loadClasses()
                }
                .onFailure { error ->
                    android.util.Log.d("DATA_HUNT", "☁ UI: Firebase Update Success? false, Error: ${error.message}")
                    _stateFlow.update { it.copy(isLoading = false, error = error.message) }
                    _uiEffectFlow.emit(ClassUiEffect.ShowSnackbar("Failed to add student: ${error.message}"))
                }
        }
    }
}
