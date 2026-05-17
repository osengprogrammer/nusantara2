package com.azuratech.azuratime.features.reporting.data.local

import kotlinx.coroutines.flow.Flow

@Dao
interface ExportJobDao {
    @Query("SELECT * FROM export_jobs WHERE userId = :userId ORDER BY jobId DESC")
    fun observeExportJobsByUser(userId: String): Flow<List<ExportJobEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: ExportJobEntity)

    @Update
    suspend fun updateJob(job: ExportJobEntity)

    @Query("DELETE FROM export_jobs WHERE userId = :userId AND status = 'COMPLETED'")
    suspend fun clearCompletedJobs(userId: String)
}
