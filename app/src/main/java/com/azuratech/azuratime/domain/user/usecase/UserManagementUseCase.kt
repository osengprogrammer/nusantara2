package com.azuratech.azuratime.domain.user.usecase

import com.azuratech.azuratime.data.local.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * UseCase to handle user-related cloud operations and complex orchestration.
 */
class UserManagementUseCase @Inject constructor(
    private val database: AppDatabase,
    private val db: FirebaseFirestore
) {
    private val userDao = database.userDao()
    private val userClassAccessDao = database.userClassAccessDao()

    suspend fun assignClassToUser(targetId: String, schoolId: String, classId: String) = withContext(Dispatchers.IO) {
        userClassAccessDao.insertAccess(UserClassAccessEntity(userId = targetId, classId = classId, schoolId = schoolId))
        try {
            val userRef = db.collection("whitelisted_users").document(targetId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                if (snapshot.exists()) {
                    val updates = mutableMapOf<String, Any>("assignedClassIds" to FieldValue.arrayUnion(classId))
                    val currentRole = snapshot.getString("memberships.$schoolId.role")
                    if (currentRole == null || currentRole == "PENDING" || currentRole == "USER") {
                        updates["memberships.$schoolId.role"] = "TEACHER"
                    }
                    transaction.update(userRef, updates)
                }
            }.await()
        } catch (e: Exception) {
            println("ERROR: [UserManagementUseCase] Failed to sync class assignment: ${e.message}")
        }
    }

    suspend fun removeClassAccess(targetId: String, classId: String, schoolId: String) = withContext(Dispatchers.IO) {
        userClassAccessDao.removeAccess(userId = targetId, classId = classId, schoolId = schoolId)
        val currentIds = userClassAccessDao.getClassIdsForUser(targetId, schoolId)
        try {
            db.collection("whitelisted_users").document(targetId)
                .set(mapOf("assignedClassIds" to currentIds), SetOptions.merge()).await()
        } catch (e: Exception) {
            println("ERROR: [UserManagementUseCase] Failed to remove class access: ${e.message}")
        }
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
