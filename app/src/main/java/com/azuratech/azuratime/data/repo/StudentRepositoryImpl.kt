package com.azuratech.azuratime.data.repo

import androidx.room.withTransaction
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.*
import com.azuratech.azuratime.domain.model.StudentProfile
import com.azuratech.azuratime.domain.model.SyncStatus
import com.azuratech.azuratime.domain.student.repository.StudentRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

import com.azuratech.azuratime.core.sync.SyncManager

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
    private val remoteDataSource: com.azuratech.azuratime.data.remote.FaceRemoteDataSource
) : StudentRepository {

    private val studentDao = database.studentDao()
    private val faceDao = database.faceDao()
    private val faceAssignmentDao = database.faceAssignmentDao()

    override fun getStudentProfiles(): Flow<List<StudentProfile>> {
        val schoolId = sessionManager.getActiveSchoolId() ?: ""
        return studentDao.getStudentProfilesFlow(schoolId)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun saveProfile(profile: StudentProfile): Result<Unit> {
        return try {
            val (student, face, assignments) = profile.toEntities()
            database.withTransaction {
                // 1. Save Core Entities
                studentDao.upsert(student)
                faceDao.upsertFace(face)
                
                // 2. Clear existing assignments for this face (prevent orphans)
                // We use the faceId from the generated entity for consistency
                faceAssignmentDao.deleteAllByFaceId(face.faceId)
                
                // 3. Insert new assignments
                assignments.forEach { faceAssignmentDao.insertAssignment(it) }
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
                // KDoc: Soft-delete cascade: deleting student marks face + assignments as isDeleted
                studentDao.markPendingDeletion(studentId, schoolId)
                val face = faceDao.getFaceByStudentId(studentId, schoolId)
                if (face != null) {
                    faceDao.markPendingDeletion(face.faceId, schoolId)
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
                
                val face = faceDao.getFaceByStudentId(studentId, schoolId)
                if (face != null) {
                    val updatedFace = when(status) {
                        SyncStatus.SYNCED -> face.copy(isSynced = true, isDeleted = false)
                        SyncStatus.PENDING_DELETE -> face.copy(isSynced = false, isDeleted = true)
                        else -> face.copy(isSynced = false)
                    }
                    faceDao.upsertFace(updatedFace)
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

            val unsyncedFaces = faceDao.getUnsyncedFaces(schoolId)
            if (unsyncedFaces.isNotEmpty()) {
                val syncResult = remoteDataSource.bulkSyncFaces(schoolId, unsyncedFaces)
                if (syncResult is Result.Success) {
                    unsyncedFaces.forEach { face ->
                        faceDao.upsertFace(face.copy(isSynced = true))
                    }
                }
            }

            val unsyncedAssignments = faceAssignmentDao.getUnsyncedAssignments(schoolId)
            for (assignment in unsyncedAssignments) {
                val syncResult = remoteDataSource.syncFaceAssignment(assignment)
                if (syncResult is Result.Success) {
                    faceAssignmentDao.updateSyncStatus(assignment.faceId, assignment.classId, schoolId, true)
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun autoHealStudentIdentities(schoolId: String): Result<Unit> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val faces = faceDao.getAllFacesForScanningList(schoolId)
            val studentsToCreate = faces.map { face ->
                StudentEntity(
                    studentId = face.studentId ?: face.faceId,
                    schoolId = schoolId,
                    name = face.name,
                    createdAt = face.createdAt,
                    isSynced = true
                )
            }
            if (studentsToCreate.isNotEmpty()) {
                studentDao.upsertAll(studentsToCreate)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }
}
