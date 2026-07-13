package com.azuratech.azuratime.features.account.domain.usecase

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import javax.inject.Inject

import com.azuratech.azuratime.core.domain.model.TeacherAssignment

/**
 * 🚀 ASSIGN CLASS TO SUPERVISOR USE CASE (v3.4.0-matrix)
 * Encapsulates the logic for assigning/unassigning classes and subjects to a supervisor account.
 */
class AssignClassToSupervisorUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    suspend operator fun invoke(
        targetAccountId: String,
        schoolId: String,
        assignments: List<TeacherAssignment>,
    ): Result<Unit> {
        return repository.assignClassToConnection(targetAccountId, schoolId, assignments)
    }
}
