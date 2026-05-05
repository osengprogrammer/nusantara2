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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🏛️ STUDENT REPOSITORY IMPLEMENTATION
 * 
 * Repository is SSOT guardian. Room is primary source. Remote sync is side-effect.
 */
@Singleton
class StudentRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val sessionManager: SessionManager
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
                studentDao.upsert(student)
                faceDao.upsertFace(face)
                assignments.forEach { faceAssignmentDao.insertAssignment(it) }
            }
            // TODO: Phase 3 - SyncManager.enqueueSync(profile.studentId)
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
            // TODO: Phase 3 - SyncManager.enqueueSync(studentId)
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
}
