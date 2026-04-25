package com.azuratech.azuratime.domain.user.usecase

import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuratime.data.local.UserEntity
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * UseCase to update user profile information.
 */
class UpdateUserUseCase @Inject constructor(
    private val database: AppDatabase,
    private val db: FirebaseFirestore
) {
    private val userDao = database.userDao()

    suspend operator fun invoke(user: UserEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            userDao.updateUser(user)
            syncToCloud(user)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    private suspend fun syncToCloud(user: UserEntity) {
        val data = hashMapOf(
            "userId" to user.userId,
            "email" to user.email,
            "name" to user.name,
            "activeSchoolId" to user.activeSchoolId,
            "activeClassId" to user.activeClassId,
            "status" to user.status,
            "isActive" to user.isActive,
            "lastUpdated" to FieldValue.serverTimestamp()
        )
        db.collection("whitelisted_users").document(user.userId).set(data, SetOptions.merge()).await()
    }
}
