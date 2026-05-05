package com.azuratech.azuratime.data.local

import androidx.room.*
import com.azuratech.azuratime.domain.checkin.model.AttendanceConflict

/**
 * Persistence entity for Attendance Conflicts.
 * Stores both local and cloud versions of a check-in record for resolution.
 */
@Entity(tableName = "attendance_conflicts")
data class AttendanceConflictEntity(
    @PrimaryKey val conflictId: String,
    @Embedded(prefix = "local_") val local: CheckInRecordEntity,
    @Embedded(prefix = "cloud_") val cloud: CheckInRecordEntity
) {
    fun toDomain(): AttendanceConflict {
        return AttendanceConflict(
            conflictId = conflictId,
            local = local.toDomain(),
            cloud = cloud.toDomain()
        )
    }
}

@Dao
interface AttendanceConflictDao {
    @Query("SELECT * FROM attendance_conflicts WHERE conflictId = :conflictId")
    suspend fun getConflictById(conflictId: String): AttendanceConflictEntity?

    @Query("SELECT * FROM attendance_conflicts")
    fun getAllConflicts(): kotlinx.coroutines.flow.Flow<List<AttendanceConflictEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conflict: AttendanceConflictEntity)

    @Delete
    suspend fun delete(conflict: AttendanceConflictEntity)
    
    @Query("DELETE FROM attendance_conflicts WHERE conflictId = :conflictId")
    suspend fun deleteById(conflictId: String)
}
