package com.azuratech.azuratime.features.account.domain.usecase

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.account.data.local.AccountEntity
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * 🚀 SYNC ACCOUNT USECASE (v3.2.0-ai-native)
 */
class SyncAccountUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    suspend operator fun invoke(accountId: String): Result<AccountEntity> {
        return repository.syncAccount(accountId)
    }
}
