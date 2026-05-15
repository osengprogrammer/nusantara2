package com.azuratech.azuratime.features.reporting.ui.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.reporting.data.repo.AuditLogRepository
import com.azuratech.azuratime.features.reporting.domain.model.SystemAuditTrail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class AuditLogViewModel @Inject constructor(
    private val repository: AuditLogRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val auditLogs: StateFlow<List<SystemAuditTrail>> = sessionManager.activeSchoolIdFlow
        .filterNotNull()
        .flatMapLatest { schoolId ->
            repository.observeAuditLogs(schoolId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
