package com.azuratech.azuratime.features.account.domain.usecase

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * 🚀 UPDATE DISPLAY NAME USECASE (v3.2.0-ai-native)
 */
class UpdateDisplayNameUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    suspend operator fun invoke(accountId: String, newName: String): Result<Unit> {
        return repository.updateDisplayName(accountId, newName)
    }
}
