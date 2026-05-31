package com.azuratech.azuratime.features.account.domain.usecase

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * 🚀 ASSIGN CLASS TO SUPERVISOR USE CASE (v3.2.0-ai-native)
 * Encapsulates the logic for assigning/unassigning classes to a supervisor account.
 */
class AssignClassToSupervisorUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    suspend operator fun invoke(
        targetAccountId: String,
        schoolId: String,
        classIds: List<String>,
    ): Result<Unit> {
        return repository.assignClassToConnection(targetAccountId, schoolId, classIds)
    }
}
