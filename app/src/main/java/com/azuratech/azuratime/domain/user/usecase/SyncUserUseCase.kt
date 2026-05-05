package com.azuratech.azuratime.domain.user.usecase

import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.*
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
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
    private val sessionManager: SessionManager,
    private val syncSchoolsUseCase: com.azuratech.azuratime.domain.school.usecase.SyncSchoolsUseCase
) {
    private val userDao = database.userDao()
    private val userClassAccessDao = database.userClassAccessDao()

    suspend operator fun invoke(userId: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        try {
            // 1. Fetch User Profile (Standardized Resolution Order)
            var snapshot = db.collection("whitelisted_users").document(userId).get().await()
            var pathResolved = "whitelisted_users"

            if (!snapshot.exists()) {
                snapshot = db.collection("accounts").document(userId).get().await()
                pathResolved = "accounts"
            }

            if (!snapshot.exists()) {
                return@withContext Result.Failure(AppError.BusinessRule("User not found in cloud"))
            }

            println("🔍 User resolved via: $pathResolved")
            val data = snapshot.data ?: return@withContext Result.Failure(AppError.BusinessRule("Empty user data"))

            // Standardize memberships mapping
            val membershipsMap = mutableMapOf<String, Membership>()
            if (pathResolved == "whitelisted_users") {
                @Suppress("UNCHECKED_CAST")
                val rawMemberships = data["memberships"] as? Map<String, Map<String, Any>> ?: emptyMap()
                rawMemberships.forEach { (sid, m) ->
                    membershipsMap[sid] = Membership(
                        schoolName = m["schoolName"] as? String ?: "Unknown",
                        role = m["role"] as? String ?: "USER"
                    )
                }
            } else {
                // Fetch from accounts/{userId}/schools subcollection
                try {
                    val schoolsSnapshot = db.collection("accounts").document(userId).collection("schools").get().await()
                    schoolsSnapshot.documents.forEach { doc ->
                        membershipsMap[doc.id] = Membership(
                            schoolName = doc.getString("schoolName") ?: "Unknown",
                            role = doc.getString("role") ?: "USER"
                        )
                    }
                } catch (e: Exception) {
                    println("⚠️ SyncUser: Failed to fetch subcollection memberships: ${e.message}")
                }
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
                memberships = membershipsMap,
                friends = parsedFriends,
                activeSchoolId = data["activeSchoolId"] as? String,
                status = data["status"] as? String ?: "PENDING",
                isActive = data["isActive"] as? Boolean ?: true,
                activeClassId = data["activeClassId"] as? String,
                role = data["role"] as? String ?: "USER",
                deviceId = data["deviceId"] as? String,
                createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis()
            )

            // Save to Local
            userDao.insertUser(user)
            println("✅ SyncUser: Saved to Room -> ${user.userId}, role=${user.role}")

            // 2. 🔥 AUTO-SYNC SCHOOLS
            val schoolIds = user.memberships.keys.toList()
            if (schoolIds.isNotEmpty()) {
                println("🔄 Auto-syncing ${schoolIds.size} schools for user $userId")
                val schoolSyncResult = syncSchoolsUseCase(schoolIds)
                
                if (schoolSyncResult is Result.Success) {
                    println("✅ School sync completed!")
                    // Auto-select first school if none active in session
                    if (sessionManager.getActiveSchoolId() == null) {
                        val firstId = user.activeSchoolId ?: schoolIds.first()
                        sessionManager.saveActiveSchoolId(firstId)
                        println("🚀 AUTO-INIT: Selecting active school: $firstId")
                    }
                }
            }

            // 3. Fetch & Sync Class Access
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

    suspend fun searchByEmail(email: String): UserEntity? = withContext(Dispatchers.IO) {
        try {
            var snapshot = db.collection("whitelisted_users")
                .whereEqualTo("email", email.trim().lowercase())
                .limit(1).get().await()

            var doc = snapshot.documents.firstOrNull()
            var pathResolved = "whitelisted_users"

            if (doc == null) {
                snapshot = db.collection("accounts")
                    .whereEqualTo("email", email.trim().lowercase())
                    .limit(1).get().await()
                doc = snapshot.documents.firstOrNull()
                pathResolved = "accounts"
            }

            if (doc == null) return@withContext null
            println("🔍 User found via: $pathResolved")
            val data = doc.data ?: return@withContext null

            // Standardize memberships mapping
            val membershipsMap = mutableMapOf<String, Membership>()
            if (pathResolved == "whitelisted_users") {
                @Suppress("UNCHECKED_CAST")
                val rawMemberships = data["memberships"] as? Map<String, Map<String, Any>> ?: emptyMap()
                rawMemberships.forEach { (sid, m) ->
                    membershipsMap[sid] = Membership(
                        schoolName = m["schoolName"] as? String ?: "Unknown",
                        role = m["role"] as? String ?: "USER"
                    )
                }
            } else {
                val schoolsSnapshot = db.collection("accounts").document(doc.id).collection("schools").get().await()
                schoolsSnapshot.documents.forEach { sDoc ->
                    membershipsMap[sDoc.id] = Membership(
                        schoolName = sDoc.getString("schoolName") ?: "Unknown",
                        role = sDoc.getString("role") ?: "USER"
                    )
                }
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

            UserEntity(
                userId = doc.id,
                email = data["email"] as? String ?: "",
                name = data["name"] as? String ?: "User",
                memberships = membershipsMap,
                friends = parsedFriends,
                activeSchoolId = data["activeSchoolId"] as? String,
                status = data["status"] as? String ?: "PENDING",
                isActive = data["isActive"] as? Boolean ?: true,
                activeClassId = data["activeClassId"] as? String,
                role = data["role"] as? String ?: "USER",
                deviceId = data["deviceId"] as? String,
                createdAt = (data["createdAt"] as? Long) ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }
}
