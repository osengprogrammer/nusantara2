package com.azuratech.azuratime.ui.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.toProfile
import com.azuratech.azuratime.data.repo.AuditLogRepository
import com.azuratech.azuratime.domain.model.AuditLogProfile
import com.azuratech.azuratime.ui.core.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * 🛠️ AUDIT LOG VIEW MODEL - Reactive SSOT Migration
 */
@HiltViewModel
class AuditLogViewModel @Inject constructor(
    private val auditLogRepository: AuditLogRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val auditLogs: StateFlow<List<AuditLogProfile>> = 
        sessionManager.activeSchoolIdFlow.filterNotNull()
            .flatMapLatest { schoolId -> 
                auditLogRepository.observeLogsBySchool(schoolId)
                    .map { entities -> entities.map { it.toProfile() } }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
