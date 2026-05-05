package com.azuratech.azuratime.domain.user.usecase

import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuratime.data.local.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * UseCase to fetch a user entity from the local database.
 * Used for write operations that require a fetch-copy-update cycle.
 */
class GetUserByIdUseCase @Inject constructor(
    private val database: AppDatabase
) {
    private val userDao = database.userDao()

    suspend operator fun invoke(userId: String): UserEntity? = withContext(Dispatchers.IO) {
        userDao.getUserById(userId)
    }
}
