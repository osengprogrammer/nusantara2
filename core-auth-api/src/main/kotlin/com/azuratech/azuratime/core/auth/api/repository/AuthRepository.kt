package com.azuratech.azuratime.core.auth.api.repository

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.api.account.model.AccountEntity
import com.azuratech.azuratime.core.auth.api.model.AuthState
import kotlinx.coroutines.flow.StateFlow

/**
 * Authentication Repository Interface.
 * Pure Kotlin, zero Android/Firebase dependencies.
 * 
 * This interface defines the contract for authentication operations.
 * The implementation lives in :core-auth-impl with Firebase/Google Auth.
 */
interface AuthRepository {
    
    /**
     * Returns the current authentication state as a StateFlow.
     * Observers can react to state changes in the UI layer.
     */
    val authState: StateFlow<AuthState>
    
    /**
     * Signs in with Google using the provided ID token.
     * 
     * @param idToken The Google ID token from Firebase Auth or GoogleSignIn
     * @return Result containing a Pair of (AccountEntity, isNewAccount)
     */
    suspend fun signInWithGoogle(idToken: String): Result<Pair<AccountEntity, Boolean>>
    
    /**
     * Registers membership data for a new user.
     * Should be called after [signInWithGoogle] returns (..., true) indicating a new account.
     * 
     * @param uid The user's unique ID
     * @param data Membership data to register
     */
    suspend fun registerMembership(uid: String, data: Map<String, Any>): Result<Unit>
    
    /**
     * Signs out the current user and clears all local data.
     * This is a destructive operation that clears the local database cache.
     */
    suspend fun clearAllDataAndSignOut(): Result<Unit>
}