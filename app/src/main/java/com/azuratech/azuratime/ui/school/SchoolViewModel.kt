package com.azuratech.azuratime.ui.school

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.model.School
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.domain.school.usecase.CreateSchoolUseCase
import com.azuratech.azuratime.domain.school.usecase.DeleteSchoolUseCase
import com.azuratech.azuratime.domain.school.usecase.GetSchoolsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SchoolUiState {
    object Loading : SchoolUiState()
    data class Success(val schools: List<School>) : SchoolUiState()
    data class Error(val error: AppError) : SchoolUiState()
}

@HiltViewModel
class SchoolViewModel @Inject constructor(
    private val getSchoolsUseCase: GetSchoolsUseCase,
    private val createSchoolUseCase: CreateSchoolUseCase,
    private val deleteSchoolUseCase: DeleteSchoolUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<SchoolUiState>(SchoolUiState.Loading)
    val uiState: StateFlow<SchoolUiState> = _uiState.asStateFlow()

    fun loadSchools(accountId: String) {
        getSchoolsUseCase(accountId)
            .onEach { result ->
                _uiState.value = when (result) {
                    is Result.Success -> SchoolUiState.Success(result.data)
                    is Result.Failure -> SchoolUiState.Error(result.error)
                    is Result.Loading -> SchoolUiState.Loading
                }
            }
            .launchIn(viewModelScope)
    }

    fun addSchool(accountId: String, name: String, timezone: String) {
        viewModelScope.launch {
            createSchoolUseCase(accountId, name, timezone)
        }
    }

    fun deleteSchool(id: String, accountId: String) {
        viewModelScope.launch {
            deleteSchoolUseCase(id, accountId)
        }
    }
}
