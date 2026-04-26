package com.azuratech.azuratime.ui.school

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.domain.classes.usecase.GetAllClassesUseCase
import com.azuratech.azuratime.domain.classes.usecase.ReassignClassUseCase
import com.azuratech.azuraengine.model.School
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.domain.school.usecase.CreateSchoolUseCase
import com.azuratech.azuratime.domain.school.usecase.DeleteSchoolUseCase
import com.azuratech.azuratime.domain.school.usecase.GetSchoolsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SchoolUiState {
    object Loading : SchoolUiState()
    data class Success(val schools: List<School>) : SchoolUiState()
    data class Error(val error: AppError) : SchoolUiState()
}

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class SchoolViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getSchoolsUseCase: GetSchoolsUseCase,
    private val createSchoolUseCase: CreateSchoolUseCase,
    private val deleteSchoolUseCase: DeleteSchoolUseCase,
    private val getAllClassesUseCase: GetAllClassesUseCase,
    private val reassignClassUseCase: ReassignClassUseCase
) : ViewModel() {

    private val _accountId = MutableStateFlow(savedStateHandle.get<String>("accountId") ?: "")
    val accountId: StateFlow<String> = _accountId.asStateFlow()

    val uiState: StateFlow<SchoolUiState> = _accountId
        .filter { it.isNotEmpty() }
        .flatMapLatest { id ->
            getSchoolsUseCase(id).map { result ->
                when (result) {
                    is Result.Success -> SchoolUiState.Success(result.data)
                    is Result.Failure -> SchoolUiState.Error(result.error)
                    is Result.Loading -> SchoolUiState.Loading
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SchoolUiState.Loading
        )

    // 🔥 Added All Classes flow for selection
    val allAccountClasses: StateFlow<List<ClassModel>> = _accountId
        .filter { it.isNotEmpty() }
        .flatMapLatest { id ->
            getAllClassesUseCase(id).map { result ->
                if (result is Result.Success) result.data else emptyList()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setAccountId(id: String) {
        if (id.isNotEmpty() && _accountId.value != id) {
            _accountId.value = id
        }
    }

    fun createSchool(name: String, timezone: String, selectedClassIds: List<String> = emptyList()) {
        val currentId = _accountId.value
        if (currentId.isEmpty()) return
        
        println("💾 DEBUG: Creating school: $name with ${selectedClassIds.size} classes")
        viewModelScope.launch {
            val result = createSchoolUseCase(currentId, name, timezone)
            if (result is Result.Success) {
                val newSchoolId = result.data
                selectedClassIds.forEach { classId ->
                    reassignClassUseCase(currentId, classId, newSchoolId)
                }
            }
        }
    }

    fun deleteSchool(id: String) {
        val currentId = _accountId.value
        if (currentId.isEmpty()) return
        
        viewModelScope.launch {
            deleteSchoolUseCase(id, currentId)
        }
    }

    // Deprecated but kept for backward compatibility if needed, though we should update callers
    fun addSchool(accountId: String, name: String, timezone: String) {
        setAccountId(accountId)
        createSchool(name, timezone)
    }
}
