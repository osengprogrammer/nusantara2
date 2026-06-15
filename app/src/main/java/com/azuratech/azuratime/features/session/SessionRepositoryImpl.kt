package com.azuratech.azuratime.features.session

import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuraengine.result.asLocalResult
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.sync.SyncManager
import com.azuratech.azuratime.features.session.data.local.ClassSessionEntity
import com.azuratech.azuratime.features.session.data.local.SessionDao
import com.azuratech.azuratime.features.session.data.local.SessionWithDetails
import com.azuratech.azuratime.features.session.data.local.SubjectEntity
import com.azuratech.azuratime.features.session.data.remote.SessionRemoteDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao,
    private val sessionManager: SessionManager,
    private val remoteDataSource: SessionRemoteDataSource,
    private val syncManager: SyncManager,
) : SessionRepository {

    override fun getSessionsByDayFlow(schoolId: String, day: Int): Flow<Result<List<SessionWithDetails>>> {
        return sessionDao.getSessionsByDayFlow(schoolId, day).asLocalResult()
    }

    override suspend fun getSessionById(sessionId: String): Result<ClassSessionEntity> {
        return try {
            val session = sessionDao.getSessionById(sessionId)
            if (session != null) {
                Result.Success(session)
            } else {
                Result.Failure(AppError.LocalDB("Session not found"))
            }
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message ?: "Failed to get session"))
        }
    }

    override suspend fun saveSession(session: ClassSessionEntity): Result<Unit> {
        return try {
            sessionDao.insertSession(session.copy(isSynced = false))
            syncManager.enqueueSync()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message ?: "Failed to save session"))
        }
    }

    override suspend fun saveSubject(subject: SubjectEntity): Result<Unit> {
        return try {
            sessionDao.insertSubject(subject.copy(isSynced = false))
            syncManager.enqueueSync()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message ?: "Failed to save subject"))
        }
    }

    override suspend fun softDeleteSession(sessionId: String): Result<Unit> {
        return try {
            sessionDao.softDeleteSession(sessionId)
            syncManager.enqueueSync()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message ?: "Failed to soft delete session"))
        }
    }

    override suspend fun deleteSession(session: ClassSessionEntity): Result<Unit> {
        return softDeleteSession(session.sessionId)
    }

    override suspend fun deleteSubject(subject: SubjectEntity): Result<Unit> {
        return try {
            val sessionCount = sessionDao.getSessionCountForSubject(subject.subjectId)
            if (sessionCount > 0) {
                Result.Failure(AppError.BusinessRule("Cannot delete subject with active sessions."))
            } else {
                sessionDao.softDeleteSubject(subject.subjectId)
                syncManager.enqueueSync()
                Result.Success(Unit)
            }
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message ?: "Failed to delete subject"))
        }
    }

    override fun observeAllSubjectsFlow(schoolId: String): Flow<Result<List<SubjectEntity>>> {
        return sessionDao.observeAllSubjectsFlow(schoolId).asLocalResult()
    }

    override fun observeAllSessionsFlow(schoolId: String): Flow<Result<List<SessionWithDetails>>> {
        return sessionDao.observeAllSessionsFlow(schoolId).asLocalResult()
    }

    override suspend fun getAttendanceCountForSession(sessionId: String): Result<Int> {
        return try {
            val count = sessionDao.getAttendanceCountForSession(sessionId)
            Result.Success(count)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message ?: "Failed to get attendance count"))
        }
    }

    override suspend fun validateSessionAccess(sessionId: String): Result<Boolean> {
        return try {
            val session = sessionDao.getSessionById(sessionId)
            val currentEmail = sessionManager.getAccountEmail()

            if (session != null && session.supervisorEmail == currentEmail) {
                Result.Success(true)
            } else {
                // If session exists but email doesn't match, access denied
                // If session doesn't exist, we can't validate, so false
                Result.Success(false)
            }
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message ?: "Validation error"))
        }
    }

    override suspend fun syncSubjects(): Result<Unit> = withContext(Dispatchers.IO) {
        val schoolId = sessionManager.getActiveSchoolId() ?: return@withContext Result.Success(Unit)

        // 1. PUSH: Unsynced local subjects to Remote
        try {
            val unsynced = sessionDao.getUnsyncedSubjects(schoolId)
            for (subject in unsynced) {
                val pushResult = remoteDataSource.syncSubject(subject)
                if (pushResult is Result.Success) {
                    sessionDao.insertSubject(subject.copy(isSynced = true))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SessionRepo", "❌ Push Subjects failed: ${e.message}")
        }

        // 2. PULL: Remote subjects to Local
        try {
            val remoteResult = remoteDataSource.getSubjectUpdates(schoolId)
            if (remoteResult is Result.Success) {
                val remoteSubjects = remoteResult.data
                val unsyncedIds = sessionDao.getUnsyncedSubjects(schoolId).map { it.subjectId }.toSet()

                // 🔥 AI Native Protection: Do NOT overwrite local unsynced changes
                val subjectsToInsert = remoteSubjects.filter { it.subjectId !in unsyncedIds }
                sessionDao.insertSubjects(subjectsToInsert)
                Result.Success(Unit)
            } else {
                remoteResult as Result.Failure
            }
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun syncSessions(): Result<Unit> = withContext(Dispatchers.IO) {
        val schoolId = sessionManager.getActiveSchoolId() ?: return@withContext Result.Success(Unit)

        // 1. PUSH: Unsynced local sessions to Remote
        try {
            val unsynced = sessionDao.getUnsyncedSessions(schoolId)
            for (session in unsynced) {
                val pushResult = remoteDataSource.syncSession(session)
                if (pushResult is Result.Success) {
                    sessionDao.insertSession(session.copy(isSynced = true))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SessionRepo", "❌ Push Sessions failed: ${e.message}")
        }

        // 2. PULL: Remote sessions to Local
        try {
            val remoteResult = remoteDataSource.getSessionUpdates(schoolId)
            if (remoteResult is Result.Success) {
                val remoteSessions = remoteResult.data
                val unsyncedIds = sessionDao.getUnsyncedSessions(schoolId).map { it.sessionId }.toSet()

                // 🔥 AI Native Protection: Do NOT overwrite local unsynced changes
                val sessionsToInsert = remoteSessions.filter { it.sessionId !in unsyncedIds }
                sessionDao.insertSessions(sessionsToInsert)
                Result.Success(Unit)
            } else {
                remoteResult as Result.Failure
            }
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }
}
