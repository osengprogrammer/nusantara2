package com.azuratech.azuratime.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.data.local.FaceEntity
import com.azuratech.azuratime.data.local.FaceWithDetails
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.domain.assignment.usecase.AssignStudentToClassUseCase
import com.azuratech.azuratime.domain.assignment.usecase.RemoveStudentFromClassUseCase
import com.azuratech.azuratime.domain.classes.usecase.GetClassesUseCase
import com.azuratech.azuratime.domain.face.usecase.DeleteFaceUseCase
import com.azuratech.azuratime.domain.face.usecase.GetFacesWithDetailsUseCase
import com.azuratech.azuratime.domain.face.usecase.UpdateFaceUseCase
import com.azuratech.azuraengine.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

import com.azuratech.azuratime.domain.student.usecase.DeleteStudentUseCase
import com.azuratech.azuratime.domain.student.usecase.UpdateStudentClassUseCase

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class FaceListViewModel @Inject constructor(
    private val getFacesWithDetailsUseCase: GetFacesWithDetailsUseCase,
    private val updateFaceUseCase: UpdateFaceUseCase,
    private val deleteFaceUseCase: DeleteFaceUseCase,
    private val getClassesUseCase: GetClassesUseCase,
    private val assignStudentToClassUseCase: AssignStudentToClassUseCase,
    private val removeStudentFromClassUseCase: RemoveStudentFromClassUseCase,
    private val updateStudentClassUseCase: UpdateStudentClassUseCase,
    private val deleteStudentUseCase: DeleteStudentUseCase,
    private val sessionManager: com.azuratech.azuratime.core.session.SessionManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedClassName = MutableStateFlow<String?>(null)
    private val _editingStudent = MutableStateFlow<FaceWithDetails?>(null)
    private val _assigningStudent = MutableStateFlow<FaceEntity?>(null)
    private val _deletingStudentId = MutableStateFlow<String?>(null)
    private val _refreshTrigger = MutableStateFlow(System.currentTimeMillis())

    private val _uiEvent = MutableSharedFlow<com.azuratech.azuratime.ui.core.UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        // Refresh when school changes
        sessionManager.activeSchoolIdFlow
            .onEach { loadStudents() }
            .launchIn(viewModelScope)
    }

    fun loadStudents() {
        println("🔄 ViewModel: Refreshing student list...")
        _refreshTrigger.value = System.currentTimeMillis()
    }

    // Data flows from UseCases
    private val _allFacesFlow = _refreshTrigger.flatMapLatest {
        getFacesWithDetailsUseCase()
    }

    private val _allClassesFlow = sessionManager.activeSchoolIdFlow
        .filterNotNull()
        .flatMapLatest { schoolId -> 
            getClassesUseCase(schoolId) 
        }
        .map { result ->
            when(result) {
                is Result.Success -> result.data
                else -> emptyList()
            }
        }

    val allClasses: StateFlow<List<ClassModel>> = _allClassesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // The "Search Machine" combines all data sources with the search query
    private val _filteredStudents = combine(
        _searchQuery.debounce(300),
        _selectedClassName,
        _allFacesFlow,
        _refreshTrigger
    ) { query, className, facesResult, _ ->
        val faces = facesResult.getOrNull() ?: emptyList()
        faces.filter { face ->
            val matchesQuery = if (query.isBlank()) true else face.face.name.contains(query, ignoreCase = true)
            val matchesClass = if (className == null) true else face.className?.contains(className, ignoreCase = true) == true
            matchesQuery && matchesClass
        }
    }

    // This flow creates the display-ready items
    private val _studentDisplayItems = _filteredStudents.map { students ->
        students.distinctBy { it.face.faceId }.map { student ->
            StudentDisplayItem(
                faceWithDetails = student,
                assignedClassNames = student.className ?: "Belum ada kelas",
                isBiometricReady = student.face.photoUrl?.let { it.startsWith("http") || File(it).exists() } == true,
                assignedClassIds = student.classIds
            )
        }
    }

    val uiState: StateFlow<FaceListUiState> = combine(
        _studentDisplayItems,
        _allClassesFlow,
        _searchQuery,
        _selectedClassName,
        _editingStudent,
        _assigningStudent,
        _deletingStudentId
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val students = args[0] as List<StudentDisplayItem>
        @Suppress("UNCHECKED_CAST")
        val allClasses = args[1] as List<ClassModel>
        val query = args[2] as String
        val className = args[3] as String?
        @Suppress("UNCHECKED_CAST")
        val editing = args[4] as FaceWithDetails?
        @Suppress("UNCHECKED_CAST")
        val assigning = args[5] as FaceEntity?
        @Suppress("UNCHECKED_CAST")
        val deletingId = args[6] as String?

        FaceListUiState.Success(
            FaceListData(
                students = students,
                allClasses = allClasses,
                searchQuery = query,
                selectedClassName = className,
                studentForQuickEdit = editing,
                studentForClassAssignment = assigning,
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

    fun onClassFilterChanged(className: String?) {
        _selectedClassName.value = className
    }

    fun onEditStudentClicked(student: FaceWithDetails) {
        _editingStudent.value = student
    }

    fun onAssignClassesClicked(student: FaceEntity) {
        _assigningStudent.value = student
    }

    fun onDismissDialog() {
        _editingStudent.value = null
        _assigningStudent.value = null
        _deletingStudentId.value = null
    }

    fun onSaveChanges(updatedFace: FaceEntity) {
        viewModelScope.launch {
            updateFaceUseCase(updatedFace)
            onDismissDialog()
        }
    }

    fun requestDeleteStudent(studentId: String) {
        println("🗑️ ViewModel: Requesting deletion for studentId=$studentId")
        _deletingStudentId.value = studentId
    }

    fun cancelDeleteStudent() {
        _deletingStudentId.value = null
    }

    fun confirmDeleteStudent() {
        viewModelScope.launch {
            val studentId = _deletingStudentId.value ?: return@launch
            println("🗑️ ViewModel: Confirming deletion for studentId=$studentId")
            
            val currentState = uiState.value
            val faceId = if (currentState is FaceListUiState.Success) {
                currentState.data.students.find { it.faceWithDetails.face.studentId == studentId }?.faceWithDetails?.face?.faceId
            } else null
            
            if (faceId != null) {
                val result = deleteStudentUseCase(studentId, faceId)
                if (result is Result.Success) {
                    _uiEvent.emit(com.azuratech.azuratime.ui.core.UiEvent.ShowSnackbar("Siswa berhasil dihapus"))
                    loadStudents()
                } else if (result is Result.Failure) {
                    _uiEvent.emit(com.azuratech.azuratime.ui.core.UiEvent.ShowSnackbar("Gagal hapus: ${result.error.message}"))
                }
            } else {
                android.util.Log.e("FaceListViewModel", "❌ FaceId not found for studentId=$studentId")
            }
            onDismissDialog()
        }
    }

    fun onAssignStudentToClass(studentId: String, classId: String) {
        viewModelScope.launch {
            println("🎓 ViewModel: Assigning studentId=$studentId to classId=$classId")
            
            val activeSchoolId = sessionManager.getActiveSchoolId()
            val result = updateStudentClassUseCase(studentId, classId, activeSchoolId)
            
            if (result is Result.Success) {
                _uiEvent.emit(com.azuratech.azuratime.ui.core.UiEvent.ShowSnackbar("Kelas berhasil diperbarui"))
                loadStudents()
                onDismissDialog()
            } else if (result is Result.Failure) {
                _uiEvent.emit(com.azuratech.azuratime.ui.core.UiEvent.ShowSnackbar("Gagal update kelas: ${result.error.message}"))
            }
        }
    }

    fun onToggleStudentClassAssignment(studentId: String, classId: String, isChecked: Boolean) {
        viewModelScope.launch {
            try {
                if (isChecked) {
                    onAssignStudentToClass(studentId, classId)
                } else {
                    removeStudentFromClassUseCase(studentId, classId)
                    loadStudents()
                }
            } catch (e: Exception) {
                android.util.Log.e("FaceListViewModel", "Gagal merubah kelas: ${e.message}")
            }
        }
    }
}
