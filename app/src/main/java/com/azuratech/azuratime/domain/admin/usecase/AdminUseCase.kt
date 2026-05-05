package com.azuratech.azuratime.domain.admin.usecase

import com.azuratech.azuratime.data.local.*
import com.azuratech.azuratime.data.repo.AdminRepository
import com.azuratech.azuratime.data.repo.UserRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * UseCase to handle administrative operations like membership approval and teacher management.
 */
class AdminUseCase @Inject constructor(
    private val database: AppDatabase,
    private val userRepository: UserRepository,
    private val db: FirebaseFirestore
) {
    private val userDao = database.userDao()
    private val userClassAccessDao = database.userClassAccessDao()

    suspend fun inviteTeacherToWorkspace(
        adminSchoolId: String,
        adminSchoolName: String,
        teacherEmail: String,
        role: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val query = db.collection("whitelisted_users").whereEqualTo("email", teacherEmail.trim().lowercase()).get().await()
            val target = query.documents.firstOrNull() ?: return@withContext false
            val targetUserId = target.id
            
            // 1. Update Locally (Source of Truth)
            userRepository.approveMembership(targetUserId, adminSchoolId, adminSchoolName, role)
            
            // 2. Update Cloud (Side Effect)
            target.reference.update("memberships.$adminSchoolId", mapOf("schoolName" to adminSchoolName, "role" to role)).await()
            
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun revokeTeacherAccessFromWorkspace(targetUserId: String, schoolId: String) = withContext(Dispatchers.IO) {
        // 1. Update Locally (Source of Truth)
        userRepository.revokeMembership(targetUserId, schoolId)

        // 2. Update Cloud (Side Effect)
        try {
            db.collection("whitelisted_users").document(targetUserId)
                .update(mapOf("memberships.$schoolId" to FieldValue.delete()))
                .await()
        } catch (e: Exception) {
            // Log error
        }
    }

    suspend fun approveMembership(
        targetUserId: String,
        schoolId: String,
        schoolName: String,
        role: String,
        assignedClassIds: List<String> = emptyList()
    ) = withContext(Dispatchers.IO) {
        // 1. Update Locally (Source of Truth)
        userRepository.approveMembership(targetUserId, schoolId, schoolName, role, assignedClassIds)

        // 2. Update Cloud (Side Effect)
        try {
            val userEmail = userDao.getUserById(targetUserId)?.email ?: ""
            val query = db.collection("whitelisted_users").whereEqualTo("email", userEmail.trim().lowercase()).get().await()
            val targetDoc = query.documents.firstOrNull()
            targetDoc?.reference?.update("memberships.$schoolId", mapOf("schoolName" to schoolName, "role" to role))?.await()

            if (assignedClassIds.isNotEmpty()) {
                val updateData = hashMapOf("assignedClassIds" to assignedClassIds, "lastClassUpdate" to FieldValue.serverTimestamp())
                db.collection("whitelisted_users").document(targetUserId).set(updateData, SetOptions.merge()).await()
            }
        } catch (e: Exception) {
            // Log error
        }
    }
}
