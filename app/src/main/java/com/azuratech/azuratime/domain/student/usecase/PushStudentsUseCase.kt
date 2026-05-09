package com.azuratech.azuratime.domain.student.usecase

import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuratime.data.remote.FaceRemoteDataSource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * UseCase to push local student, face, and assignment changes to the cloud.
 */
class PushStudentsUseCase @Inject constructor(
    private val database: AppDatabase,
    private val remoteDataSource: FaceRemoteDataSource,
    private val firestore: FirebaseFirestore,
    private val sessionManager: SessionManager
) {
    private val studentDao = database.studentDao()
    private val faceDao = database.faceDao()
    private val faceAssignmentDao = database.faceAssignmentDao()

    suspend operator fun invoke(): Result<Unit> = withContext(Dispatchers.IO) {
        val schoolId = sessionManager.getActiveSchoolId() ?: return@withContext Result.Success(Unit)
        
        try {
            // 1. Push Unsynced Students
            val unsyncedStudents = studentDao.getUnsyncedStudents(schoolId)
            for (student in unsyncedStudents) {
                val docRef = firestore.collection("schools").document(schoolId)
                    .collection("students").document(student.studentId)
                
                if (student.isDeleted) {
                    docRef.delete().await()
                } else {
                    val data = mapOf(
                        "studentId" to student.studentId,
                        "schoolId" to student.schoolId,
                        "name" to student.name,
                        "studentCode" to student.studentCode,
                        "classId" to student.classId,
                        "createdAt" to student.createdAt,
                        "lastUpdated" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                    docRef.set(data).await()
                }
                studentDao.upsert(student.copy(isSynced = true))
            }

            // 2. Push Unsynced Faces
            val unsyncedFaces = faceDao.getUnsyncedFaces(schoolId)
            if (unsyncedFaces.isNotEmpty()) {
                val syncResult = remoteDataSource.bulkSyncFaces(schoolId, unsyncedFaces)
                if (syncResult is Result.Success) {
                    unsyncedFaces.forEach { face ->
                        faceDao.upsertFace(face.copy(isSynced = true))
                    }
                }
            }

            // 3. Push Unsynced Assignments
            val unsyncedAssignments = faceAssignmentDao.getUnsyncedAssignments(schoolId)
            for (assignment in unsyncedAssignments) {
                val syncResult = remoteDataSource.syncFaceAssignment(assignment)
                if (syncResult is Result.Success) {
                    faceAssignmentDao.updateSyncStatus(
                        assignment.faceId, 
                        assignment.classId, 
                        schoolId, 
                        true
                    )
                }
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }
}
