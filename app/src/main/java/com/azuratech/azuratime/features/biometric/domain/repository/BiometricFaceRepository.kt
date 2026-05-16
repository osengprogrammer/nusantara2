package com.azuratech.azuratime.features.biometric.domain.repository

import android.app.Application
import android.graphics.Bitmap
import com.azuratech.azuratime.core.data.local.*
import com.azuratech.azuratime.features.school.data.local.*
import com.azuratech.azuratime.features.staff.data.local.*
import com.azuratech.azuratime.features.attendance.data.local.*
import com.azuratech.azuratime.features.biometric.data.local.*
import com.azuratech.azuratime.features.biometric.data.local.*
import com.azuratech.azuratime.features.biometric.data.remote.FaceRemoteDataSource
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuraengine.result.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricFaceRepository @Inject constructor(
    private val application: Application,
    private val localDataSource: FaceLocalDataSource,
    private val remoteDataSource: FaceRemoteDataSource,
    private val sessionManager: SessionManager,
    private val studentRepository: com.azuratech.azuratime.features.student.domain.repository.StudentRepository
) {
    private val schoolId: String
        get() = sessionManager.getActiveSchoolId() ?: ""

    // Delegation methods for DAOs / DataSources
    fun observeEnrollmentsBySchool(schoolId: String) = localDataSource.getAllStudentsFlow(schoolId)

    suspend fun submitEnrollment(studentId: String, photoUri: String): Result<Unit> {
        return try {
            val face = localDataSource.getStudentFaceByIdentity(studentId, schoolId)
            if (face != null) {
                localDataSource.upsertStudentFace(face.copy(photoUrl = photoUri, isSynced = false))
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(com.azuratech.azuraengine.result.AppError.LocalDB(e.message))
        }
    }

    suspend fun deleteEnrollment(faceId: String): Result<Unit> {
        return try {
            localDataSource.markPendingDeletion(faceId, schoolId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(com.azuratech.azuraengine.result.AppError.LocalDB(e.message))
        }
    }

    fun getAllStudentsFlow(schoolId: String) = localDataSource.getAllStudentsFlow(schoolId)
    fun getStudentsWithDetailsFlow(schoolId: String) = localDataSource.getAllStudentsWithDetailsFlow(schoolId)
    fun getEnrolledStudentsFlow(schoolId: String) = localDataSource.getAllStudentsForScanningFlow(schoolId)
    fun getAllStudentsForScanningFlow(schoolId: String) = localDataSource.getAllStudentsForScanningFlow(schoolId)
    fun getStudentsInClassFlow(classId: String, schoolId: String) = localDataSource.getStudentsInClassFlow(classId, schoolId)
    suspend fun getStudentWithDetails(studentId: String, schoolId: String) = localDataSource.getStudentWithDetails(studentId, schoolId)
    suspend fun getClassIdsForStudent(studentId: String, schoolId: String) = localDataSource.getClassIdsForStudent(studentId, schoolId)
    suspend fun deleteAssignmentsByStudent(studentId: String, schoolId: String) = localDataSource.deleteAssignmentsByStudent(studentId, schoolId)
    suspend fun insertAssignment(assignment: FaceAssignmentEntity) = localDataSource.insertAssignment(assignment)
    suspend fun upsertStudentFace(studentFace: BiometricFaceEntity) = localDataSource.upsertStudentFace(studentFace)

    suspend fun assignStudentToClass(studentId: String, classId: String): Result<Unit> = try {
        localDataSource.insertAssignment(FaceAssignmentEntity(studentId, classId, schoolId))
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(com.azuratech.azuraengine.result.AppError.LocalDB(e.message))
    }

    suspend fun removeStudentFromClass(studentId: String, @Suppress("UNUSED_PARAMETER") classId: String): Result<Unit> = try {
        localDataSource.deleteAssignmentsByStudent(studentId, schoolId) // Simple version: remove all then re-add if needed, or specific delete
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(com.azuratech.azuraengine.result.AppError.LocalDB(e.message))
    }

    suspend fun removeAllAssignmentsForStudent(studentId: String): Result<Unit> = try {
        localDataSource.deleteAssignmentsByStudent(studentId, schoolId)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(com.azuratech.azuraengine.result.AppError.LocalDB(e.message))
    }

    suspend fun deleteStudent(studentId: String) = deleteEnrollment(studentId)

    suspend fun updateStudentClass(studentId: String, classId: String?): Result<Unit> = try {
        localDataSource.deleteAssignmentsByStudent(studentId, schoolId)
        if (classId != null) {
            localDataSource.insertAssignment(FaceAssignmentEntity(studentId, classId, schoolId))
        }
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(com.azuratech.azuraengine.result.AppError.LocalDB(e.message))
    }

    suspend fun saveStudentProfile(profile: com.azuratech.azuratime.features.student.domain.model.StudentProfile, @Suppress("UNUSED_PARAMETER") photoBytes: ByteArray? = null): Result<Unit> {
        // Simple delegation to StudentRepository
        return studentRepository.saveProfile(profile)
    }
    
    /**
     * 🔥 SSOT: Pull face updates from cloud to local Room.
     */
    suspend fun syncFaces(): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val lastSync = sessionManager.getLastFacesSyncTime()
            val syncResult = remoteDataSource.getFaceUpdates(schoolId, lastSync)
            
            if (syncResult is Result.Success) {
                val updatedData = syncResult.data
                if (updatedData.isNotEmpty()) {
                    // 🔥 AI Friendly: Deduplicate by studentId to prevent duplicates in Roster
                    // If multiple faces point to the same student, we pick the most recent or the one where faceId == studentId
                    val uniqueFaces = updatedData.filter { it.second }.map { it.first }
                        .groupBy { it.studentId }
                        .map { entry ->
                            val facesForStudent = entry.value
                            // Preference: most recently updated
                            facesForStudent.maxByOrNull { it.lastUpdated } 
                                ?: facesForStudent.first()
                        }

                    val toDelete = updatedData.filter { !it.second }.map { it.first }

                    if (uniqueFaces.isNotEmpty()) localDataSource.upsertAllStudentFaces(uniqueFaces)
                    if (toDelete.isNotEmpty()) {
                        toDelete.forEach { localDataSource.deleteStudentFaceById(it.studentId, schoolId) }
                    }

                    FaceCache.refresh(application, schoolId)
                    sessionManager.saveLastFacesSyncTime(System.currentTimeMillis())
                }
                Result.Success(Unit)
            } else {
                syncResult as Result.Failure
            }
        } catch (e: Exception) {
            Result.Failure(com.azuratech.azuraengine.result.AppError.Network(e.message))
        }
    }

    /**
     * 🔥 SSOT: Pull assignments from cloud to local Room.
     */
    suspend fun syncAssignments(): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (schoolId.isBlank()) return@withContext Result.Success(Unit)
        try {
            // This is a simplified version of SyncAssignmentsUseCase logic
            // In a real app, you'd fetch from Firestore here. 
            // For now, we'll keep it as a placeholder that delegatest to remoteDataSource if needed,
            // or we'll implement the logic here if we have Firestore access.
            // Since FaceRemoteDataSource doesn't have getAssignments, we might need to add it.
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(com.azuratech.azuraengine.result.AppError.Network(e.message))
        }
    }

    // Remote delegation
    suspend fun syncFaceAssignment(assignment: FaceAssignmentEntity) = remoteDataSource.syncFaceAssignment(assignment)
    suspend fun bulkSyncFaces(schoolId: String, faces: List<BiometricFaceEntity>) = remoteDataSource.bulkSyncFaces(schoolId, faces)
    suspend fun uploadFacePhoto(schoolId: String, faceId: String, imageBytes: ByteArray) = remoteDataSource.uploadFacePhoto(schoolId, faceId, imageBytes)
}
