package com.azuratech.azuratime.features.template.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.core.result.onFailure
import com.azuratech.azuratime.core.result.onSuccess
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.template.domain.model.SchoolTemplate
import com.azuratech.azuratime.features.template.domain.usecase.ApplySchoolTemplateUseCase
import com.azuratech.azuratime.features.template.domain.usecase.FetchSchoolTemplatesUseCase
import com.azuratech.azuratime.features.template.domain.usecase.FetchGlobalClassesByIdsUseCase
import com.azuratech.azuratime.features.template.domain.usecase.FetchGlobalSubjectsByIdsUseCase
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
    private val fetchSchoolTemplatesUseCase: FetchSchoolTemplatesUseCase,
    private val fetchGlobalClassesByIdsUseCase: FetchGlobalClassesByIdsUseCase,
    private val fetchGlobalSubjectsByIdsUseCase: FetchGlobalSubjectsByIdsUseCase,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiStateFlow = MutableStateFlow(TemplateDashboardUiState())
    val uiStateFlow: StateFlow<TemplateDashboardUiState> = _uiStateFlow.asStateFlow()

    private val _uiEffectFlow = MutableSharedFlow<TemplateDashboardUiEffect>()
    val uiEffectFlow: SharedFlow<TemplateDashboardUiEffect> = _uiEffectFlow.asSharedFlow()

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
        _uiStateFlow.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            fetchSchoolTemplatesUseCase()
                .onSuccess { templates ->
                    Log.d("TemplateDebug", "Data diterima: ${templates.size} item")

                    val allClassIds = templates.flatMap { it.defaultClassIds }.distinct()
                    val allSubjectIds = templates.flatMap { it.defaultSubjectIds }.distinct()

                    // Fetch details in batch
                    val classTemplates = fetchGlobalClassesByIdsUseCase(allClassIds)
                        .getOrNull() ?: emptyList()
                    val subjectTemplates = fetchGlobalSubjectsByIdsUseCase(allSubjectIds)
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

                    _uiStateFlow.update { it.copy(isLoading = false, templates = enriched) }
                }
                .onFailure { error ->
                    Log.e("TemplateDebug", "Gagal fetch: ${error.message}")
                    _uiStateFlow.update { it.copy(isLoading = false, error = error.message) }
                    _uiEffectFlow.emit(TemplateDashboardUiEffect.ShowSnackbar("Failed to load templates: ${error.message}"))
                }
        }
    }

    private fun applyTemplate(template: SchoolTemplate) {
        val schoolId = sessionManager.getActiveSchoolId()
        val accountId = sessionManager.getCurrentAccountId()

        if (schoolId == null || accountId == null) {
            viewModelScope.launch {
                _uiEffectFlow.emit(TemplateDashboardUiEffect.ShowSnackbar("Active school or account session not found."))
            }
            return
        }

        _uiStateFlow.update { it.copy(isApplying = true, error = null) }
        viewModelScope.launch {
            applySchoolTemplateUseCase(
                schoolId = schoolId,
                ownerId = accountId,
                template = template,
            ).onSuccess {
                _uiStateFlow.update { it.copy(isApplying = false) }
                _uiEffectFlow.emit(TemplateDashboardUiEffect.ShowToast("Template applied successfully!"))
                _uiEffectFlow.emit(TemplateDashboardUiEffect.NavigateBack)
            }.onFailure { error ->
                _uiStateFlow.update { it.copy(isApplying = false, error = error.message) }
                _uiEffectFlow.emit(TemplateDashboardUiEffect.ShowSnackbar("Failed to apply template: ${error.message}"))
            }
        }
    }
}
