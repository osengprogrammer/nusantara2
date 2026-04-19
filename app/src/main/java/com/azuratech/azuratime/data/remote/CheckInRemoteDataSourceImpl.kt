package com.azuratech.azuratime.data.remote

import android.util.Log
import com.azuratech.azuratime.data.local.CheckInRecordEntity
import com.azuratech.azuratime.data.local.toCheckInRecordEntity
import com.azuratech.azuratime.domain.result.AppError
import com.azuratech.azuratime.domain.result.Result
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckInRemoteDataSourceImpl @Inject constructor(
    private val db: FirebaseFirestore
) : CheckInRemoteDataSource {

    override suspend fun getRecordUpdates(schoolId: String, lastSync: Long): Result<List<CheckInRecordEntity>> {
        return try {
            val lastTimestamp = com.google.firebase.Timestamp(java.util.Date(lastSync))
            val snapshot = db.collection("schools").document(schoolId)
                .collection("checkin_records")
                .whereGreaterThan("lastUpdated", lastTimestamp)
                .get().await()

            val records = snapshot.documents.mapNotNull { doc ->
                doc.toCheckInRecordEntity(schoolId)
            }
            Result.Success(records)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun syncRecord(record: CheckInRecordEntity): Result<Unit> {
        return try {
            val batch = db.batch()
            val data = record.toFirestoreMap().toMutableMap()
            data["lastUpdated"] = com.google.firebase.firestore.FieldValue.serverTimestamp()

            batch.set(
                db.collection("schools").document(record.schoolId).collection("checkin_records").document(record.id),
                data,
                SetOptions.merge()
            )
            batch.set(
                db.collection("attendance_logs").document(record.id),
                data,
                SetOptions.merge()
            )

            batch.commit().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun deleteRecord(schoolId: String, recordId: String): Result<Unit> {
        return try {
            val batch = db.batch()
            batch.delete(db.collection("schools").document(schoolId).collection("checkin_records").document(recordId))
            batch.delete(db.collection("attendance_logs").document(recordId))
            batch.commit().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }
}
