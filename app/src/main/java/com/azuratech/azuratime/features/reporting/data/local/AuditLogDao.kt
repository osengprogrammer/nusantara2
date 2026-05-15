package com.azuratech.azuratime.features.reporting.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_log_table WHERE schoolId = :schoolId ORDER BY timestamp DESC")
    fun observeLogsBySchool(schoolId: String): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogEntity)

    @Query("DELETE FROM audit_log_table WHERE schoolId = :schoolId AND timestamp < :cutoff")
    suspend fun purgeOldLogs(schoolId: String, cutoff: Long)
}
