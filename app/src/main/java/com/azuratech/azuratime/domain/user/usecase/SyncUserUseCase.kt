package com.azuratech.azuratime.domain.user.usecase

import android.util.Log
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.*
import com.azuratech.azuratime.domain.result.AppError
import com.azuratech.azuratime.domain.result.Result
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * UseCase to synchronize user profile and class access from Cloud to local Room DB.
 */
class SyncUserUseCase @Inject constructor(
    private val database: AppDatabase,
    private val db: FirebaseFirestore,
    private val sessionManager: SessionManager
) {
    private val userDao = database.userDao()
    private val userClassAccessDao = database.userClassAccessDao()

    suspend operator fun invoke(userId: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        try {
            // 1. Fetch User Profile
            val snapshot = db.collection("whitelisted_users").document(userId).get().await()
            if (!snapshot.exists()) return@withContext Result.Failure(AppError.BusinessRule("User not found in cloud"))
            val data = snapshot.data ?: return@withContext Result.Failure(AppError.BusinessRule("Empty user data"))

            @Suppress("UNCHECKED_CAST")
            val rawMemberships = data["memberships"] as? Map<String, Map<String, Any>> ?: emptyMap()
            val parsedMemberships = rawMemberships.mapValues { entry ->
                Membership(
                    schoolName = entry.value["schoolName"] as? String ?: "Unknown",
                    role = entry.value["role"] as? String ?: "USER"
                )
            }

            @Suppress("UNCHECKED_CAST")
            val rawFriends = data["friends"] as? Map<String, Map<String, Any>> ?: emptyMap()
            val parsedFriends = rawFriends.mapValues { entry ->
                FriendConnection(
                    friendName = entry.value["friendName"] as? String ?: "Guru",
                    friendEmail = entry.value["friendEmail"] as? String ?: "",
                    status = entry.value["status"] as? String ?: "UNKNOWN"
                )
            }

            val user = UserEntity(
                userId = userId,
                email = data["email"] as? String ?: "",
                name = data["name"] as? String ?: "User",
                memberships = parsedMemberships,
                friends = parsedFriends,
                activeSchoolId = data["activeSchoolId"] as? String,
                status = data["status"] as? String ?: "PENDING",
                isActive = data["isActive"] as? Boolean ?: true,
                activeClassId = data["activeClassId"] as? String,
                deviceId = data["deviceId"] as? String,
                createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis()
            )

            // Save to Local
            userDao.insertUser(user)

            // 2. Fetch & Sync Class Access
            val schoolId = user.activeSchoolId ?: sessionManager.getActiveSchoolId() ?: ""
            if (schoolId.isNotEmpty()) {
                val cloudClassIds = (data["assignedClassIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

                userClassAccessDao.clearAllAccessForUserInSchool(userId, schoolId)
                for (cid in cloudClassIds) {
                    userClassAccessDao.insertAccess(
                        UserClassAccessEntity(userId = userId, classId = cid, schoolId = schoolId)
                    )
                }
            }

            Result.Success(user)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }
}
