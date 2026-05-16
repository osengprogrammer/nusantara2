package com.azuratech.azuratime.features.biometric.domain.repository

import android.app.Application
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.data.local.BiometricCache
import com.azuratech.azuratime.core.data.local.StudentBiometricDetails
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.biometric.data.local.StudentBiometricEntity
import com.azuratech.azuratime.features.biometric.data.local.StudentClassAssignmentEntity
import com.azuratech.azuratime.features.biometric.data.remote.BiometricRemoteDataSource
import com.azuratech.azuratime.features.biometric.data.local.BiometricLocalDataSource
import com.azuratech.azuratime.features.student.domain.model.StudentProfile
import com.azuratech.azuratime.features.student.domain.repository.StudentRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudentBiometricRepository @Inject constructor(
    private val application: Application,
    private val localDataSource: BiometricLocalDataSource,
    private val remoteDataSource: BiometricRemoteDataSource,
    private val sessionManager: SessionManager,
    private val studentRepository: StudentRepository
) {
    private val schoolId: String
        get() = sessionManager.getActiveSchoolId() ?: ""

    // Delegation methods for DAOs / DataSources
    fun observeEnrollmentsBySchool(schoolId: String) = localDataSource.getAllStudentsFlow(schoolId)

    suspend fun submitEnrollment(studentId: String, photoUri: String): Result<Unit> {
        return try {
            val biometric = localDataSource.getStudentFaceByIdentity(studentId, schoolId)
            if (biometric != null) {
                localDataSource.upsertStudentFace(biometric.copy(photoUrl = photoUri, isSynced = false))
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(com.azuratech.azuraengine.result.AppError.LocalDB(e.message))
        }
    }

    suspend fun deleteEnrollment(studentId: String): Result<Unit> {
        return try {
            localDataSource.markPendingDeletion(studentId, schoolId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(com.azuratech.azuraengine.result.AppError.LocalDB(e.message))
        }
    }

    fun getAllStudentsFlow(schoolId: String) = localDataSource.getAllStudentsFlow(schoolId)
    fun getStudentsWithDetailsFlow(schoolId: String) = localDataSource.getAllStudentsWithDetailsFlow(schoolId)
    fun getAllAssignmentsFlow(schoolId: String) = localDataSource.getAllAssignmentsFlow(schoolId)
    fun getEnrolledStudentsFlow(schoolId: String) = localDataSource.getAllStudentsForScanningFlow(schoolId)
    fun getStudentsInClassFlow(classId: String, schoolId: String) = localDataSource.getStudentsInClassFlow(classId, schoolId)
    suspend fun getStudentWithDetails(studentId: String, schoolId: String) = localDataSource.getStudentWithDetails(studentId, schoolId)
    suspend fun getClassIdsForStudent(studentId: String, schoolId: String) = localDataSource.getClassIdsForStudent(studentId, schoolId)
    suspend fun deleteAssignmentsByStudent(studentId: String, schoolId: String) = localDataSource.deleteAssignmentsByStudent(studentId, schoolId)
    suspend fun insertAssignment(assignment: StudentClassAssignmentEntity) = localDataSource.insertAssignment(assignment)
    suspend fun upsertStudentBiometric(studentBiometric: StudentBiometricEntity) = localDataSource.upsertStudentFace(studentBiometric)

    suspend fun assignStudentToClass(studentId: String, classId: String): Result<Unit> = try {
        localDataSource.insertAssignment(StudentClassAssignmentEntity(studentId, classId, schoolId))
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(com.azuratech.azuraengine.result.AppError.LocalDB(e.message))
    }

    suspend fun removeStudentFromClass(studentId: String, @Suppress("UNUSED_PARAMETER") classId: String): Result<Unit> = try {
        localDataSource.deleteAssignmentsByStudent(studentId, schoolId) 
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
            localDataSource.insertAssignment(StudentClassAssignmentEntity(studentId, classId, schoolId))
        }
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(com.azuratech.azuraengine.result.AppError.LocalDB(e.message))
    }

    suspend fun saveStudentProfile(profile: StudentProfile, @Suppress("UNUSED_PARAMETER") photoBytes: ByteArray? = null): Result<Unit> {
        return studentRepository.saveProfile(profile)
    }
    
    suspend fun syncBiometrics(): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val lastSync = sessionManager.getLastFacesSyncTime()
            val syncResult = remoteDataSource.getBiometricUpdates(schoolId, lastSync)
            
            if (syncResult is Result.Success) {
                val updatedData = syncResult.data
                if (updatedData.isNotEmpty()) {
                    val uniqueBiometrics = updatedData.filter { it.second }.map { it.first }
                        .groupBy { it.studentId }
                        .map { entry ->
                            val itemsForStudent = entry.value
                            itemsForStudent.maxByOrNull { it.lastUpdated } 
                                ?: itemsForStudent.first()
                        }

                    val toDelete = updatedData.filter { !it.second }.map { it.first }

                    if (uniqueBiometrics.isNotEmpty()) localDataSource.upsertAllStudentFaces(uniqueBiometrics)
                    if (toDelete.isNotEmpty()) {
                        toDelete.forEach { localDataSource.deleteStudentFaceById(it.studentId, schoolId) }
                    }

                    BiometricCache.refresh(application, schoolId)
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

    suspend fun syncAssignments(): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        Result.Success(Unit)
    }

    // Remote delegation
    suspend fun syncStudentAssignment(assignment: StudentClassAssignmentEntity) = remoteDataSource.syncStudentAssignment(assignment)
    suspend fun bulkSyncBiometrics(schoolId: String, biometrics: List<StudentBiometricEntity>) = remoteDataSource.bulkSyncBiometrics(schoolId, biometrics)
    suspend fun uploadBiometricPhoto(schoolId: String, studentId: String, imageBytes: ByteArray) = remoteDataSource.uploadBiometricPhoto(schoolId, studentId, imageBytes)
}
