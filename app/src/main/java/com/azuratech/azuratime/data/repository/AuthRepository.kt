package com.azuratech.azuratime.data.repository

import android.app.Application
import android.util.Log
import com.azuratech.azuratime.R
import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuratime.data.local.Membership
import com.azuratech.azuratime.data.local.UserEntity
import com.azuratech.azuratime.core.session.SessionManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
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
class AuthRepository @Inject constructor( // 🔥 1. Tambahkan Inject Constructor
    private val application: Application,
    private val database: AppDatabase,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val sessionManager: SessionManager
) {
    private val userDao = database.userDao()

    suspend fun signInWithGoogle(idToken: String): Pair<UserEntity?, Boolean> = withContext(Dispatchers.IO) {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: throw Exception("Google profile not found.")
            val email = firebaseUser.email?.lowercase()?.trim() ?: throw Exception("Email not available.")
            val uid = firebaseUser.uid

            val userDoc: DocumentSnapshot? = runCatching {
                val whiteList = firestore.collection("whitelisted_users").document(uid).get().await()
                if (whiteList.exists()) whiteList 
                else firestore.collection("memberships").document(uid).get().await()
            }.getOrNull()

            if (userDoc == null || !userDoc.exists()) {
                val newUser = UserEntity(
                    userId = uid,
                    email = email,
                    name = firebaseUser.displayName ?: "User Baru",
                    memberships = emptyMap(),
                    activeSchoolId = null,
                    status = SessionManager.STATUS_PENDING
                )
                return@withContext Pair(newUser, true) 
            }

            @Suppress("UNCHECKED_CAST")
            val membershipsRaw = userDoc.get("memberships") as? Map<String, Any> ?: emptyMap()
            val memberships = membershipsRaw.entries.mapNotNull { (schoolId, value) ->
                val m = value as? Map<*, *> ?: return@mapNotNull null
                val schoolName = m["schoolName"] as? String ?: ""
                val role = m["role"] as? String ?: "TEACHER"
                schoolId to Membership(schoolName = schoolName, role = role)
            }.toMap()

            val savedActiveSchoolId = userDoc.getString("activeSchoolId")
                ?: memberships.keys.firstOrNull()

            val userEntity = UserEntity(
                userId = uid,
                email = email,
                name = userDoc.getString("name") ?: firebaseUser.displayName ?: "User Azura",
                memberships = memberships,
                activeSchoolId = savedActiveSchoolId,
                status = userDoc.getString("status") ?: SessionManager.STATUS_PENDING
            )

            userDao.insertUser(userEntity)
            sessionManager.saveCurrentUserId(uid)
            sessionManager.saveUserEmail(email)
            sessionManager.saveUserStatus(userEntity.status)
            savedActiveSchoolId?.let { sessionManager.saveActiveSchoolId(it) }

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
        firestore.collection("memberships").document(uid).set(data).await()
    }

    suspend fun clearAllDataAndSignOut() = withContext(Dispatchers.IO) {
        database.clearAllTables()
        // Kita tidak menghancurkan instance secara manual lagi, Hilt akan mengelola lifecycle-nya
        // AppDatabase.destroyInstance() 
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(application.getString(R.string.my_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(application, gso).signOut().await()
        sessionManager.clearSession()
        firebaseAuth.signOut()
    }
}