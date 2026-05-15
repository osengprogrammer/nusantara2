package com.azuratech.azuratime.features.student.ui.roster

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.school.data.repo.SchoolRepository
import com.azuratech.azuratime.features.student.domain.model.StudentProfile
import com.azuratech.azuratime.features.student.domain.repository.StudentRepository
import com.azuratech.azuratime.features.student.ui.components.StudentDisplayItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class StudentRosterViewModel @Inject constructor(
    private val studentRepository: StudentRepository,
    private val schoolRepository: SchoolRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedClassId = MutableStateFlow<String?>(null)

    private val _allClasses = sessionManager.activeSchoolIdFlow
        .filterNotNull()
        .flatMapLatest { schoolId ->
            schoolRepository.observeClasses(schoolId).map { result ->
                result.getOrNull() ?: emptyList()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _studentProfiles = studentRepository.getStudentProfiles()

    val uiState: StateFlow<StudentRosterUiState> = combine(
        _studentProfiles,
        _allClasses,
        _searchQuery,
        _selectedClassId
    ) { profiles, classes, query, classId ->
        val classMap = classes.associateBy { it.id }
        
        val displayItems = profiles
            .filter { profile ->
                val matchesQuery = profile.name.contains(query, ignoreCase = true) ||
                        (profile.studentCode?.contains(query, ignoreCase = true) ?: false)
                val matchesClass = classId == null || profile.classIds.contains(classId)
                matchesQuery && matchesClass
            }
            .map { profile ->
                val assignedClassNames = profile.classIds
                    .mapNotNull { classId -> classMap[classId]?.name }
                    .joinToString(", ")
                
                StudentDisplayItem(
                    profile = profile,
                    assignedClassNames = assignedClassNames.ifEmpty { "Tanpa Kelas" },
                    isBiometricReady = profile.faceExists
                )
            }

        val selectedClassName = classId?.let { id -> classMap[id]?.name }

        StudentRosterUiState.Success(
            StudentRosterData(
                searchQuery = query,
                selectedClassName = selectedClassName,
                students = displayItems,
                allClasses = classes
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StudentRosterUiState.Loading)

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onClassSelected(classId: String?) {
        _selectedClassId.value = classId
    }

    fun deleteStudent(studentId: String) {
        viewModelScope.launch {
            studentRepository.deleteProfile(studentId)
        }
    }
}
