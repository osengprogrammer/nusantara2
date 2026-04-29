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
import com.azuratech.azuraengine.model.User
import com.azuratech.azuratime.domain.user.usecase.ObserveUserUseCase
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.ui.util.UiState
import com.azuratech.azuratime.ui.core.UiEvent
import com.azuratech.azuratime.data.local.UserEntity
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
    private val observeUserUseCase: ObserveUserUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    // 🔥 NEW: Reactive School ID Flow
    private val activeSchoolIdFlow = sessionManager.activeSchoolIdFlow
        .onStart { 
            val initial = savedStateHandle.get<String>("schoolId") ?: sessionManager.getActiveSchoolId()
            emit(initial) 
        }
        .filterNotNull()
        .distinctUntilChanged()
        .onEach { println("🔍 ClassViewModel: activeSchoolId='$it'") }

    private val schoolId: String 
        get() = sessionManager.getActiveSchoolId() ?: ""
        
    private val accountId: String = savedStateHandle.get<String>("accountId")
        ?: sessionManager.getCurrentUserId() ?: ""

    // 🔥 User Flow for UI
    val user: StateFlow<User?> = sessionManager.currentUserIdFlow
        .filterNotNull()
        .flatMapLatest { observeUserUseCase(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // =====================================================
    // 📊 CLASS FLOWS (State Management)
    // =====================================================

    val uiState: StateFlow<UiState<List<ClassModel>>> = activeSchoolIdFlow
        .flatMapLatest { id -> getClassesUseCase(id) }
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

    fun createClass(name: String, schoolId: String? = null, grade: String = "") {
        val targetSchoolId = schoolId ?: this.schoolId
        println("💾 DEBUG: ViewModel creating class: $name for schoolId=$targetSchoolId")
        viewModelScope.launch {
            val result = createClassUseCase(accountId, name, targetSchoolId)
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
        viewModelScope.launch {
            // Update class in pool (schoolId is optional/null here for pool update)
            updateClassUseCase(accountId, null, newName, classId)
        }
    }

    fun importClassesFromCsv(uri: Uri, onComplete: () -> Unit) {
        viewModelScope.launch {
            importClassesUseCase(uri.toString())
            onComplete()
        }
    }

    fun deleteClass(
        classId: String,
        onFailure: (String) -> Unit = {},
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            // schoolId is now less critical for delete if it's in a pool, 
            // but we might still want to check if it's used in ANY school
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
            // This now adds/updates assignment
            reassignClassUseCase(accountId, classId, newSchoolId)
        }
    }
}
