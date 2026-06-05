package com.azuratech.azuratime.features.account.domain.usecase

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.account.domain.model.Account
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 🚀 GET ACCOUNT USECASE (v3.2.0-ai-native)
 * Domain logic for retrieving an account by ID.
 */
class GetAccountUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    operator fun invoke(id: String): Flow<Result<Account>> {
        return repository.getAccountFlow(id)
    }
}
