package com.azuratech.azuratime.features.session.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * POJO for JOIN results between [ClassSessionEntity] and [SubjectEntity].
 */
data class SessionWithDetails(
    @Embedded val session: ClassSessionEntity,
    val subjectName: String,
    val className: String? = null, // Optional: Can be filled if we join with ClassEntity too
)

@Dao
interface SessionDao {
    /**
     * Returns sessions for a specific day with their subject details.
     * Uses lookupKey-style logic (dayOfWeek) for fast daily schedule queries.
     */
    @Query(
        """
        SELECT s.*, subj.name as subjectName 
        FROM class_sessions s 
        INNER JOIN subjects subj ON s.subjectId = subj.subjectId 
        WHERE s.schoolId = :schoolId AND s.dayOfWeek = :day AND s.isActive = 1
        ORDER BY s.startTime ASC
    """,
    )
    fun getSessionsByDayFlow(schoolId: String, day: Int): Flow<List<SessionWithDetails>>

    @Query("SELECT * FROM class_sessions WHERE sessionId = :sessionId AND isActive = 1")
    suspend fun getSessionById(sessionId: String): ClassSessionEntity?

    @Query("SELECT * FROM subjects WHERE schoolId = :schoolId ORDER BY name ASC")
    fun observeAllSubjectsFlow(schoolId: String): Flow<List<SubjectEntity>>

    @Query(
        """
        SELECT s.*, subj.name as subjectName 
        FROM class_sessions s 
        INNER JOIN subjects subj ON s.subjectId = subj.subjectId 
        WHERE s.schoolId = :schoolId AND s.isActive = 1
        ORDER BY s.dayOfWeek ASC, s.startTime ASC
    """,
    )
    fun observeAllSessionsFlow(schoolId: String): Flow<List<SessionWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ClassSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity)

    @Query("UPDATE class_sessions SET isActive = 0 WHERE sessionId = :sessionId")
    suspend fun softDeleteSession(sessionId: String)

    @Query("SELECT COUNT(*) FROM check_in_records WHERE sessionId = :sessionId")
    suspend fun getAttendanceCountForSession(sessionId: String): Int

    @Query("SELECT COUNT(*) FROM class_sessions WHERE subjectId = :subjectId AND isActive = 1")
    suspend fun getSessionCountForSubject(subjectId: String): Int

    @Delete
    suspend fun deleteSubject(subject: SubjectEntity)
}
