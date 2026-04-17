package com.azuratech.azuratime.data.repository

import android.content.Context
import android.util.Log
import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuratime.ml.recognizer.FaceRecognizer
import com.azuratech.azuratime.core.session.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🏰 MAIN REPOSITORY
 * Pusat inisialisasi aplikasi, AI Brain, dan Security Cloud Listener.
 * 🔥 Sudah menggunakan Hilt Dependency Injection.
 */
@Singleton
class MainRepository @Inject constructor( // 🔥 FIX: Tambahkan Inject Constructor
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val sessionManager: SessionManager
) {
    fun getCurrentUid(): String? = firebaseAuth.currentUser?.uid
    
    fun getCurrentEmail(): String = firebaseAuth.currentUser?.email ?: ""

    // =====================================================
    // 🧠 AI INITIALIZATION
    // =====================================================
    suspend fun initializeAiBrain(context: Context) = withContext(Dispatchers.IO) {
        try {
            FaceRecognizer.initialize(context)
            Log.d("MainRepository", "✅ AI Brain Awakened in Background!")
        } catch (e: Exception) {
            Log.e("MainRepository", "❌ AI Init Error: ${e.message}")
        }
    }

    // =====================================================
    // 🛡️ SECURITY & REVOKE LISTENER
    // =====================================================
    
    fun observeRevokeStatus(uid: String): Flow<Boolean> = callbackFlow {
        val listener = firestore.collection("whitelisted_users")
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MainRepository", "❌ Revoke Listener Error: ${error.message}")
                    return@addSnapshotListener
                }
                
                val cloudStatus = snapshot?.getString("status") ?: ""
                
                if (cloudStatus == "REVOKED") {
                    trySend(true)
                } else {
                    trySend(false)
                }
            }

        awaitClose { listener.remove() }
    }

    fun executeRevocationCleanup() {
        Log.w("MainRepository", "🚨 AKSES DICABUT OLEH ADMIN! Membersihkan sesi...")
        sessionManager.clearSession()
        firebaseAuth.signOut()
        AppDatabase.destroyInstance()
    }
}