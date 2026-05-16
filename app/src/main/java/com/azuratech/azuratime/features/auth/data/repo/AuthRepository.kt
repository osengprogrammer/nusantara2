package com.azuratech.azuratime.features.auth.data.repo

import android.app.Application
import android.util.Log
import com.azuratech.azuratime.R
import com.azuratech.azuratime.core.data.local.AppDatabase
import com.azuratech.azuratime.features.account.data.local.AccountEntity
import com.azuratech.azuratime.features.account.data.repo.AccountRepository
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.sync.SyncManager
import com.azuratech.azuratime.core.domain.model.SyncStatus
import com.azuratech.azuraengine.result.Result as DomainResult
import com.azuratech.azuratime.core.data.repo.SecurityRepository
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
    private val accountRepository: AccountRepository,
    private val securityRepository: SecurityRepository
) {
    private val accountDao = database.accountDao()

    suspend fun signInWithGoogle(idToken: String): Pair<AccountEntity?, Boolean> = withContext(Dispatchers.IO) {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: throw Exception("Google profile not found.")
            val email = firebaseUser.email?.lowercase()?.trim() ?: throw Exception("Email not available.")
            val uid = firebaseUser.uid

            // SSOT Migration v7.1: Check Room first
            var accountEntity = accountDao.getAccountById(uid)
            
            if (accountEntity == null) {
                // Not in Room, attempt to pull from Cloud
                println("🔍 AuthRepository: Account not in Room, pulling from Cloud...")
                val syncResult = accountRepository.syncAccount(uid)
                if (syncResult is DomainResult.Success) {
                    accountEntity = syncResult.data
                }
            }

            if (accountEntity == null) {
                // Truly a new user (Account)
                println("🔍 AuthRepository: New account detected.")
                val newAccount = AccountEntity(
                    accountId = uid,
                    email = email,
                    name = firebaseUser.displayName ?: "User Baru",
                    memberships = emptyMap(),
                    activeSchoolId = null,
                    status = SessionManager.STATUS_PENDING,
                    syncStatus = SyncStatus.PENDING_UPDATE.name
                )
                accountDao.upsertAccount(newAccount)
                syncManager.enqueueProfileSync(uid)
                
                sessionManager.saveCurrentUserId(uid)
                sessionManager.saveUserEmail(email)
                sessionManager.saveUserStatus(newAccount.status)
                
                return@withContext Pair(newAccount, true) 
            }

            // Existing account: Save to session and Room (already saved if pulled via UseCase)
            sessionManager.saveCurrentUserId(uid)
            sessionManager.saveUserEmail(email)
            sessionManager.saveUserStatus(accountEntity.status)
            accountEntity.activeSchoolId?.let { sessionManager.saveActiveSchoolId(it) }

            if (accountEntity.status == SessionManager.STATUS_ACTIVE) {
                securityRepository.refreshIsoKeyFromServer()
            }

            return@withContext Pair(accountEntity, false)

        } catch (e: Exception) {
            Log.e("AuthRepository", "Error: ${e.message}")
            throw e
        }
    }

    suspend fun registerMembership(uid: String, data: Map<String, Any>) = withContext(Dispatchers.IO) {
        // SSOT Migration v7.1: Update Room first, then trigger push worker
        val account = accountDao.getAccountById(uid)
        if (account != null) {
            val updatedAccount = account.copy(
                status = data["status"]?.toString() ?: account.status,
                name = data["name"]?.toString() ?: account.name,
                syncStatus = SyncStatus.PENDING_UPDATE.name
            )
            accountDao.updateAccount(updatedAccount)
            syncManager.enqueueProfileSync(uid)
            println("💾 Room: Updated account for membership registration. Sync enqueued.")
        } else {
            // If account doesn't exist, we can't update. This shouldn't happen in the normal flow.
            Log.e("AuthRepository", "Cannot register membership: Account $uid not found in Room.")
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
