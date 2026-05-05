package com.azuratech.azuratime.data.repo

import com.azuratech.azuratime.core.session.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
class MembershipRepository @Inject constructor( // 🔥 FIX: Tambahkan Hilt Inject
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val sessionManager: SessionManager
) {
    fun getCurrentUid(): String? = firebaseAuth.currentUser?.uid

    // =====================================================
    // 🔍 DATA CHECKING
    // =====================================================

    suspend fun checkWhitelisted(uid: String): Map<String, Any>? = withContext(Dispatchers.IO) {
        val doc = firestore.collection("whitelisted_users").document(uid).get().await()
        return@withContext if (doc.exists()) doc.data else null
    }

    suspend fun checkMembershipExists(uid: String): Boolean = withContext(Dispatchers.IO) {
        firestore.collection("memberships").document(uid).get().await().exists()
    }

    // =====================================================
    // ✍️ DATA WRITING & SESSION
    // =====================================================

    suspend fun createPendingUser(uid: String, email: String, displayName: String?) = withContext(Dispatchers.IO) {
        val pendingData = hashMapOf(
            "userId" to uid,
            "email" to email,
            "name" to (displayName ?: "User"),
            "status" to "PENDING",
            "hardwareId" to sessionManager.getHardwareId(),
            "createdAt" to System.currentTimeMillis()
        )
        firestore.collection("memberships").document(uid).set(pendingData).await()
        sessionManager.saveUserStatus(SessionManager.STATUS_PENDING)
    }

    fun savePendingStatus() {
        sessionManager.saveUserStatus(SessionManager.STATUS_PENDING)
    }

    fun activateSession(data: Map<String, Any>?): Boolean {
        val isoKey = data?.get("secureIsoKey")?.toString() ?: ""
        val schoolId = data?.get("schoolId")?.toString() ?: ""
        val role = data?.get("role")?.toString() ?: "N/A"
        
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
            android.util.Log.w("MembershipRepo", "⚠️ Activation succeeded without secureIsoKey. Security features may be limited.")
        }
        
        return true
    }

    // =====================================================
    // 👁️ REAL-TIME OBSERVATION & POLLING
    // =====================================================

    fun observeMemberships(uid: String): Flow<List<com.azuratech.azuratime.data.local.Membership>> = callbackFlow {
        val listener = firestore.collection("memberships").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val membershipsMap = snapshot?.get("memberships") as? Map<String, Map<String, Any>>
                val list = membershipsMap?.map { (id, data) ->
                    com.azuratech.azuratime.data.local.Membership(
                        schoolName = data["schoolName"] as? String ?: "Unknown",
                        role = data["role"] as? String ?: "MEMBER",
                        assignedClassIds = data["assignedClassIds"] as? List<String> ?: emptyList()
                    )
                } ?: emptyList()
                
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun observeMembershipFlow(uid: String): Flow<MembershipDocUpdate> = callbackFlow {
        val listener = firestore.collection("memberships").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(MembershipDocUpdate.Error("Connection issue."))
                    return@addSnapshotListener
                }
                
                if (snapshot == null || !snapshot.exists()) {
                    trySend(MembershipDocUpdate.DocumentMissing)
                    return@addSnapshotListener
                }
                
                val status = snapshot.getString("status") ?: "PENDING"
                val reason = snapshot.getString("reason")
                trySend(MembershipDocUpdate.StatusChanged(status, snapshot.data, reason))
            }

        awaitClose { listener.remove() }
    }

    suspend fun pollWhitelistedFinal(uid: String): Map<String, Any>? = withContext(Dispatchers.IO) {
        var retryCount = 0
        while (retryCount < 3) {
            val finalDoc = firestore.collection("whitelisted_users").document(uid).get().await()
            if (finalDoc.exists()) {
                return@withContext finalDoc.data
            }
            delay(1500) // Tunggu 1.5 detik
            retryCount++
        }
        return@withContext null
    }
}