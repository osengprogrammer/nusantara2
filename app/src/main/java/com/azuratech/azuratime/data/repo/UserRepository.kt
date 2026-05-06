package com.azuratech.azuratime.data.repo

import com.azuratech.azuratime.data.local.*
import com.azuratech.azuratime.domain.checkin.model.AttendanceConflict
import com.azuratech.azuratime.domain.model.MembershipStatus
import com.azuratech.azuratime.domain.model.SyncStatus
import com.azuratech.azuratime.core.sync.SyncManager
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
class UserRepository @Inject constructor(
    private val database: AppDatabase,
    private val syncManager: SyncManager
) {
    private val userDao = database.userDao()
    private val userClassAccessDao = database.userClassAccessDao()

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
     * Observe a specific user as a Flow of UserEntity.
     */
    fun observeUserEntity(userId: String) = userDao.observeUserById(userId)

    // Delegation
    fun getUserDao() = userDao
    fun getUserClassAccessDao() = userClassAccessDao

    // State flow for conflicts
    private val _conflicts = MutableStateFlow<List<AttendanceConflict>>(emptyList())
    val conflicts: StateFlow<List<AttendanceConflict>> = _conflicts.asStateFlow()
    
    fun setConflicts(list: List<AttendanceConflict>) { _conflicts.value = list }
}
