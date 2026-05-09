package com.azuratech.azuratime.domain.student.repository

import com.azuratech.azuratime.domain.model.StudentProfile
import com.azuratech.azuratime.domain.model.SyncStatus
import com.azuratech.azuraengine.result.Result
import kotlinx.coroutines.flow.Flow

/**
 * 🏰 STUDENT REPOSITORY INTERFACE
 * The single source of truth for Student Profiles.
 */
interface StudentRepository {
    /**
     * Observe all active student profiles for the current school.
     */
    fun getStudentProfiles(): Flow<List<StudentProfile>>

    /**
     * Create or update a student profile locally and enqueue for remote sync.
     */
    suspend fun saveProfile(profile: StudentProfile): Result<Unit>

    /**
     * Mark a student profile for deletion locally and enqueue for remote sync.
     */
    suspend fun deleteProfile(studentId: String): Result<Unit>

    /**
     * Update the sync status of a student profile after a remote operation.
     */
    suspend fun updateSyncStatus(studentId: String, status: SyncStatus): Result<Unit>

    /**
     * 🔥 SSOT Push: Upload all local changes to cloud.
     */
    suspend fun pushPendingProfiles(): Result<Unit>

    /**
     * 🔥 SSOT Auto-Heal: Ensure every face has a student identity.
     */
    suspend fun autoHealStudentIdentities(schoolId: String): Result<Unit>
}
