package com.azuratech.azuratime.features.attendance.data.remote

import com.azuratech.azuratime.core.data.local.AttendanceRecordEntity
import com.azuratech.azuratime.core.data.local.toAttendanceRecordEntity
import com.azuratech.azuratime.core.result.AppError
import com.azuratech.azuratime.core.result.Result
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceRemoteDataSourceImpl @Inject constructor(
    private val db: FirebaseFirestore,
) : AttendanceRemoteDataSource {

    override suspend fun getRecordUpdates(schoolId: String, lastSync: Long): Result<List<AttendanceRecordEntity>> {
        return try {
            val query = db.collection("schools").document(schoolId)
                .collection("checkin_records")

            val snapshot = if (lastSync > 0) {
                val lastTimestamp = com.google.firebase.Timestamp(java.util.Date(lastSync))
                query.whereGreaterThan("lastUpdated", lastTimestamp).get().await()
            } else {
                // 🔥 Recovery Mode: Fetch all history if lastSync is 0 (first sync after login)
                query.get().await()
            }

            val records = snapshot.documents.mapNotNull { doc ->
                doc.toAttendanceRecordEntity(schoolId)
            }
            Result.Success(records)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun syncRecord(record: AttendanceRecordEntity): Result<Unit> {
        return try {
            val data = record.toFirestoreMap().toMutableMap()
            data["lastUpdated"] = com.google.firebase.firestore.FieldValue.serverTimestamp()

            db.collection("schools").document(record.schoolId)
                .collection("checkin_records").document(record.id)
                .set(data, SetOptions.merge())
                .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun deleteRecord(schoolId: String, recordId: String): Result<Unit> {
        return try {
            db.collection("schools").document(schoolId)
                .collection("checkin_records").document(recordId)
                .delete()
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }
}
