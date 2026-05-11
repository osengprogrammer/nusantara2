package com.azuratech.azuratime.data.repo

import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.sync.SyncManager
import com.azuratech.azuratime.data.local.UserDao
import com.azuratech.azuratime.domain.model.SyncStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class MembershipDocUpdate {
    data class StatusChanged(val status: String, val data: Map<String, Any>?, val reason: String?) : MembershipDocUpdate()
    object DocumentMissing : MembershipDocUpdate()
    data class Error(val message: String) : MembershipDocUpdate()
}

/**
 * 🏰 MEMBERSHIP REPOSITORY
 * Sudah menggunakan Hilt Inject Constructor.
 */
@Singleton
class MembershipRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val sessionManager: SessionManager,
    private val userDao: UserDao,
    private val syncManager: SyncManager,
    private val userRepository: StaffAccountRepository
) {
    fun getCurrentUid(): String? = firebaseAuth.currentUser?.uid

    // =====================================================
    // 🔍 DATA CHECKING
    // =====================================================

    suspend fun checkWhitelisted(uid: String): Map<String, Any>? = withContext(Dispatchers.IO) {
        // SSOT Migration v7.1: Read from Room first
        val user = userDao.getUserById(uid)
        if (user != null && user.status == SessionManager.STATUS_ACTIVE) {
            return@withContext userToMap(user)
        }

        // Trigger sync as a refresh
        userRepository.syncUser(uid)
        
        // Re-read after sync attempt
        val refreshedUser = userDao.getUserById(uid)
        return@withContext if (refreshedUser != null && refreshedUser.status == SessionManager.STATUS_ACTIVE) {
            userToMap(refreshedUser)
        } else null
    }

    suspend fun checkMembershipExists(uid: String): Boolean = withContext(Dispatchers.IO) {
        // SSOT Migration v7.1: Read from Room first
        val user = userDao.getUserById(uid)
        if (user != null) return@withContext true
        
        // Refresh from cloud
        userRepository.syncUser(uid)
        return@withContext userDao.getUserById(uid) != null
    }

    // =====================================================
    // ✍️ DATA WRITING & SESSION
    // =====================================================

    suspend fun createPendingUser(uid: String, email: String, displayName: String?) = withContext(Dispatchers.IO) {
        // SSOT Migration v7.1: Save to Room first, then sync
        val user = com.azuratech.azuratime.data.local.UserEntity(
            userId = uid,
            email = email,
            name = displayName ?: "User",
            status = SessionManager.STATUS_PENDING,
            syncStatus = SyncStatus.PENDING_UPDATE.name
        )
        userDao.insertUser(user)
        syncManager.enqueueProfileSync(uid)
        sessionManager.saveUserStatus(SessionManager.STATUS_PENDING)
    }

    fun savePendingStatus() {
        sessionManager.saveUserStatus(SessionManager.STATUS_PENDING)
    }

    fun activateSession(data: Map<String, Any>?): Boolean {
        val isoKey = data?.get("secureIsoKey")?.toString() ?: ""
        val schoolId = data?.get("schoolId")?.toString() ?: ""
        
        val expireDate = (data?.get("expireDate") as? Number)?.toLong() 
            ?: (System.currentTimeMillis() + 31536000000L) // +1 Year fallback

        // 🔥 Save active school ID if present
        if (schoolId.isNotEmpty()) {
            sessionManager.saveActiveSchoolId(schoolId)
        }

        // 🔥 Always save status to unblock UI
        sessionManager.saveUserStatus(SessionManager.STATUS_ACTIVE)

        if (!isoKey.isNullOrEmpty()) {
            sessionManager.injectSecurityEnvelope(isoKey, expireDate)
        } else {
            // SSOT v7.1: If isoKey is missing in map, attempt to refresh from server
            // Note: This is a side-effect, usually we'd prefer this to be in a Worker
            android.util.Log.w("MembershipRepo", "⚠️ Activation record missing secureIsoKey. Security features may be limited.")
        }
        
        return true
    }

    // =====================================================
    // 👁️ REAL-TIME OBSERVATION & POLLING
    // =====================================================

    fun observeMemberships(uid: String): Flow<List<com.azuratech.azuratime.data.local.Membership>> {
        // SSOT Migration v7.1: Observe Room instead of Firestore
        return userDao.observeUserById(uid).map { user ->
            user?.memberships?.values?.toList() ?: emptyList()
        }
    }

    fun observeMembershipFlow(uid: String): Flow<MembershipDocUpdate> {
        // SSOT Migration v7.1: Observe Room instead of Firestore
        return userDao.observeUserById(uid).map { user ->
            if (user == null) MembershipDocUpdate.DocumentMissing
            else {
                MembershipDocUpdate.StatusChanged(user.status, userToMap(user), null)
            }
        }
    }

    suspend fun pollWhitelistedFinal(uid: String): Map<String, Any>? = withContext(Dispatchers.IO) {
        // SSOT Migration v7.1: Polling now checks Room while background sync runs
        var retryCount = 0
        while (retryCount < 5) {
            userRepository.syncUser(uid)
            val user = userDao.getUserById(uid)
            if (user != null && user.status == SessionManager.STATUS_ACTIVE) {
                return@withContext userToMap(user)
            }
            delay(2000)
            retryCount++
        }
        null
    }

    private fun userToMap(user: com.azuratech.azuratime.data.local.UserEntity): Map<String, Any> {
        return mapOf(
            "userId" to user.userId,
            "email" to user.email,
            "name" to user.name,
            "status" to user.status,
            "activeSchoolId" to (user.activeSchoolId ?: ""),
            "role" to user.role,
            "memberships" to user.memberships
        )
    }
}