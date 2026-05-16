package com.azuratech.azuratime.features.biometric.ui.assignment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.core.data.local.AppDatabase
import com.azuratech.azuratime.features.biometric.domain.repository.StudentBiometricRepository
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.features.school.data.repo.SchoolRepository
import com.azuratech.azuraengine.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentAssignmentViewModel @Inject constructor(
    private val biometricRepository: StudentBiometricRepository,
    private val schoolRepository: SchoolRepository,
    private val sessionManager: SessionManager,
    private val database: AppDatabase
) : ViewModel() {

    private val biometricDao = database.biometricDao()

    @OptIn(ExperimentalCoroutinesApi::class)
    val studentRosterFlow: StateFlow<List<com.azuratech.azuratime.core.data.local.StudentBiometricDetails>> =
        sessionManager.activeSchoolIdFlow
            .filterNotNull()
            .flatMapLatest { schoolId ->
                biometricRepository.getStudentsWithDetailsFlow(schoolId)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allAssignedClassesMap: StateFlow<Map<String, List<ClassModel>>> =
        sessionManager.activeSchoolIdFlow
            .filterNotNull()
            .flatMapLatest { schoolId ->
                combine(
                    biometricRepository.getAllAssignmentsFlow(schoolId),
                    schoolRepository.observeClasses(schoolId)
                ) { assignments, classesResult ->
                    val allClasses = classesResult.getOrNull() ?: emptyList()
                    val classMap = allClasses.associateBy { it.id }
                    
                    assignments.groupBy { it.studentId }
                        .mapValues { entry ->
                            entry.value.mapNotNull { classMap[it.classId] }
                        }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val availableClasses = sessionManager.activeSchoolIdFlow
        .filterNotNull()
        .flatMapLatest { schoolId ->
            schoolRepository.observeClasses(schoolId).map { it.getOrNull() ?: emptyList() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun assignToClass(studentId: String, classId: String) {
        viewModelScope.launch {
            biometricRepository.assignStudentToClass(studentId, classId)
        }
    }

    fun removeSpecificAssignment(studentId: String, classId: String) {
        viewModelScope.launch {
            biometricRepository.removeStudentFromClass(studentId, classId)
        }
    }

    fun removeAllAssignmentsForStudent(studentId: String) {
        viewModelScope.launch {
            biometricRepository.removeAllAssignmentsForStudent(studentId)
        }
    }
}
