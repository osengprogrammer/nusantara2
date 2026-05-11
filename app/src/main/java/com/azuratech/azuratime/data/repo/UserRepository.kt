package com.azuratech.azuratime.data.repo

import com.azuratech.azuratime.data.local.*
import com.azuratech.azuratime.domain.checkin.model.AttendanceConflict
import com.azuratech.azuratime.domain.model.MembershipStatus
import com.azuratech.azuratime.domain.model.SyncStatus
import com.azuratech.azuratime.core.sync.SyncManager
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuraengine.result.AppError
import androidx.room.withTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🏰 USER REPOSITORY
 * Thin wrapper for User Data Sources.
 */
@Singleton
class StaffAccountRepository @Inject constructor(
    private val database: AppDatabase,
    private val syncManager: com.azuratech.azuratime.core.sync.SyncManager,
    private val firestore: com.google.firebase.firestore.FirebaseFirestore,
    private val sessionManager: com.azuratech.azuratime.core.session.SessionManager,
    private val schoolRepository: SchoolRepository,
    private val schoolRemoteDataSource: com.azuratech.azuratime.data.remote.SchoolRemoteDataSource
) {
    private val userDao = database.userDao()
    private val userClassAccessDao = database.userClassAccessDao()

    /**
     * 🔥 SSOT: Sync user profile from Cloud to Local.
     */
    suspend fun syncUser(userId: String): Result<UserEntity> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            // Logic from SyncUserUseCase
            var snapshot = com.google.android.gms.tasks.Tasks.await(firestore.collection("whitelisted_users").document(userId).get())
            var pathResolved = "whitelisted_users"

            if (!snapshot.exists()) {
                snapshot = com.google.android.gms.tasks.Tasks.await(firestore.collection("accounts").document(userId).get())
                pathResolved = "accounts"
            }

            if (!snapshot.exists()) {
                return@withContext Result.Failure(AppError.BusinessRule("User not found in cloud"))
            }

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
                try {
                    val schoolsSnapshot = com.google.android.gms.tasks.Tasks.await(firestore.collection("accounts").document(userId).collection("schools").get())
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

            // 2. AUTO-SYNC SCHOOLS
            val schoolIds = user.memberships.keys.toList()
            if (schoolIds.isNotEmpty()) {
                val remoteSchoolsResult = schoolRemoteDataSource.getSchoolsByIds(schoolIds)
                if (remoteSchoolsResult is Result.Success) {
                    remoteSchoolsResult.data.forEach { school ->
                        schoolRepository.saveSchoolLocally(school)
                    }
                }
            }

            Result.Success(user)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    /**
     * 🔥 Update membership status locally in Room.
     * Follows SSOT: writes to DB first, triggers sync as side-effect.
     */
    suspend fun updateMembership(
        userId: String, 
        schoolId: String, 
        schoolName: String,
        status: MembershipStatus, 
        role: String
    ) {
        database.withTransaction {
            val user = userDao.getUserById(userId) ?: return@withTransaction
            
            val updatedMemberships = user.memberships.toMutableMap()
            
            when (status) {
                MembershipStatus.LEFT -> updatedMemberships.remove(schoolId)
                else -> {
                    updatedMemberships[schoolId] = Membership(
                        schoolName = schoolName.ifBlank { user.memberships[schoolId]?.schoolName ?: "" },
                        role = role
                    )
                }
            }
            
            val updatedUser = user.copy(
                memberships = updatedMemberships,
                syncStatus = SyncStatus.PENDING_UPDATE.name
            )
            
            userDao.updateUser(updatedUser)
            println("💾 Room: Updated membership for school $schoolId (Status: $status)")
            
            // Trigger background sync
            syncManager.enqueueProfileSync(userId)
        }
    }

    /**
     * 🔥 Approve a membership request locally.
     */
    suspend fun approveMembership(
        userId: String, 
        schoolId: String, 
        schoolName: String, 
        role: String,
        assignedClassIds: List<String> = emptyList()
    ) {
        database.withTransaction {
            val user = userDao.getUserById(userId) ?: return@withTransaction
            
            val updatedMemberships = user.memberships.toMutableMap()
            updatedMemberships[schoolId] = Membership(
                schoolName = schoolName,
                role = role,
                assignedClassIds = assignedClassIds
            )
            
            val updatedUser = user.copy(
                memberships = updatedMemberships,
                syncStatus = SyncStatus.PENDING_UPDATE.name
            )
            
            userDao.updateUser(updatedUser)

            // Update class access if provided
            if (assignedClassIds.isNotEmpty()) {
                userClassAccessDao.clearAllAccessForUserInSchool(userId, schoolId)
                assignedClassIds.forEach { cid ->
                    userClassAccessDao.insertAccess(UserClassAccessEntity(userId = userId, classId = cid, schoolId = schoolId))
                }
            }
            
            // Trigger background sync
            syncManager.enqueueProfileSync(userId)
        }
    }

    /**
     * 🔥 Revoke membership access locally.
     */
    suspend fun revokeMembership(userId: String, schoolId: String) {
        updateMembership(userId, schoolId, "", MembershipStatus.LEFT, "")
    }

    /**
     * 🔥 Mark user profile as fully synced with Firestore.
     */
    suspend fun markUserSynced(userId: String) {
        val user = userDao.getUserById(userId) ?: return
        userDao.updateUser(user.copy(syncStatus = SyncStatus.SYNCED.name))
        println("✅ Room: User profile $userId marked as SYNCED")
    }

    /**
     * 🔥 SSOT: Push user profile updates to Firestore.
     */
    suspend fun pushUser(userId: String): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val user = userDao.getUserById(userId) ?: return@withContext Result.Failure(AppError.LocalDB("User not found: $userId"))

            if (user.syncStatus == SyncStatus.SYNCED.name) {
                return@withContext Result.Success(Unit)
            }

            // Map memberships to Firestore format
            val membershipsData = user.memberships.mapValues { (_, membership) ->
                mapOf(
                    "schoolName" to membership.schoolName,
                    "role" to membership.role,
                    "assignedClassIds" to membership.assignedClassIds
                )
            }

            val updateData = mapOf(
                "memberships" to membershipsData,
                "activeSchoolId" to user.activeSchoolId,
                "status" to user.status,
                "isActive" to user.isActive,
                "activeClassId" to user.activeClassId,
                "role" to user.role,
                "lastUpdated" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            // Push to Firestore
            com.google.android.gms.tasks.Tasks.await(
                firestore.collection("whitelisted_users").document(userId).update(updateData)
            )

            // Success: Mark as synced
            markUserSynced(userId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    /**
     * Observe a specific user as a Flow of UserEntity.
     */
    fun observeUserEntity(userId: String) = userDao.observeUserById(userId)

    suspend fun getUserById(userId: String) = userDao.getUserById(userId)

    suspend fun updateUser(user: com.azuratech.azuraengine.model.User): Result<Unit> = try {
        val entity = userDao.getUserById(user.userId)
        if (entity != null) {
            userDao.updateUser(entity.copy(
                name = user.name,
                email = user.email,
                activeSchoolId = user.activeSchoolId,
                activeClassId = user.activeClassId,
                syncStatus = SyncStatus.PENDING_UPDATE.name
            ))
            syncManager.enqueueProfileSync(user.userId)
        }
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(AppError.LocalDB(e.message))
    }

    suspend fun updateUser(entity: UserEntity) = try {
        userDao.updateUser(entity)
        syncManager.enqueueProfileSync(entity.userId)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(AppError.LocalDB(e.message))
    }

    suspend fun searchUserByEmail(email: String): Result<UserEntity?> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val snapshot = com.google.android.gms.tasks.Tasks.await(
                firestore.collection("whitelisted_users")
                    .whereEqualTo("email", email.lowercase().trim())
                    .limit(1)
                    .get()
            )

            if (snapshot.isEmpty) {
                return@withContext Result.Success(null)
            }

            val doc = snapshot.documents.first()
            val userId = doc.id
            
            // Sync user to Room first to maintain SSOT
            val syncResult = syncUser(userId)
            if (syncResult is Result.Success) {
                Result.Success(syncResult.data)
            } else {
                Result.Success(null)
            }
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    suspend fun searchByEmail(email: String): UserEntity? {
        // Simple mock/local search for now, or cloud pull if needed
        return null 
    }

    suspend fun sendFriendRequest(myId: String, myName: String, myEmail: String, targetEmail: String): Boolean {
        // Logic to send friend request
        return true
    }

    suspend fun acceptFriendRequest(myId: String, friendId: String) {
        // Logic to accept
    }

    suspend fun rejectFriendRequest(myId: String, friendId: String) {
        // Logic to reject
    }

    // Delegation
    fun getUserDao() = userDao
    fun getUserClassAccessDao() = userClassAccessDao

    // State flow for conflicts
    private val _conflicts = MutableStateFlow<List<AttendanceConflict>>(emptyList())
    val conflicts: StateFlow<List<AttendanceConflict>> = _conflicts.asStateFlow()
    
    fun setConflicts(list: List<AttendanceConflict>) { _conflicts.value = list }
}

typealias UserRepository = StaffAccountRepository
