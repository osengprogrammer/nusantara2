package com.azuratech.azuratime.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.data.local.ClassEntity
import com.azuratech.azuratime.data.local.FaceEntity
import com.azuratech.azuratime.data.local.FaceWithDetails
import com.azuratech.azuratime.data.repository.ClassRepository
import com.azuratech.azuratime.data.repository.FaceAssignmentRepository
import com.azuratech.azuratime.domain.face.usecase.DeleteFaceUseCase
import com.azuratech.azuratime.domain.face.usecase.GetFacesWithDetailsUseCase
import com.azuratech.azuratime.domain.face.usecase.UpdateFaceUseCase
import com.azuratech.azuratime.domain.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class FaceListViewModel @Inject constructor(
    private val getFacesWithDetailsUseCase: GetFacesWithDetailsUseCase,
    private val updateFaceUseCase: UpdateFaceUseCase,
    private val deleteFaceUseCase: DeleteFaceUseCase,
    private val classRepository: ClassRepository,
    private val assignmentRepository: FaceAssignmentRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedClassName = MutableStateFlow<String?>(null)
    private val _editingStudent = MutableStateFlow<FaceWithDetails?>(null)
    private val _assigningStudent = MutableStateFlow<FaceEntity?>(null)

    // Data flows from UseCases
    private val _allFacesFlow = getFacesWithDetailsUseCase()
    private val _allClassesFlow = classRepository.allClasses

    // The "Search Machine" combines all data sources with the search query
    private val _filteredStudents = combine(
        _searchQuery.debounce(300),
        _selectedClassName,
        _allFacesFlow
    ) { query, className, facesResult ->
        val faces = facesResult.getOrNull() ?: emptyList()
        faces.filter { face ->
            val matchesQuery = if (query.isBlank()) true else face.face.name.contains(query, ignoreCase = true)
            val matchesClass = if (className == null) true else face.className == className
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
                assignedClassIds = student.classId?.let { listOf(it) } ?: emptyList()
            )
        }
    }

    val uiState: StateFlow<FaceListUiState> = combine(
        _studentDisplayItems,
        _allClassesFlow,
        _searchQuery,
        _selectedClassName,
        _editingStudent,
        _assigningStudent
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val students = args[0] as List<StudentDisplayItem>
        @Suppress("UNCHECKED_CAST")
        val allClasses = args[1] as List<ClassEntity>
        val query = args[2] as String
        val className = args[3] as String?
        @Suppress("UNCHECKED_CAST")
        val editing = args[4] as FaceWithDetails?
        @Suppress("UNCHECKED_CAST")
        val assigning = args[5] as FaceEntity?

        FaceListUiState(
            isLoading = false, // Can be enhanced later
            students = students,
            allClasses = allClasses,
            searchQuery = query,
            selectedClassName = className,
            studentForQuickEdit = editing,
            studentForClassAssignment = assigning
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FaceListUiState()
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
    }

    fun onSaveChanges(updatedFace: FaceEntity) {
        viewModelScope.launch {
            updateFaceUseCase(updatedFace)
            onDismissDialog()
        }
    }

    fun onDeleteStudent(student: FaceEntity) {
        viewModelScope.launch {
            val result = deleteFaceUseCase(student.faceId)
            if (result is Result.Failure) {
                android.util.Log.e("FaceListViewModel", "Gagal hapus: ${result.error.message}")
            }
        }
    }

    fun onToggleStudentClassAssignment(studentId: String, classId: String, isAssigned: Boolean) {
        viewModelScope.launch {
            try {
                if (isAssigned) {
                    assignmentRepository.assignToClass(studentId, classId)
                } else {
                    assignmentRepository.removeSpecificAssignment(studentId, classId)
                }
            } catch (e: Exception) {
                android.util.Log.e("FaceListViewModel", "Gagal merubah kelas: ${e.message}")
            }
        }
    }
}
