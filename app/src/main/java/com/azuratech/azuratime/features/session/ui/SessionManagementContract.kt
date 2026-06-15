package com.azuratech.azuratime.features.session.ui

import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.features.account.domain.model.TeacherAssignment
import com.azuratech.azuratime.features.session.data.local.SessionWithDetails
import com.azuratech.azuratime.features.session.data.local.SubjectEntity
import com.azuratech.azuratime.features.session.domain.model.SessionType

data class SessionManagementUiState(
    val isLoading: Boolean = false,
    val subjects: List<SubjectEntity> = emptyList(),
    val sessions: List<SessionWithDetails> = emptyList(),
    val availableClasses: List<ClassModel> = emptyList(),
    val assignments: List<TeacherAssignment> = emptyList(), // 🔥 Matrix Assignments
    val selectedTier: SessionType = SessionType.ACADEMIC,
    val error: String? = null,
)

sealed class SessionManagementUiEvent {
    data class AddSubject(val name: String, val description: String?) : SessionManagementUiEvent()
    data class DeleteSubject(val subject: SubjectEntity) : SessionManagementUiEvent()
    data class SelectTier(val tier: SessionType) : SessionManagementUiEvent()
    data class AddSession(
        val classId: String?,
        val subjectId: String?,
        val sessionType: SessionType,
        val dayOfWeek: Int,
        val startTime: String,
        val endTime: String,
    ) : SessionManagementUiEvent()
    data class DeleteSession(val session: SessionWithDetails) : SessionManagementUiEvent()
    object GenerateFromMatrix : SessionManagementUiEvent() // 🔥 Point C: Auto-generate sessions
}

sealed class SessionManagementUiEffect {
    data class ShowToast(val message: String) : SessionManagementUiEffect()
}
