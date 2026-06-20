package com.azuratech.azuratime.features.template.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.onFailure
import com.azuratech.azuraengine.result.onSuccess
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.template.domain.model.SchoolTemplate
import com.azuratech.azuratime.features.template.domain.repository.TemplateRepository
import com.azuratech.azuratime.features.template.domain.usecase.ApplySchoolTemplateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🚀 TemplateDashboardViewModel.kt (v3.4.0-ai-native)
 * Unified ViewModel adhering to the strict MVI pattern to manage school templates.
 */
@HiltViewModel
class TemplateDashboardViewModel @Inject constructor(
    private val applySchoolTemplateUseCase: ApplySchoolTemplateUseCase,
    private val repository: TemplateRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TemplateDashboardUiState())
    val uiState: StateFlow<TemplateDashboardUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<TemplateDashboardUiEffect>()
    val uiEffect: SharedFlow<TemplateDashboardUiEffect> = _uiEffect.asSharedFlow()

    init {
        onEvent(TemplateDashboardUiEvent.LoadTemplates)
    }

    fun onEvent(event: TemplateDashboardUiEvent) {
        when (event) {
            is TemplateDashboardUiEvent.LoadTemplates -> loadTemplates()
            is TemplateDashboardUiEvent.ApplyTemplate -> applyTemplate(event.template)
        }
    }

    private fun loadTemplates() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repository.fetchSchoolTemplates()
                .onSuccess { templates ->
                    Log.d("TemplateDebug", "Data diterima: ${templates.size} item")

                    val allClassIds = templates.flatMap { it.defaultClassIds }.distinct()
                    val allSubjectIds = templates.flatMap { it.defaultSubjectIds }.distinct()

                    // Fetch details in batch
                    val classTemplates = repository.fetchGlobalClassesByIds(allClassIds)
                        .getOrNull() ?: emptyList()
                    val subjectTemplates = repository.fetchGlobalSubjectsByIds(allSubjectIds)
                        .getOrNull() ?: emptyList()

                    val classMap = classTemplates.associateBy { it.id }
                    val subjectMap = subjectTemplates.associateBy { it.id }

                    val enriched = templates.map { template ->
                        EnrichedSchoolTemplate(
                            template = template,
                            classNames = template.defaultClassIds.mapNotNull { classMap[it]?.name },
                            subjectNames = template.defaultSubjectIds.mapNotNull { subjectMap[it]?.name },
                        )
                    }

                    _uiState.update { it.copy(isLoading = false, templates = enriched) }
                }
                .onFailure { error ->
                    Log.e("TemplateDebug", "Gagal fetch: ${error.message}")
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                    _uiEffect.emit(TemplateDashboardUiEffect.ShowSnackbar("Failed to load templates: ${error.message}"))
                }
        }
    }

    private fun applyTemplate(template: SchoolTemplate) {
        val schoolId = sessionManager.getActiveSchoolId()
        val accountId = sessionManager.getCurrentAccountId()

        if (schoolId == null || accountId == null) {
            viewModelScope.launch {
                _uiEffect.emit(TemplateDashboardUiEffect.ShowSnackbar("Active school or account session not found."))
            }
            return
        }

        _uiState.update { it.copy(isApplying = true, error = null) }
        viewModelScope.launch {
            applySchoolTemplateUseCase(
                schoolId = schoolId,
                ownerId = accountId,
                template = template,
            ).onSuccess {
                _uiState.update { it.copy(isApplying = false) }
                _uiEffect.emit(TemplateDashboardUiEffect.ShowToast("Template applied successfully!"))
                _uiEffect.emit(TemplateDashboardUiEffect.NavigateBack)
            }.onFailure { error ->
                _uiState.update { it.copy(isApplying = false, error = error.message) }
                _uiEffect.emit(TemplateDashboardUiEffect.ShowSnackbar("Failed to apply template: ${error.message}"))
            }
        }
    }
}
