package com.azuratech.azuratime.features.student.domain.repository

import com.azuratech.azuratime.features.student.domain.model.StudentProfile
import com.azuratech.azuratime.core.domain.model.SyncStatus
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
    fun getStudentProfiles(): Flow<Result<List<StudentProfile>>>

    /**
     * 🔥 One-shot fetch: Get all active student profiles for the current school.
     */
    suspend fun getAll(): Result<List<StudentProfile>>

    /**
     * Get a single student profile by ID.
     */
    suspend fun getProfileById(studentId: String): Result<StudentProfile?>

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

    /**
     * 🔥 SSOT Pull: Fetch all students from cloud to local Room.
     */
    suspend fun pullStudents(schoolId: String): Result<Unit>
}
