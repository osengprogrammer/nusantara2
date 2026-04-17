package com.azuratech.azuratime.data.repository

import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuratime.data.local.UserEntity
import com.azuratech.azuratime.data.local.UserClassAccessEntity
import com.azuratech.azuratime.data.local.Membership
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepository @Inject constructor(
    private val database: AppDatabase,
    private val db: FirebaseFirestore
) {
    private val userDao = database.userDao()
    private val userClassAccessDao = database.userClassAccessDao()

    fun startObservingTeachers(schoolId: String) {
        // Placeholder for legacy admin watcher logic
        // This can be expanded to start a snapshot listener or perform periodic syncs
        android.util.Log.d("AdminRepository", "Started observing teachers for school: $schoolId")
    }

    fun observeUsersForSchool(schoolId: String): Flow<List<UserEntity>> =
        userDao.observeAllUsers().map { users ->
            users.filter { it.memberships.containsKey(schoolId) }
        }

    suspend fun inviteTeacherToWorkspace(
        adminSchoolId: String,
        adminSchoolName: String,
        teacherEmail: String,
        role: String
    ): Boolean = withContext(Dispatchers.IO) {
        val query = db.collection("whitelisted_users").whereEqualTo("email", teacherEmail.trim().lowercase()).get().await()
        val target = query.documents.firstOrNull() ?: return@withContext false
        target.reference.update("memberships.$adminSchoolId", mapOf("schoolName" to adminSchoolName, "role" to role)).await()
        true
    }

    suspend fun revokeTeacherAccessFromWorkspace(targetUserId: String, schoolId: String) = withContext(Dispatchers.IO) {
        db.collection("whitelisted_users").document(targetUserId)
            .update(mapOf("memberships.$schoolId" to FieldValue.delete()))
            .await()

        val localUser = userDao.getUserById(targetUserId)
        localUser?.let {
            val updatedMemberships = it.memberships.toMutableMap().also { m -> m.remove(schoolId) }
            userDao.updateUser(it.copy(memberships = updatedMemberships))
        }
    }

    suspend fun approveMembership(
        targetUserId: String,
        schoolId: String,
        schoolName: String,
        role: String,
        assignedClassIds: List<String> = emptyList()
    ) = withContext(Dispatchers.IO) {

        // 1. Update Firestore Membership & Role
        val userEmail = userDao.getUserById(targetUserId)?.email ?: ""
        val query = db.collection("whitelisted_users").whereEqualTo("email", userEmail.trim().lowercase()).get().await()
        val targetDoc = query.documents.firstOrNull()
        targetDoc?.reference?.update("memberships.$schoolId", mapOf("schoolName" to schoolName, "role" to role))?.await()

        // 2. Jika ada akses kelas, push ke Firestore User Class Access
        if (assignedClassIds.isNotEmpty()) {
            val updateData = hashMapOf(
                "assignedClassIds" to assignedClassIds,
                "lastClassUpdate" to FieldValue.serverTimestamp()
            )
            db.collection("whitelisted_users").document(targetUserId).set(updateData, SetOptions.merge()).await()
        }

        // 3. Update Local Room (User Profil & Role)
        val localUser = userDao.getUserById(targetUserId)
        localUser?.let {
            val updatedMemberships = it.memberships.toMutableMap()
            updatedMemberships[schoolId] = Membership(
                schoolName = schoolName,
                role = role
            )
            userDao.updateUser(it.copy(memberships = updatedMemberships))
        }

        // 4. Update Local Room (Class Authority)
        userClassAccessDao.clearAllAccessForUserInSchool(targetUserId, schoolId)
        assignedClassIds.forEach { cid ->
            userClassAccessDao.insertAccess(
                UserClassAccessEntity(userId = targetUserId, classId = cid, schoolId = schoolId)
            )
        }
    }
}