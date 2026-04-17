package com.azuratech.azuratime.data.repository

import android.app.Application
import android.util.Log
import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuratime.data.local.CheckInRecordEntity
import com.azuratech.azuratime.data.local.toCheckInRecordEntity
import com.azuratech.azuratime.core.session.SessionManager
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckInRepository @Inject constructor(
    private val application: Application,
    database: AppDatabase,
    private val db: FirebaseFirestore,
    private val sessionManager: SessionManager
) {
    private val checkInRecordDao = database.checkInRecordDao()

    private val schoolId: String get() = sessionManager.getActiveSchoolId() ?: ""

    private var _activeClassId: String? = null

    fun setActiveClass(classId: String?) {
        _activeClassId = classId
    }

    // =====================================================
    // 📖 READ (FLOWS)
    // =====================================================

    fun getFilteredRecords(
        nameFilter: String,
        startDate: LocalDate?,
        endDate: LocalDate?,
        userId: String?,
        classId: String?,
        assignedIds: List<String>,
        schoolId: String = this.schoolId
    ): Flow<List<CheckInRecordEntity>> {
        return checkInRecordDao.getFilteredRecords(
            nameFilter = nameFilter,
            startDate = startDate,
            endDate = endDate,
            userId = userId,
            classId = classId,
            assignedIds = assignedIds,
            schoolId = schoolId
        )
    }

    // =====================================================
    // 📥 PULL & DELTA SYNC
    // =====================================================

    suspend fun performRecordsDeltaSync() = withContext(Dispatchers.IO) {
        if (schoolId.isBlank()) return@withContext

        val lastSync = sessionManager.getLastRecordsSyncTime()
        val lastTimestamp = com.google.firebase.Timestamp(java.util.Date(lastSync))

        try {
            val snapshot = db.collection("schools").document(schoolId)
                .collection("checkin_records")
                .whereGreaterThan("lastUpdated", lastTimestamp)
                .get().await()

            if (snapshot.documents.isNotEmpty()) {
                val records = snapshot.documents.mapNotNull { doc ->
                    doc.toCheckInRecordEntity(schoolId)
                }
                records.forEach { record ->
                    checkInRecordDao.insert(record)
                }
                sessionManager.saveLastRecordsSyncTime()
                Log.i("CheckInRepository", "✅ Delta Sync: Pulled ${records.size} records")
            }
        } catch (e: Exception) {
            Log.e("CheckInRepository", "❌ Delta Sync Failed: ${e.message}")
        }
    }

    // =====================================================
    // ✍️ WRITE & SYNC
    // =====================================================

    suspend fun saveRecord(record: CheckInRecordEntity) = withContext(Dispatchers.IO) {
        checkInRecordDao.insert(record)

        if (record.schoolId.isBlank()) return@withContext

        try {
            syncRecordToCloud(record)
            checkInRecordDao.update(record.copy(isSynced = true))
        } catch (e: Exception) {
            Log.e("CheckInRepository", "Gagal sync ke cloud: ${e.message}")
        }
    }

    suspend fun updateRecordClass(recordId: String, classId: String, className: String) = withContext(Dispatchers.IO) {
        val record = checkInRecordDao.getRecordById(recordId, schoolId)
        record?.let {
            val updatedRecord = it.copy(
                classId = classId,
                className = className,
                isSynced = false
            )
            checkInRecordDao.update(updatedRecord)

            if (updatedRecord.schoolId.isBlank()) return@withContext

            try {
                syncRecordToCloud(updatedRecord)
                checkInRecordDao.update(updatedRecord.copy(isSynced = true))
            } catch (e: Exception) {
                Log.e("CheckInRepository", "Gagal sync Update Class ke cloud: ${e.message}")
            }
        }
    }

    suspend fun updateRecord(record: CheckInRecordEntity) = withContext(Dispatchers.IO) {
        val recordToUpdate = record.copy(isSynced = false)
        checkInRecordDao.update(recordToUpdate)

        if (recordToUpdate.schoolId.isBlank()) return@withContext

        try {
            syncRecordToCloud(recordToUpdate)
            checkInRecordDao.update(recordToUpdate.copy(isSynced = true))
        } catch (e: Exception) {
            Log.e("CheckInRepository", "Gagal sync Update ke cloud: ${e.message}")
        }
    }

    suspend fun deleteRecord(record: CheckInRecordEntity) = withContext(Dispatchers.IO) {
        checkInRecordDao.delete(record)

        if (record.schoolId.isBlank()) return@withContext

        try {
            deleteRecordFromCloud(schoolId, record.id)
        } catch (e: Exception) {
            Log.e("CheckInRepository", "Gagal hapus dari cloud: ${e.message}")
        }
    }

    // =====================================================
    // ☁️ CLOUD OPERATIONS (Migrated from FirestoreManager)
    // =====================================================

    suspend fun syncRecordToCloud(record: CheckInRecordEntity) {
        val batch = db.batch()
        val data = record.toFirestoreMap()

        batch.set(db.collection("schools").document(record.schoolId).collection("checkin_records").document(record.id), data, com.google.firebase.firestore.SetOptions.merge())
        batch.set(db.collection("attendance_logs").document(record.id), data, com.google.firebase.firestore.SetOptions.merge())

        batch.commit().await()
    }

    suspend fun deleteRecordFromCloud(schoolId: String, recordId: String) {
        val batch = db.batch()
        batch.delete(db.collection("schools").document(schoolId).collection("checkin_records").document(recordId))
        batch.delete(db.collection("attendance_logs").document(recordId))
        batch.commit().await()
    }
}
