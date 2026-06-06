package com.azuratech.azuratime.features.biometric.data.remote

import com.azuratech.azuratime.features.student.data.local.StudentClassAssignmentEntity
import com.azuratech.azuratime.features.biometric.data.local.StudentBiometricEntity
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuraengine.result.AppError
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricRemoteDataSourceImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage,
) : BiometricRemoteDataSource {

    private fun getTenantRef(schoolId: String) = db.collection("schools").document(schoolId)

    override suspend fun getBiometricUpdates(schoolId: String, lastSync: Long): Result<List<Pair<StudentBiometricEntity, Boolean>>> {
        return try {
            val lastTimestamp = com.google.firebase.Timestamp(java.util.Date(lastSync))

            val snapshot = getTenantRef(schoolId).collection("master_faces")
                .whereGreaterThan("lastUpdated", lastTimestamp).get().await()

            val updatedData = snapshot.documents.mapNotNull { doc ->
                try {
                    val embedding = (doc.get("embedding") as? List<*>)?.map { (it as Number).toFloat() }?.toFloatArray()
                    val entity = StudentBiometricEntity(
                        studentId = doc.id,
                        schoolId = schoolId,
                        name = doc.getString("name") ?: "",
                        embedding = embedding,
                        photoUrl = doc.getString("photoUrl"),
                        isSynced = true,
                    )
                    Pair(entity, doc.getBoolean("isActive") ?: true)
                } catch (e: Exception) { null }
            }.toMutableList()

            Result.Success(updatedData)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun uploadBiometricPhoto(schoolId: String, studentId: String, imageBytes: ByteArray): Result<String?> {
        return try {
            val ref = storage.reference.child("schools/$schoolId/faces/$studentId.jpg")
            ref.putBytes(imageBytes).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.Success(downloadUrl)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun bulkSyncBiometrics(schoolId: String, students: List<StudentBiometricEntity>): Result<Unit> {
        return try {
            if (students.isEmpty()) return Result.Success(Unit)
            students.chunked(500).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { student ->
                    val data = hashMapOf(
                        "name" to student.name,
                        "embedding" to student.embedding?.toList(),
                        "photoUrl" to student.photoUrl,
                        "lastUpdated" to FieldValue.serverTimestamp(),
                        "isActive" to !student.isDeleted,
                    )
                    batch.set(getTenantRef(schoolId).collection("master_faces").document(student.studentId), data, SetOptions.merge())
                }
                batch.commit().await()
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun syncStudentAssignment(assignment: StudentClassAssignmentEntity): Result<Unit> {
        return try {
            val docId = "${assignment.studentId}_${assignment.classId}"
            val data = hashMapOf(
                "studentId" to assignment.studentId,
                "classId" to assignment.classId,
                "lastUpdated" to FieldValue.serverTimestamp(),
            )
            // 1. Update dedicated assignments collection
            getTenantRef(assignment.schoolId).collection("student_class_assignments").document(docId).set(data, SetOptions.merge()).await()

            // 2. 🔥 AI Native Fix: Also update the main student document using arrayUnion to prevent 'Last One Wins' overwrite
            try {
                getTenantRef(assignment.schoolId).collection("students").document(assignment.studentId)
                    .update("classIds", FieldValue.arrayUnion(assignment.classId), "lastUpdated", FieldValue.serverTimestamp())
                    .await()
            } catch (e: Exception) {
                // If student doc doesn't exist yet, it will be created by the next full sync
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun deleteStudent(studentId: String, schoolId: String, classIds: List<String>): Result<Unit> {
        return try {
            val batch = db.batch()
            batch.delete(getTenantRef(schoolId).collection("master_faces").document(studentId))
            classIds.forEach { classId ->
                batch.delete(getTenantRef(schoolId).collection("student_class_assignments").document("${studentId}_$classId"))
            }
            batch.commit().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun deleteStudentAssignments(assignment: StudentClassAssignmentEntity): Result<Unit> {
        return try {
            val docId = "${assignment.studentId}_${assignment.classId}"
            getTenantRef(assignment.schoolId).collection("student_class_assignments").document(docId).delete().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun getStudentAssignments(schoolId: String): Result<List<StudentClassAssignmentEntity>> {
        return try {
            val snapshot = getTenantRef(schoolId).collection("student_class_assignments").get().await()
            val assignments = snapshot.documents.mapNotNull { doc ->
                val studentId = doc.getString("studentId") ?: return@mapNotNull null
                val classId = doc.getString("classId") ?: return@mapNotNull null
                StudentClassAssignmentEntity(
                    studentId = studentId,
                    classId = classId,
                    schoolId = schoolId,
                    isSynced = true,
                )
            }
            Result.Success(assignments)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }
}
