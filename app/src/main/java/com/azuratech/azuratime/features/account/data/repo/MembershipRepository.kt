package com.azuratech.azuratime.features.account.data.repo

import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.sync.SyncManager
import com.azuratech.azuratime.features.account.data.local.AccountDao
import com.azuratech.azuratime.features.account.data.local.Membership
import com.azuratech.azuratime.core.domain.model.SyncStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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
    private val accountDao: AccountDao,
    private val syncManager: SyncManager,
    private val accountRepository: AccountRepository,
) {
    fun getCurrentUid(): String? = firebaseAuth.currentUser?.uid

    // =====================================================
    // 🔍 DATA CHECKING
    // =====================================================

    suspend fun checkWhitelisted(uid: String): Map<String, Any>? = withContext(Dispatchers.IO) {
        // SSOT Migration v7.1: Read from Room first
        val account = accountDao.getAccountById(uid)
        if (account != null && account.status == SessionManager.STATUS_ACTIVE) {
            return@withContext accountToMap(account)
        }

        // Trigger sync as a refresh
        accountRepository.syncAccount(uid)

        // Re-read after sync attempt
        val refreshedAccount = accountDao.getAccountById(uid)
        return@withContext if (refreshedAccount != null && refreshedAccount.status == SessionManager.STATUS_ACTIVE) {
            accountToMap(refreshedAccount)
        } else {
            null
        }
    }

    suspend fun checkMembershipExists(uid: String): Boolean = withContext(Dispatchers.IO) {
        // SSOT Migration v7.1: Read from Room first
        val account = accountDao.getAccountById(uid)
        if (account != null) return@withContext true

        // Refresh from cloud
        accountRepository.syncAccount(uid)
        return@withContext accountDao.getAccountById(uid) != null
    }

    // =====================================================
    // ✍️ DATA WRITING & SESSION
    // =====================================================

    suspend fun createPendingUser(uid: String, email: String, displayName: String?) = withContext(Dispatchers.IO) {
        // SSOT Migration v7.1: Save to Room first
        val account = com.azuratech.azuratime.features.account.data.local.AccountEntity(
            accountId = uid,
            email = email,
            name = displayName ?: "User",
            status = SessionManager.STATUS_PENDING,
            syncStatus = SyncStatus.PENDING_UPDATE.name,
        )
        accountDao.upsertAccount(account)

        // 🔥 Push to 'memberships' collection for Firebase Cloud Function
        try {
            val hardwareId = sessionManager.getHardwareId()
            val pendingData = mapOf(
                "accountId" to uid,
                "email" to email,
                "name" to (displayName ?: "User"),
                "hardwareId" to hardwareId,
                "status" to "PENDING",
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            )
            firestore.collection("memberships").document(uid).set(pendingData, com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (e: Exception) {
            android.util.Log.e("MembershipRepo", "Failed to push to memberships collection: ${e.message}")
        }

        sessionManager.saveUserStatus(SessionManager.STATUS_PENDING)
    }

    fun savePendingStatus() {
        sessionManager.saveUserStatus(SessionManager.STATUS_PENDING)
    }

    fun activateSession(data: Map<String, Any>?): Boolean {
        val isoKey = data?.get("secureIsoKey")?.toString() ?: ""
        val schoolId = data?.get("activeSchoolId")?.toString() ?: data?.get("schoolId")?.toString() ?: ""

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
            android.util.Log.w("MembershipRepo", "⚠️ Activation record missing secureIsoKey. Security features may be limited.")
        }

        return true
    }

    // =====================================================
    // 👁️ REAL-TIME OBSERVATION & POLLING
    // =====================================================

    fun observeMemberships(uid: String): Flow<List<com.azuratech.azuratime.features.account.data.local.Membership>> {
        // SSOT Migration v7.1: Observe Room instead of Firestore
        return accountDao.observeAccountById(uid).map { account ->
            account?.memberships?.values?.toList() ?: emptyList()
        }
    }

    fun observeMembershipFlow(uid: String): Flow<MembershipDocUpdate> {
        // SSOT Migration v7.1: Observe Room instead of Firestore
        return accountDao.observeAccountById(uid).map { account ->
            if (account == null) {
                MembershipDocUpdate.DocumentMissing
            } else {
                MembershipDocUpdate.StatusChanged(account.status, accountToMap(account), null)
            }
        }
    }

    suspend fun pollWhitelistedFinal(uid: String): Map<String, Any>? = withContext(Dispatchers.IO) {
        // SSOT Migration v7.1: Polling now checks Room while background sync runs
        var retryCount = 0
        while (retryCount < 5) {
            accountRepository.syncAccount(uid)
            val account = accountDao.getAccountById(uid)
            if (account != null && account.status == SessionManager.STATUS_ACTIVE) {
                return@withContext accountToMap(account)
            }
            delay(2000)
            retryCount++
        }
        null
    }

    private fun accountToMap(account: com.azuratech.azuratime.features.account.data.local.AccountEntity): Map<String, Any> {
        return mapOf(
            "accountId" to account.accountId,
            "email" to account.email,
            "name" to account.name,
            "status" to account.status,
            "activeSchoolId" to (account.activeSchoolId ?: ""),
            "role" to account.role,
            "memberships" to account.memberships,
        )
    }
}
