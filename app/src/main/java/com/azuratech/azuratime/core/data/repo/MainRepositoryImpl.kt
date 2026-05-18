package com.azuratech.azuratime.core.data.repo

import android.content.Context
import android.util.Log
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.data.local.AppDatabase
import com.azuratech.azuratime.core.domain.repository.MainRepository
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.ml.recognizer.FaceRecognizer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val sessionManager: SessionManager,
) : MainRepository {
    override fun getCurrentUid(): Result<String?> = Result.Success(firebaseAuth.currentUser?.uid)

    override fun getCurrentEmail(): Result<String> = Result.Success(firebaseAuth.currentUser?.email ?: "")

    override suspend fun initializeAiBrain(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            FaceRecognizer.initialize(context)
            Log.d("MainRepository", "✅ AI Brain Awakened in Background!")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("MainRepository", "❌ AI Init Error: ${e.message}")
            Result.Failure(AppError.BusinessRule(e.message))
        }
    }

    override fun observeRevokeStatus(uid: String): Flow<Result<Boolean>> = callbackFlow {
        val listener = firestore.collection("whitelisted_accounts")
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MainRepository", "❌ Revoke Listener Error: ${error.message}")
                    trySend(Result.Failure(AppError.Network(error.message)))
                    return@addSnapshotListener
                }

                val cloudStatus = snapshot?.getString("status") ?: ""
                trySend(Result.Success(cloudStatus == "REVOKED"))
            }

        awaitClose { listener.remove() }
    }

    override fun executeRevocationCleanup(): Result<Unit> {
        return try {
            Log.w("MainRepository", "🚨 AKSES DICABUT OLEH ADMIN! Membersihkan sesi...")
            sessionManager.clearSession()
            firebaseAuth.signOut()
            AppDatabase.destroyInstance()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }
}
