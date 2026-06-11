package com.azuratech.azuratime.features.session

import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuraengine.result.asLocalResult
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.session.data.local.ClassSessionEntity
import com.azuratech.azuratime.features.session.data.local.SessionDao
import com.azuratech.azuratime.features.session.data.local.SessionWithDetails
import com.azuratech.azuratime.features.session.data.local.SubjectEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao,
    private val sessionManager: SessionManager,
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
            sessionDao.insertSession(session)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message ?: "Failed to save session"))
        }
    }

    override suspend fun saveSubject(subject: SubjectEntity): Result<Unit> {
        return try {
            sessionDao.insertSubject(subject)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message ?: "Failed to save subject"))
        }
    }

    override suspend fun softDeleteSession(sessionId: String): Result<Unit> {
        return try {
            sessionDao.softDeleteSession(sessionId)
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
                sessionDao.deleteSubject(subject)
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
}
