package com.azuratech.azuratime.features.account.domain.repository

import com.azuratech.azuratime.features.account.domain.model.SupervisorAssignment
import kotlinx.coroutines.flow.Flow

/**
 * 🎓 ACCOUNT CLASS ACCESS REPOSITORY (v3.8.0)
 * Domain interface to decouple Account feature from Room DB details.
 */
interface AccountClassAccessRepository {
    fun observeAssignments(accountId: String, schoolId: String): Flow<List<SupervisorAssignment>>
    suspend fun updateAssignments(accountId: String, schoolId: String, assignments: List<SupervisorAssignment>)
}
