package com.azuratech.azuratime.domain.user.usecase

import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuratime.data.local.UserEntity
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

import com.azuratech.azuraengine.model.User

/**
 * UseCase to observe user details.
 */
class ObserveUserUseCase @Inject constructor(
    private val database: AppDatabase
) {
    private val userDao = database.userDao()

    operator fun invoke(userId: String): Flow<User?> = 
        userDao.observeUserById(userId).map { it?.toDomain() }
}
