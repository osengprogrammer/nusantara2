package com.azuratech.azuratime.data.repository

import android.util.Log
import com.azuratech.azuratime.data.local.*
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.domain.user.usecase.*
import com.azuratech.azuratime.domain.result.Result
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val database: AppDatabase,
    private val db: FirebaseFirestore,
    private val sessionManager: SessionManager,
    private val syncUserUseCase: SyncUserUseCase
) {
    private val userDao = database.userDao()
    private val userClassAccessDao = database.userClassAccessDao()
    private val checkInRecordDao = database.checkInRecordDao()

    private val _conflicts = MutableStateFlow<List<AttendanceConflict>>(emptyList())
    val conflicts = _conflicts.asStateFlow()

    private val schoolId: String
        get() = sessionManager.getActiveSchoolId() ?: ""

    fun observeUserById(userId: String): Flow<UserEntity?> = userDao.observeUserById(userId)

    fun observeClassIdsForUser(userId: String): Flow<List<String>> =
        sessionManager.activeSchoolIdFlow
            .filterNotNull()
            .flatMapLatest { schoolId ->
                userClassAccessDao.observeClassIdsForUser(userId, schoolId)
            }

    fun observeUsersBySchool(targetSchoolId: String): Flow<List<UserEntity>> =
        userDao.observeAllUsers().map { users ->
            users.filter { it.memberships.containsKey(targetSchoolId) }
        }

    suspend fun getLocalUserById(userId: String): UserEntity? = withContext(Dispatchers.IO) {
        userDao.getUserById(userId)
    }

    @Deprecated("Route through SyncUserUseCase")
    suspend fun syncUserFromCloud(userId: String): UserEntity? = syncUserUseCase(userId).getOrNull()

    @Deprecated("Route through SyncUserUseCase")
    suspend fun refreshUserFromCloud(userId: String): UserEntity? = syncUserFromCloud(userId)

    suspend fun getUserByUidFromCloud(uid: String): UserEntity? = withContext(Dispatchers.IO) {
        val snapshot = db.collection("whitelisted_users").document(uid).get().await()
        if (!snapshot.exists()) return@withContext null
        val data = snapshot.data ?: return@withContext null

        Log.d("AZURA_DEBUG", "Raw Cloud Data for $uid: $data")

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

        UserEntity(
            userId = uid,
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
    }

    suspend fun getUserByEmailFromCloud(email: String): UserEntity? = withContext(Dispatchers.IO) {
        val snapshot = db.collection("whitelisted_users")
            .whereEqualTo("email", email.trim().lowercase())
            .limit(1).get().await()

        val doc = snapshot.documents.firstOrNull() ?: return@withContext null
        getUserByUidFromCloud(doc.id)
    }

    suspend fun fetchUserClassesFromCloud(userId: String): List<String> = withContext(Dispatchers.IO) {
        val doc = db.collection("whitelisted_users").document(userId).get().await()
        (doc.get("assignedClassIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
    }

    suspend fun syncUserByEmailFromCloud(email: String): UserEntity? = withContext(Dispatchers.IO) {
        val cloudUser = getUserByEmailFromCloud(email)
        if (cloudUser != null) {
            userDao.insertUser(cloudUser)
        }
        return@withContext cloudUser
    }

    suspend fun syncUserToCloud(user: UserEntity) = withContext(Dispatchers.IO) {
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
        try {
            db.collection("whitelisted_users").document(user.userId).set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.e("UserRepository", "❌ User Sync Fail: ${e.message}")
        }
    }

    suspend fun assignClassToUser(targetId: String, schoolId: String, classId: String) = withContext(Dispatchers.IO) {
        userClassAccessDao.insertAccess(
            UserClassAccessEntity(userId = targetId, classId = classId, schoolId = schoolId)
        )

        try {
            val userRef = db.collection("whitelisted_users").document(targetId)

            db.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                if (snapshot.exists()) {
                    val currentRole = snapshot.getString("memberships.$schoolId.role")
                    val updates = mutableMapOf<String, Any>()

                    updates["assignedClassIds"] = FieldValue.arrayUnion(classId)

                    if (currentRole == null || currentRole == "PENDING" || currentRole == "USER") {
                        updates["memberships.$schoolId.role"] = "TEACHER"
                    }

                    transaction.update(userRef, updates)
                }
            }.await()

            val currentIds = userClassAccessDao.getClassIdsForUser(targetId, schoolId)
            syncUserClassesToCloud(targetId, currentIds)

            Log.i("UserRepository", "✅ Akses kelas diberikan!")
        } catch (e: Exception) {
            Log.e("UserRepository", "🚨 Gagal sync ke Cloud: ${e.message}")
        }
    }

    suspend fun syncUserClassesToCloud(userId: String, classIds: List<String>) = withContext(Dispatchers.IO) {
        val updateData = hashMapOf(
            "assignedClassIds" to classIds,
            "lastClassUpdate" to FieldValue.serverTimestamp()
        )
        db.collection("whitelisted_users").document(userId).set(updateData, SetOptions.merge()).await()
    }

    suspend fun removeClassAccess(targetId: String, classId: String) = withContext(Dispatchers.IO) {
        userClassAccessDao.removeAccess(userId = targetId, classId = classId, schoolId = schoolId)

        val currentIds = userClassAccessDao.getClassIdsForUser(targetId, schoolId)
        try {
            syncUserClassesToCloud(targetId, currentIds)
        } catch (e: Exception) {
            Log.e("UserRepository", "❌ Gagal hapus cloud: ${e.message}")
        }
    }

    suspend fun resolveAttendanceConflict(conflict: AttendanceConflict, useCloud: Boolean) = withContext(Dispatchers.IO) {
        if (useCloud) {
            checkInRecordDao.insert(conflict.cloud)
        }
        _conflicts.value = _conflicts.value.filter { it != conflict }
    }

    suspend fun updateDisplayName(user: UserEntity) = withContext(Dispatchers.IO) {
        userDao.updateUser(user)
        try {
            syncUserToCloud(user)
        } catch (e: Exception) {
            Log.e("UserRepository", "Gagal sync nama: ${e.message}")
        }
    }

    suspend fun updateUserMemberships(
        userId: String,
        newMemberships: Map<String, Membership>,
        activeSchoolId: String? = null
    ) = withContext(Dispatchers.IO) {
        val currentUser = userDao.getUserById(userId) ?: return@withContext
        val updatedUser = currentUser.copy(
            memberships = newMemberships,
            activeSchoolId = activeSchoolId ?: currentUser.activeSchoolId
        )
        userDao.insertUser(updatedUser)
        updateUserMembershipsInCloud(userId, newMemberships, activeSchoolId)
    }

    suspend fun updateUserMembershipsInCloud(userId: String, memberships: Map<String, Membership>, activeSchoolId: String? = null) = withContext(Dispatchers.IO) {
        val serialized = memberships.mapValues { (_, m) -> mapOf("schoolName" to m.schoolName, "role" to m.role) }
        val updates = mutableMapOf<String, Any>("memberships" to serialized)
        activeSchoolId?.let { updates["activeSchoolId"] = it }
        db.collection("whitelisted_users").document(userId).update(updates).await()
    }

    suspend fun sendFriendRequest(myId: String, myName: String, myEmail: String, targetEmail: String): Boolean = withContext(Dispatchers.IO) {
        val query = db.collection("whitelisted_users").whereEqualTo("email", targetEmail.trim().lowercase()).get().await()
        val targetDoc = query.documents.firstOrNull() ?: return@withContext false
        val targetId = targetDoc.id
        val targetName = targetDoc.getString("name") ?: "Guru"

        val batch = db.batch()
        batch.update(db.collection("whitelisted_users").document(myId), mapOf(
            "friends.$targetId.friendName" to targetName,
            "friends.$targetId.friendEmail" to targetEmail,
            "friends.$targetId.status" to "REQUEST_SENT"
        ))
        batch.update(targetDoc.reference, mapOf(
            "friends.$myId.friendName" to myName,
            "friends.$myId.friendEmail" to myEmail,
            "friends.$myId.status" to "PENDING_APPROVAL"
        ))
        batch.commit().await()
        true
    }

    suspend fun acceptFriendRequest(myId: String, friendId: String) = withContext(Dispatchers.IO) {
        val batch = db.batch()
        batch.update(db.collection("whitelisted_users").document(myId), "friends.$friendId.status", "FRIENDS")
        batch.update(db.collection("whitelisted_users").document(friendId), "friends.$myId.status", "FRIENDS")
        batch.commit().await()
    }

    suspend fun rejectFriendRequest(myId: String, friendId: String) = withContext(Dispatchers.IO) {
        val batch = db.batch()
        batch.update(db.collection("whitelisted_users").document(myId), "friends.$friendId", FieldValue.delete())
        batch.update(db.collection("whitelisted_users").document(friendId), "friends.$myId", FieldValue.delete())
        batch.commit().await()
    }
}
