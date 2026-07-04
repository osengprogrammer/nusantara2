package com.azuratech.azuratime.feature.audit.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AuditDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: AuditEntity)

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    suspend fun getRecentEvents(): List<AuditEntity>
}
