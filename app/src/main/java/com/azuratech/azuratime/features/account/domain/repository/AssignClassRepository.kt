package com.azuratech.azuratime.features.account.domain.repository

import com.azuratech.azuratime.features.account.domain.model.Account
import com.azuratech.azuratime.features.account.domain.model.TeacherAssignment
import kotlinx.coroutines.flow.Flow

/**
 * 🎓 ASSIGN CLASS REPOSITORY (v3.9.0)
 * Domain interface to decouple Assign Class feature from Room DB details.
 */
interface AssignClassRepository {
    suspend fun getAccountById(id: String): Account?
    fun observeAssignments(accountId: String, schoolId: String): Flow<List<TeacherAssignment>>
}
