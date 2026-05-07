package com.azuratech.azuratime.data.repo

import android.app.Application
import android.util.Log
import com.azuratech.azuratime.R
import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuratime.data.local.UserEntity
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.sync.SyncManager
import com.azuratech.azuratime.domain.user.usecase.SyncUserUseCase
import com.azuratech.azuratime.domain.model.SyncStatus
import com.azuratech.azuraengine.result.Result as DomainResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🏰 AUTH REPOSITORY (Optimized for Azura Time)
 */
@Singleton
class AuthRepository @Inject constructor(
    private val application: Application,
    private val database: AppDatabase,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val sessionManager: SessionManager,
    private val syncManager: SyncManager,
    private val syncUserUseCase: SyncUserUseCase
) {
    private val userDao = database.userDao()

    suspend fun signInWithGoogle(idToken: String): Pair<UserEntity?, Boolean> = withContext(Dispatchers.IO) {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: throw Exception("Google profile not found.")
            val email = firebaseUser.email?.lowercase()?.trim() ?: throw Exception("Email not available.")
            val uid = firebaseUser.uid

            // SSOT Migration v7.1: Check Room first
            var userEntity = userDao.getUserById(uid)
            
            if (userEntity == null) {
                // Not in Room, attempt to pull from Cloud
                println("🔍 AuthRepository: User not in Room, pulling from Cloud...")
                val syncResult = syncUserUseCase(uid)
                if (syncResult is DomainResult.Success) {
                    userEntity = syncResult.data
                }
            }

            if (userEntity == null) {
                // Truly a new user
                println("🔍 AuthRepository: New user detected.")
                val newUser = UserEntity(
                    userId = uid,
                    email = email,
                    name = firebaseUser.displayName ?: "User Baru",
                    memberships = emptyMap(),
                    activeSchoolId = null,
                    status = SessionManager.STATUS_PENDING,
                    syncStatus = SyncStatus.PENDING_UPDATE.name
                )
                userDao.insertUser(newUser)
                syncManager.enqueueProfileSync(uid)
                
                sessionManager.saveCurrentUserId(uid)
                sessionManager.saveUserEmail(email)
                sessionManager.saveUserStatus(newUser.status)
                
                return@withContext Pair(newUser, true) 
            }

            // Existing user: Save to session and Room (already saved if pulled via UseCase)
            sessionManager.saveCurrentUserId(uid)
            sessionManager.saveUserEmail(email)
            sessionManager.saveUserStatus(userEntity.status)
            userEntity.activeSchoolId?.let { sessionManager.saveActiveSchoolId(it) }

            if (userEntity.status == SessionManager.STATUS_ACTIVE) {
                sessionManager.refreshIsoKeyFromServer()
            }

            return@withContext Pair(userEntity, false)

        } catch (e: Exception) {
            Log.e("AuthRepository", "Error: ${e.message}")
            throw e
        }
    }

    suspend fun registerMembership(uid: String, data: Map<String, Any>) = withContext(Dispatchers.IO) {
        // SSOT Migration v7.1: Update Room first, then trigger push worker
        val user = userDao.getUserById(uid)
        if (user != null) {
            val updatedUser = user.copy(
                status = data["status"]?.toString() ?: user.status,
                name = data["name"]?.toString() ?: user.name,
                syncStatus = SyncStatus.PENDING_UPDATE.name
            )
            userDao.updateUser(updatedUser)
            syncManager.enqueueProfileSync(uid)
            println("💾 Room: Updated user for membership registration. Sync enqueued.")
        } else {
            // If user doesn't exist, we can't update. This shouldn't happen in the normal flow.
            Log.e("AuthRepository", "Cannot register membership: User $uid not found in Room.")
        }
    }

    suspend fun clearAllDataAndSignOut() = withContext(Dispatchers.IO) {
        database.clearAllTables()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(application.getString(R.string.my_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(application, gso).signOut().await()
        sessionManager.clearSession()
        firebaseAuth.signOut()
    }
}