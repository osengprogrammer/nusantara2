package com.azuratech.azuratime.features.session.domain.repository

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.session.data.local.ClassSessionEntity
import com.azuratech.azuratime.features.session.data.local.SessionWithDetails
import com.azuratech.azuratime.features.session.data.local.SubjectEntity
import kotlinx.coroutines.flow.Flow

/**
 * 📅 SESSION REPOSITORY (v3.3.0-ai-native)
 * Decouples attendance from physical classes by attaching it to Subject Sessions.
 */
interface SessionRepository {
    /**
     * 🔥 AI Native: Observe sessions for a specific day with their subject details.
     */
    fun getSessionsByDayFlow(schoolId: String, day: Int): Flow<Result<List<SessionWithDetails>>>

    /**
     * 🔥 AI Native Optimization: Resolves the active session using database-level filtering.
     */
    suspend fun getActiveSessionOptimized(
        schoolId: String,
        email: String,
        day: Int,
        currentTime: String,
    ): Result<SessionWithDetails?>

    suspend fun getSessionById(sessionId: String): Result<ClassSessionEntity>

    suspend fun saveSession(session: ClassSessionEntity): Result<Unit>
    suspend fun updateSession(session: ClassSessionEntity): Result<Unit>
    suspend fun saveSubject(subject: SubjectEntity): Result<Unit>

    suspend fun deleteSession(session: ClassSessionEntity): Result<Unit>

    /**
     * 🔥 Production Hardening: Soft delete session to prevent orphaned attendance records.
     */
    suspend fun softDeleteSession(sessionId: String): Result<Unit>

    /**
     * 🔥 Data Integrity: Only allow subject deletion if no active sessions are linked.
     */
    suspend fun deleteSubject(subject: SubjectEntity): Result<Unit>

    fun observeAllSubjectsFlow(schoolId: String): Flow<Result<List<SubjectEntity>>>
    fun observeAllSessionsFlow(schoolId: String): Flow<Result<List<SessionWithDetails>>>

    /**
     * 🔥 Data Integrity: Check if any attendance records exist for this session.
     */
    suspend fun getAttendanceCountForSession(sessionId: String): Result<Int>

    /**
     * 🔥 RBAC: Validates if the current supervisor has access to the session.
     */
    suspend fun validateSessionAccess(sessionId: String): Result<Boolean>

    /**
     * 🔥 Sync Engine: Synchronize subjects between local and remote.
     */
    suspend fun syncSubjects(): Result<Unit>

    /**
     * 🔥 Sync Engine: Synchronize sessions between local and remote.
     */
    suspend fun syncSessions(): Result<Unit>
}
