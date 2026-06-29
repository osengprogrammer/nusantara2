package com.azuratech.azuratime.features.auth.data.repo

import android.app.Application
import android.util.Log
import com.azuratech.azuratime.R
import com.azuratech.azuratime.core.data.local.AppDatabase
import androidx.room.withTransaction
import com.azuratech.azuratime.features.account.data.local.AccountEntity
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.sync.SyncManager
import com.azuratech.azuratime.core.domain.model.SyncStatus
import com.azuratech.azuraengine.result.Result as DomainResult
import com.azuratech.azuratime.core.domain.repository.SecurityRepository
import com.azuratech.azuratime.features.auth.domain.repository.AuthRepository
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
 * 🏰 AUTH REPOSITORY IMPLEMENTATION (v3.2.0-ai-native)
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val application: Application,
    private val database: AppDatabase,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val sessionManager: SessionManager,
    private val syncManager: SyncManager,
    private val accountRepository: AccountRepository,
    private val securityRepository: SecurityRepository,
) : AuthRepository {
    private val accountDao = database.accountDao()

    override suspend fun signInWithGoogle(idToken: String): DomainResult<Pair<AccountEntity, Boolean>> = withContext(Dispatchers.IO) {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: return@withContext DomainResult.Failure(com.azuratech.azuraengine.result.AppError.BusinessRule("Google profile not found."))
            val email = firebaseUser.email?.lowercase()?.trim() ?: return@withContext DomainResult.Failure(com.azuratech.azuraengine.result.AppError.BusinessRule("Email not available."))
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
                // Truly a new Account
                println("🔍 AuthRepository: New account detected.")
                val newAccount = AccountEntity(
                    accountId = uid,
                    email = email,
                    name = firebaseUser.displayName ?: "New Account",
                    role = "USER", // 🔥 AI Native Secure Default
                    memberships = emptyMap(),
                    activeSchoolId = null,
                    status = SessionManager.STATUS_PENDING,
                    syncStatus = SyncStatus.PENDING_UPDATE.name,
                )
                accountDao.upsertAccount(newAccount)
                syncManager.enqueueAccountSync(uid)

                sessionManager.saveCurrentAccountId(uid)
                sessionManager.saveAccountEmail(email)
                sessionManager.saveAccountStatus(newAccount.status)

                return@withContext DomainResult.Success(Pair(newAccount, true))
            }

            // Existing account: Save to session and Room (already saved if pulled via UseCase)
            sessionManager.saveCurrentAccountId(uid)
            sessionManager.saveAccountEmail(email)
            sessionManager.saveAccountStatus(accountEntity.status)
            accountEntity.activeSchoolId?.let { sessionManager.saveActiveSchoolId(it) }

            if (accountEntity.status == SessionManager.STATUS_ACTIVE) {
                securityRepository.refreshIsoKeyFromServer()
            }

            return@withContext DomainResult.Success(Pair(accountEntity, false))
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error: ${e.message}")
            DomainResult.Failure(com.azuratech.azuraengine.result.AppError.Network(e.message))
        }
    }

    override suspend fun registerMembership(uid: String, data: Map<String, Any>): DomainResult<Unit> = withContext(Dispatchers.IO) {
        try {
            // SSOT Migration v7.1: Update Room first, then trigger push worker
            val account = accountDao.getAccountById(uid)
            if (account != null) {
                val updatedAccount = account.copy(
                    status = data["status"]?.toString() ?: account.status,
                    name = data["name"]?.toString() ?: account.name,
                    syncStatus = SyncStatus.PENDING_UPDATE.name,
                )
                accountDao.updateAccount(updatedAccount)
                syncManager.enqueueAccountSync(uid)
                println("💾 Room: Updated account for membership registration. Sync enqueued.")
                DomainResult.Success(Unit)
            } else {
                // If account doesn't exist, we can't update. This shouldn't happen in the normal flow.
                Log.e("AuthRepository", "Cannot register membership: Account $uid not found in Room.")
                DomainResult.Failure(com.azuratech.azuraengine.result.AppError.LocalDB("Account $uid not found in Room."))
            }
        } catch (e: Exception) {
            DomainResult.Failure(com.azuratech.azuraengine.result.AppError.Network(e.message))
        }
    }

    override suspend fun clearAllDataAndSignOut(): DomainResult<Unit> = withContext(Dispatchers.IO) {
        try {
            // 🔥 AI Native: Set logging out state early
            sessionManager.setLoggingOut(true)

            // 1. Google Sign Out (Non-blocking)
            try {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(application.getString(R.string.my_web_client_id))
                    .requestEmail()
                    .build()
                GoogleSignIn.getClient(application, gso).signOut()
            } catch (e: Exception) {
                Log.e("AuthRepository", "Google SignOut Error: ${e.message}")
            }

            // 2. Firebase Sign Out
            try {
                firebaseAuth.signOut()
            } catch (e: Exception) {
                Log.e("AuthRepository", "Firebase SignOut Error: ${e.message}")
            }

            // 3. Clear Local SQLite Database (Room cache)
            try {
                database.withTransaction {
                    database.clearAllTables()
                }
                Log.d("AuthRepository", "SQLite database cleared successfully.")
            } catch (e: Exception) {
                Log.e("AuthRepository", "Failed to clear SQLite database: ${e.message}")
            }

            // 4. Clear Local Session (This triggers UI switch via BootViewModel)
            sessionManager.clearSession()

            DomainResult.Success(Unit)
        } catch (e: Exception) {
            sessionManager.setLoggingOut(false)
            DomainResult.Failure(com.azuratech.azuraengine.result.AppError.BusinessRule(e.message))
        }
    }
}
