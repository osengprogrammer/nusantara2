package com.azuratech.azuratime.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.data.local.StudentEntity
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.domain.student.repository.StudentRepository
import com.azuratech.azuratime.domain.model.StudentProfile
import com.azuratech.azuratime.domain.model.SyncStatus
import com.azuratech.azuraengine.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class FaceListViewModel @Inject constructor(
    private val studentRepository: StudentRepository,
    private val schoolRepository: com.azuratech.azuratime.data.repo.SchoolRepository,
    private val sessionManager: com.azuratech.azuratime.core.session.SessionManager,
    private val syncManager: com.azuratech.azuratime.core.sync.SyncManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedClassId = MutableStateFlow<String?>(null)
    private val _editingStudent = MutableStateFlow<StudentProfile?>(null)
    private val _assigningStudent = MutableStateFlow<StudentProfile?>(null)
    private val _deletingStudentId = MutableStateFlow<String?>(null)
    private val _refreshTrigger = MutableStateFlow(System.currentTimeMillis())

    private val _uiEvent = MutableSharedFlow<com.azuratech.azuratime.ui.core.UiEvent>()
    val uiEventFlow = _uiEvent.asSharedFlow()

    init {
        // Refresh when school changes
        sessionManager.activeSchoolIdFlow
            .onEach { schoolId ->
                loadStudents()
                // 🔥 Fresh Install Guard: Trigger sync if local data might be missing
                if (schoolId != null) {
                    syncManager.enqueueSync()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun StudentEntity.toProfile(): StudentProfile = StudentProfile(
        studentId = studentId,
        name = name,
        schoolId = schoolId,
        classIds = listOfNotNull(classId),
        studentCode = studentCode,
        syncStatus = if (isSynced) SyncStatus.SYNCED else SyncStatus.PENDING_UPDATE,
        createdAt = createdAt
    )

    fun loadStudents() {
        println("🔄 ViewModel: Refreshing student list...")
        _refreshTrigger.value = System.currentTimeMillis()
    }

    private val _allClassesFlow = sessionManager.activeSchoolIdFlow
        .filterNotNull()
        .flatMapLatest { schoolId -> 
            schoolRepository.observeClasses(schoolId) 
        }
        .map { result ->
            when(result) {
                is Result.Success -> result.data
                else -> emptyList()
            }
        }

    val allClassesStateFlow: StateFlow<List<ClassModel>> = _allClassesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // SSOT: Direct stream from Repository
    val faceListStateFlow: StateFlow<List<StudentProfile>> = combine(
        _searchQuery.debounce(300),
        _selectedClassId,
        _refreshTrigger.flatMapLatest { studentRepository.getStudentProfiles() }
    ) { query, classId, profiles ->
        profiles.filter { profile ->
            val matchesQuery = if (query.isBlank()) true else profile.name.contains(query, ignoreCase = true)
            val matchesClass = if (classId == null) true else profile.classIds.contains(classId)
            matchesQuery && matchesClass
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val uiStateStateFlow: StateFlow<FaceListUiState> = combine(
        faceListStateFlow,
        allClassesStateFlow,
        _searchQuery,
        _selectedClassId,
        _editingStudent,
        _assigningStudent,
        _deletingStudentId
    ) { args ->
        val query = args[2] as String
        val classId = args[3] as String?
        val deletingId = args[6] as String?

        FaceListUiState.Success(
            FaceListData(
                students = emptyList(), // Screen now observes faceListStateFlow directly
                allClasses = args[1] as List<ClassModel>,
                searchQuery = query,
                selectedClassName = classId,
                studentForQuickEdit = null, // Logic moved to StudentProfile
                studentForClassAssignment = null,
                studentForDeletion = deletingId
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FaceListUiState.Loading
    )

    // --- Event Handlers ---

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onClassFilterChanged(classId: String?) {
        _selectedClassId.value = classId
    }

    fun onEditStudentClicked(student: StudentProfile) {
        _editingStudent.value = student
    }

    fun onAssignClassesClicked(student: StudentProfile) {
        _assigningStudent.value = student
    }

    fun onDismissDialog() {
        _editingStudent.value = null
        _assigningStudent.value = null
        _deletingStudentId.value = null
    }

    fun onSaveChanges(profile: StudentProfile) {
        viewModelScope.launch {
            studentRepository.saveProfile(profile.copy(syncStatus = SyncStatus.PENDING_UPDATE))
            onDismissDialog()
        }
    }

    fun requestDeleteStudent(studentId: String) {
        _deletingStudentId.value = studentId
    }

    fun cancelDeleteStudent() {
        _deletingStudentId.value = null
    }

    fun confirmDeleteStudent() {
        viewModelScope.launch {
            val studentId = _deletingStudentId.value ?: return@launch
            val result = studentRepository.deleteProfile(studentId)
            if (result is Result.Success) {
                _uiEvent.emit(com.azuratech.azuratime.ui.core.UiEvent.ShowSnackbar("Siswa berhasil dihapus"))
                loadStudents()
            } else if (result is Result.Failure) {
                _uiEvent.emit(com.azuratech.azuratime.ui.core.UiEvent.ShowSnackbar("Gagal hapus: ${result.error.message}"))
            }
            onDismissDialog()
        }
    }

    fun onAssignStudentToClass(studentId: String, classId: String) {
        viewModelScope.launch {
            val profiles = faceListStateFlow.value
            val profile = profiles.find { it.studentId == studentId } ?: return@launch
            val updatedProfile = profile.copy(
                classIds = (profile.classIds + classId).distinct(),
                syncStatus = SyncStatus.PENDING_UPDATE
            )
            val result = studentRepository.saveProfile(updatedProfile)
            if (result is Result.Success) {
                _uiEvent.emit(com.azuratech.azuratime.ui.core.UiEvent.ShowSnackbar("Kelas berhasil diperbarui"))
                loadStudents()
                onDismissDialog()
            }
        }
    }

    fun onToggleStudentClassAssignment(studentId: String, classId: String, isChecked: Boolean) {
        viewModelScope.launch {
            val profiles = faceListStateFlow.value
            val profile = profiles.find { it.studentId == studentId } ?: return@launch
            val newClassIds = if (isChecked) {
                (profile.classIds + classId).distinct()
            } else {
                profile.classIds.filter { it != classId }
            }
            
            val updatedProfile = profile.copy(
                classIds = newClassIds,
                syncStatus = SyncStatus.PENDING_UPDATE
            )
            studentRepository.saveProfile(updatedProfile)
            loadStudents()
        }
    }
}

