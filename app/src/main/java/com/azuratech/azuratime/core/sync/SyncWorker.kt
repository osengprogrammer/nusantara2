package com.azuratech.azuratime.core.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuratime.data.repository.CheckInRepository
import com.azuratech.azuratime.data.repository.ClassRepository
import com.azuratech.azuratime.data.repository.FaceRepository
import com.azuratech.azuratime.data.repository.FaceAssignmentRepository
import com.azuratech.azuratime.data.repository.UserRepository
import com.azuratech.azuratime.core.session.SessionManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 🛡️ THE INVISIBLE GUARDRAIL: Persistent Background Sync
 * 
 * Memastikan sinkronisasi data tetap berjalan hingga selesai,
 * bahkan jika aplikasi ditutup atau jaringan terputus (akan di-retry
 * saat koneksi kembali ada).
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: AppDatabase,
    private val userRepository: UserRepository,
    private val classRepository: ClassRepository,
    private val faceRepository: FaceRepository,
    private val faceAssignmentRepository: FaceAssignmentRepository,
    private val checkInRepository: CheckInRepository,
    private val sessionManager: SessionManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("AZURA_SYNC", "SyncWorker: Starting persistent background sync...")
            val schoolId = sessionManager.getActiveSchoolId() ?: ""
            if (schoolId.isNotEmpty()) {
                // 1. Push lokal ke Cloud
                pushPendingRecordsToCloud(schoolId)
                // 2. Pull Cloud ke Lokal
                pullWorkspaceData()
                
                Log.d("AZURA_SYNC", "SyncWorker: Sync completed successfully.")
                Result.success()
            } else {
                Log.w("AZURA_SYNC", "SyncWorker: No active school ID found. Aborting.")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e("AZURA_SYNC", "SyncWorker: Error during sync - ${e.message}")
            // Jika gagal karena jaringan/timeout, jadwalkan ulang
            Result.retry()
        }
    }

    private suspend fun pullWorkspaceData() {
        // Sedot Hak Akses
        val currentUserId = sessionManager.getCurrentUserId()
        if (currentUserId != null) {
            userRepository.syncUserFromCloud(currentUserId)
        }

        // Sedot Kelas Delta
        classRepository.performClassDeltaSync()

        // Sedot Wajah Delta
        faceRepository.performFaceDeltaSync()

        // Sedot Face Assignments
        faceAssignmentRepository.performAssignmentSync()
    }

    private suspend fun pushPendingRecordsToCloud(schoolId: String) {
        val unsynced = database.checkInRecordDao().getUnsyncedRecords(schoolId)
        for (record in unsynced) {
            try {
                checkInRepository.saveRecord(record)
            } catch (e: Exception) {
                Log.e("AZURA_SYNC", "Failed pushing record: ${record.id}")
                // Throw exception agar worker melakukan retry
                throw e
            }
        }
    }
}