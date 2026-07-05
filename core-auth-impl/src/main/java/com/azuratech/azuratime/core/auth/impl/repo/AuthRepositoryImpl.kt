package com.azuratech.azuratime.core.auth.impl.repo

import com.azuratech.azuratime.core.auth.api.repository.AuthRepository
import com.azuratech.azuratime.core.data.local.account.AccountEntity
import com.azuratech.azuratime.core.domain.repository.account.AccountRepository
import javax.inject.Inject

// Located in :core-auth-impl with dependencies on :core-data
class AuthRepositoryImpl @Inject constructor(
    private val accountRepository: AccountRepository
    // Add your other dependencies here
) : AuthRepository {
    // Your implementation logic
}
