package com.azuratech.azuratime.features.reporting.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExportJobDao {
    @Query("SELECT * FROM export_jobs WHERE accountId = :accountId ORDER BY jobId DESC")
    fun observeExportJobsByAccount(accountId: String): Flow<List<ExportJobEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: ExportJobEntity)

    @Update
    suspend fun updateJob(job: ExportJobEntity)

    @Query("DELETE FROM export_jobs WHERE accountId = :accountId AND status = 'COMPLETED'")
    suspend fun clearCompletedJobs(accountId: String)
}
