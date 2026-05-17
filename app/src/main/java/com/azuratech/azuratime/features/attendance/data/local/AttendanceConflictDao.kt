package com.azuratech.azuratime.features.attendance.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceConflictDao {
    @Query("SELECT * FROM attendance_conflicts WHERE conflictId = :conflictId")
    suspend fun getConflictById(conflictId: String): AttendanceConflictEntity?

    @Query("SELECT * FROM attendance_conflicts WHERE local_schoolId = :schoolId")
    fun observeConflictsBySchool(schoolId: String): Flow<List<AttendanceConflictEntity>>

    @Query("SELECT * FROM attendance_conflicts")
    fun getAllConflicts(): Flow<List<AttendanceConflictEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conflict: AttendanceConflictEntity)

    @Delete
    suspend fun delete(conflict: AttendanceConflictEntity)

    @Query("DELETE FROM attendance_conflicts WHERE conflictId = :conflictId")
    suspend fun deleteById(conflictId: String)
}
