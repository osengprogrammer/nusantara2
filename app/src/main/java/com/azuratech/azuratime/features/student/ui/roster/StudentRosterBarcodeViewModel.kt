package com.azuratech.azuratime.features.student.ui.roster

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import com.azuratech.azuratime.features.student.domain.repository.StudentRepository
import com.azuratech.azuratime.features.student.ui.components.StudentDisplayItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🎓 STUDENT ROSTER BARCODE VIEW MODEL
 * Manages the state for generating and printing student barcodes.
 */
@HiltViewModel
class StudentRosterBarcodeViewModel @Inject constructor(
    private val studentRepository: StudentRepository,
    private val schoolRepository: SchoolRepository,
    private val sessionManager: SessionManager,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
) : ViewModel() {

    private val _uiStateFlow = MutableStateFlow(StudentRosterBarcodeUiState())
    val uiStateFlow = _uiStateFlow.asStateFlow()

    private val _uiEffectFlow = MutableSharedFlow<StudentRosterUiEffect>()
    val uiEffectFlow = _uiEffectFlow.asSharedFlow()

    init {
        loadData()
    }

    fun onEvent(event: StudentRosterBarcodeUiEvent) {
        when (event) {
            StudentRosterBarcodeUiEvent.LoadData -> loadData()
            is StudentRosterBarcodeUiEvent.ToggleSelection -> {
                val current = _uiStateFlow.value.selectedStudentIds
                val updated = if (current.contains(event.studentId)) current - event.studentId else current + event.studentId
                _uiStateFlow.update { it.copy(selectedStudentIds = updated) }
            }
            StudentRosterBarcodeUiEvent.SelectAll -> {
                val allIds = _uiStateFlow.value.students.map { it.profile.studentId }.toSet()
                _uiStateFlow.update { it.copy(selectedStudentIds = allIds) }
            }
            StudentRosterBarcodeUiEvent.DeselectAll -> {
                _uiStateFlow.update { it.copy(selectedStudentIds = emptySet()) }
            }
            StudentRosterBarcodeUiEvent.ExportSelected -> {
                exportToPdf()
            }
        }
    }

    private fun exportToPdf() {
        val selectedIds = _uiStateFlow.value.selectedStudentIds
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isLoading = true) }

            val schoolId = _uiStateFlow.value.schoolId ?: ""
            val selectedStudents = _uiStateFlow.value.students.filter { it.profile.studentId in selectedIds }

            val file = com.azuratech.azuratime.features.student.util.BarcodePdfGenerator.generateBarcodePdf(
                cacheDir = context.cacheDir,
                schoolId = schoolId,
                students = selectedStudents,
            )

            _uiStateFlow.update { it.copy(isLoading = false) }

            if (file != null) {
                _uiEffectFlow.emit(StudentRosterUiEffect.ExportPdf(file))
            } else {
                _uiEffectFlow.emit(StudentRosterUiEffect.ShowToast("Failed to generate PDF file"))
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            val schoolId = sessionManager.getActiveSchoolId()
            _uiStateFlow.update { it.copy(isLoading = true, schoolId = schoolId) }

            if (schoolId == null) {
                _uiStateFlow.update { it.copy(isLoading = false, error = "School not found") }
                return@launch
            }

            // Combine students and classes for display
            val classesFlow = schoolRepository.observeClassesFlow(schoolId).map { it.getOrNull() ?: emptyList() }
            val studentsFlow = studentRepository.getStudentProfilesFlow().map { it.getOrNull() ?: emptyList() }

            combine(studentsFlow, classesFlow) { profiles, classes ->
                val classMap = classes.associateBy { it.id }
                profiles.map { profile ->
                    StudentDisplayItem(
                        profile = profile,
                        assignedClassNames = profile.classIds.mapNotNull { classMap[it]?.name }.joinToString(", "),
                        isBiometricReady = profile.biometricExists,
                    )
                }
            }.take(1).collect { items ->
                _uiStateFlow.update { it.copy(isLoading = false, students = items) }
            }
        }
    }
}
