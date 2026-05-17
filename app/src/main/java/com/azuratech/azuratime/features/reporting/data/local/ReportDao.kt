package com.azuratech.azuratime.features.reporting.data.local

import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Query("SELECT * FROM reports WHERE schoolId = :schoolId ORDER BY startDate DESC")
    fun observeReportsBySchool(schoolId: String): Flow<List<ReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity)

    @Delete
    suspend fun deleteReport(report: ReportEntity)
}
