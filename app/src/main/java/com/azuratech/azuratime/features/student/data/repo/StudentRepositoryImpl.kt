package com.azuratech.azuratime.features.student.data.repo

import androidx.room.withTransaction
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.data.local.*
import com.azuratech.azuratime.features.school.data.local.*
import com.azuratech.azuratime.features.account.data.local.*
import com.azuratech.azuratime.features.attendance.data.local.*
import com.azuratech.azuratime.features.biometric.data.local.*
import com.azuratech.azuratime.features.student.data.local.StudentDao
import com.azuratech.azuratime.features.student.data.local.StudentEntity
import com.azuratech.azuratime.features.student.domain.model.StudentProfile
import com.azuratech.azuratime.core.domain.model.SyncStatus
import com.azuratech.azuratime.features.student.domain.repository.StudentRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

import com.azuratech.azuratime.core.sync.SyncManager
import com.azuratech.azuratime.features.biometric.data.remote.BiometricRemoteDataSource

/**
 * 🏛️ STUDENT REPOSITORY IMPLEMENTATION
 * 
 * Repository is SSOT guardian. Room is primary source. Remote sync is side-effect.
 */
@Singleton
class StudentRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val sessionManager: SessionManager,
    private val syncManager: SyncManager,
    private val firestore: com.google.firebase.firestore.FirebaseFirestore,
    private val remoteDataSource: BiometricRemoteDataSource
) : StudentRepository {

    private val studentDao = database.studentDao()
    private val biometricDao = database.biometricDao()
    private val assignmentDao = database.studentClassAssignmentDao()

    override fun getStudentProfiles(): Flow<List<StudentProfile>> {
        val schoolId = sessionManager.getActiveSchoolId() ?: ""
        return studentDao.getStudentProfilesFlow(schoolId)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun saveProfile(profile: StudentProfile): Result<Unit> {
        return try {
            val (student, biometric, assignments) = profile.toEntities()
            database.withTransaction {
                // 1. Save Core Entities
                studentDao.upsert(student)
                
                // 🔥 AI Friendly: Clear legacy biometrics with different studentId but same studentId (unified)
                biometricDao.deleteOtherBiometricsForStudent(student.studentId, biometric.studentId, student.schoolId)
                biometricDao.upsertStudentBiometric(biometric)
                
                // 2. Clear existing assignments for this student (prevent orphans)
                assignmentDao.deleteAllByStudentId(biometric.studentId)
                
                // 3. Insert new assignments
                for (assignment in assignments) {
                    assignmentDao.insertAssignment(assignment)
                }
            }
            
            // 🔥 Phase 3 - Trigger background sync immediately
            syncManager.enqueueSync()
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun deleteProfile(studentId: String): Result<Unit> {
        return try {
            val schoolId = sessionManager.getActiveSchoolId() ?: ""
            database.withTransaction {
                val student = studentDao.getById(studentId, schoolId)
                if (student?.isSynced == true) {
                     // Soft-delete if already synced to cloud
                     studentDao.markPendingDeletion(studentId, schoolId)
                     val biometric = biometricDao.getStudentBiometricByIdentity(studentId, schoolId)
                     if (biometric != null) {
                         biometricDao.markPendingDeletion(biometric.studentId, schoolId)
                     }
                } else {
                     // Hard-delete if only local
                     val biometric = biometricDao.getStudentBiometricByIdentity(studentId, schoolId)
                     if (biometric != null) {
                         assignmentDao.deleteAllByStudentId(biometric.studentId)
                         biometricDao.deleteStudentBiometricById(biometric.studentId, schoolId)
                     }
                     studentDao.deleteById(studentId, schoolId)
                }
            }
            
            // 🔥 Phase 3 - Trigger background sync immediately
            syncManager.enqueueSync()
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun updateSyncStatus(studentId: String, status: SyncStatus): Result<Unit> {
        return try {
            val schoolId = sessionManager.getActiveSchoolId() ?: ""
            database.withTransaction {
                val student = studentDao.getById(studentId, schoolId)
                if (student != null) {
                    val updatedStudent = when(status) {
                        SyncStatus.SYNCED -> student.copy(isSynced = true, isDeleted = false)
                        SyncStatus.PENDING_DELETE -> student.copy(isSynced = false, isDeleted = true)
                        else -> student.copy(isSynced = false)
                    }
                    studentDao.upsert(updatedStudent)
                }
                
                val biometric = biometricDao.getStudentBiometricByIdentity(studentId, schoolId)
                if (biometric != null) {
                    val updatedBiometric = when(status) {
                        SyncStatus.SYNCED -> biometric.copy(isSynced = true, isDeleted = false)
                        SyncStatus.PENDING_DELETE -> biometric.copy(isSynced = false, isDeleted = true)
                        else -> biometric.copy(isSynced = false)
                    }
                    biometricDao.upsertStudentBiometric(updatedBiometric)
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun pushPendingProfiles(): Result<Unit> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val schoolId = sessionManager.getActiveSchoolId() ?: return@withContext Result.Success(Unit)
        try {
            // Logic from PushStudentsUseCase
            val unsyncedStudents = studentDao.getUnsyncedStudents(schoolId)
            for (student in unsyncedStudents) {
                val docRef = firestore.collection("schools").document(schoolId)
                    .collection("students").document(student.studentId)
                
                if (student.isDeleted) {
                    com.google.android.gms.tasks.Tasks.await(docRef.delete())
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
                    com.google.android.gms.tasks.Tasks.await(docRef.set(data))
                }
                studentDao.upsert(student.copy(isSynced = true))
            }

            val unsyncedBiometrics = biometricDao.getUnsyncedBiometrics(schoolId)
            if (unsyncedBiometrics.isNotEmpty()) {
                val syncResult = remoteDataSource.bulkSyncBiometrics(schoolId, unsyncedBiometrics)
                if (syncResult is Result.Success) {
                    unsyncedBiometrics.forEach { biometric ->
                        biometricDao.upsertStudentBiometric(biometric.copy(isSynced = true))
                    }
                }
            }

            val unsyncedAssignments = assignmentDao.getUnsyncedAssignments(schoolId)
            for (assignment in unsyncedAssignments) {
                val syncResult = remoteDataSource.syncStudentAssignment(assignment)
                if (syncResult is Result.Success) {
                    assignmentDao.updateSyncStatus(assignment.studentId, assignment.classId, schoolId, true)
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun autoHealStudentIdentities(schoolId: String): Result<Unit> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val biometrics = biometricDao.getAllStudentsForScanningList(schoolId)
            val studentsToCreate = mutableListOf<StudentEntity>()
            
            for (biometric in biometrics) {
                val targetStudentId = biometric.studentId
                val existing = studentDao.getById(targetStudentId, schoolId)
                
                if (existing == null) {
                    studentsToCreate.add(
                        StudentEntity(
                            studentId = targetStudentId,
                            schoolId = schoolId,
                            name = biometric.name,
                            createdAt = biometric.createdAt,
                            isSynced = true
                        )
                    )
                }
            }
            
            if (studentsToCreate.isNotEmpty()) {
                studentDao.upsertAll(studentsToCreate)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun pullStudents(schoolId: String): Result<Unit> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("schools").document(schoolId)
                .collection("students").get().let { com.google.android.gms.tasks.Tasks.await(it) }
            
            val students = snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("studentId") ?: return@mapNotNull null
                StudentEntity(
                    studentId = id,
                    schoolId = schoolId,
                    name = doc.getString("name") ?: "",
                    studentCode = doc.getString("studentCode"),
                    classId = doc.getString("classId"),
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    isSynced = true
                )
            }
            
            if (students.isNotEmpty()) {
                studentDao.upsertAll(students)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }
}
