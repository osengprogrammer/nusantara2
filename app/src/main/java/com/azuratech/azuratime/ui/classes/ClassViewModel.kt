package com.azuratech.azuratime.ui.classes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import com.azuratech.azuraengine.model.School
import com.azuratech.azuratime.domain.school.usecase.GetSchoolsUseCase
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.domain.classes.usecase.DeleteClassUseCase
import com.azuratech.azuratime.domain.classes.usecase.GetClassesUseCase
import com.azuratech.azuratime.domain.classes.usecase.GetAllClassesUseCase
import com.azuratech.azuratime.domain.classes.usecase.CreateClassUseCase
import com.azuratech.azuratime.domain.classes.usecase.UpdateClassUseCase
import com.azuratech.azuratime.domain.classes.usecase.ReassignClassUseCase
import com.azuratech.azuratime.domain.classes.usecase.ImportClassesUseCase
import com.azuratech.azuratime.domain.classes.usecase.GetAvailableClassesUseCase
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.ui.util.UiState
import com.azuratech.azuratime.ui.core.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.net.Uri

/**
 * 🛠️ CLASS VIEW MODEL - Refactored to match School pattern
 */
@HiltViewModel
class ClassViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getClassesUseCase: GetClassesUseCase,
    private val getAllClassesUseCase: GetAllClassesUseCase,
    private val createClassUseCase: CreateClassUseCase,
    private val updateClassUseCase: UpdateClassUseCase,
    private val deleteClassUseCase: DeleteClassUseCase,
    private val reassignClassUseCase: ReassignClassUseCase,
    private val importClassesUseCase: ImportClassesUseCase,
    private val getAvailableClassesUseCase: GetAvailableClassesUseCase,
    private val getSchoolsUseCase: GetSchoolsUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val schoolId: String = savedStateHandle.get<String>("schoolId") 
        ?: sessionManager.getActiveSchoolId() ?: ""
        
    private val accountId: String = savedStateHandle.get<String>("accountId")
        ?: sessionManager.getCurrentUserId() ?: ""

    // =====================================================
    // 📊 CLASS FLOWS (State Management)
    // =====================================================

    private val _uiState = MutableStateFlow<UiState<List<ClassModel>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<ClassModel>>> = getClassesUseCase(schoolId)
        .map { result ->
            when(result) {
                is Result.Success -> {
                    if (result.data.isEmpty()) UiState.Empty
                    else UiState.Success(result.data)
                }
                is Result.Failure -> UiState.Error(result.error.message ?: "Unknown error")
                is Result.Loading -> UiState.Loading
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiState.Loading
        )

    val classes: StateFlow<List<ClassModel>> = uiState.map {
        if (it is UiState.Success) it.data else emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 🔥 Added Available Classes Flow
    val availableClasses: StateFlow<List<String>> = getAvailableClassesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 🔥 Added All Classes for Account Flow
    val allAccountClasses: StateFlow<UiState<List<ClassModel>>> = getAllClassesUseCase(accountId)
        .map { result ->
            when(result) {
                is Result.Success -> if (result.data.isEmpty()) UiState.Empty else UiState.Success(result.data)
                is Result.Failure -> UiState.Error(result.error.message ?: "Unknown error")
                is Result.Loading -> UiState.Loading
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val schools: StateFlow<List<School>> = getSchoolsUseCase(accountId)
        .map { result ->
            if (result is Result.Success) result.data else emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // =====================================================
    // ➕ CRUD OPERATIONS
    // =====================================================

    fun createClass(name: String, grade: String = "") {
        val targetSchoolId = schoolId.ifBlank { schools.value.firstOrNull()?.id ?: "" }
        
        if (targetSchoolId.isBlank()) {
            println("❌ DEBUG: Save failed: No school found for account $accountId")
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowSnackbar("Gagal: Akun ini belum memiliki sekolah. Buat sekolah dulu."))
            }
            return
        }

        println("💾 DEBUG: ViewModel creating class: $name for school: $targetSchoolId")
        viewModelScope.launch {
            val result = createClassUseCase(accountId, targetSchoolId, name)
            when (result) {
                is Result.Success -> {
                    println("✅ DEBUG: Class created successfully")
                    _uiEvent.emit(UiEvent.ShowSnackbar("Kelas '$name' berhasil dibuat!"))
                }
                is Result.Failure -> {
                    println("❌ DEBUG: Save failed: ${result.error}")
                    _uiEvent.emit(UiEvent.ShowSnackbar("Gagal membuat kelas: ${result.error.message}"))
                }
                else -> Unit
            }
        }
    }

    fun addClass(name: String) {
        createClass(name)
    }

    fun updateClass(classId: String, newName: String) {
        if (schoolId.isBlank()) return
        viewModelScope.launch {
            updateClassUseCase(accountId, schoolId, newName, classId)
        }
    }

    fun importClassesFromCsv(uri: Uri, onComplete: () -> Unit) {
        viewModelScope.launch {
            // NOTE: ImportClassesUseCase might also need refactoring if it uses sessionManager
            importClassesUseCase(uri.toString())
            onComplete()
        }
    }

    fun deleteClass(
        classId: String,
        onFailure: (String) -> Unit = {},
        onSuccess: () -> Unit = {}
    ) {
        if (schoolId.isBlank()) return
        viewModelScope.launch {
            val result = deleteClassUseCase(accountId, schoolId, classId)
            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Success -> onSuccess()
                    is Result.Failure -> onFailure(result.error.message ?: "Gagal menghapus kelas")
                    else -> Unit
                }
            }
        }
    }

    fun reassignClass(classId: String, newSchoolId: String) {
        viewModelScope.launch {
            reassignClassUseCase(accountId, classId, newSchoolId)
        }
    }
}
