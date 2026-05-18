package com.azuratech.azuratime.features.attendance.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface AttendanceRecordDao {
    @Query("SELECT * FROM check_in_records WHERE schoolId = :schoolId ORDER BY timestamp DESC")
    fun getAllRecords(schoolId: String): Flow<List<AttendanceRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: AttendanceRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<AttendanceRecordEntity>)

    @Update
    suspend fun update(record: AttendanceRecordEntity)

    @Delete
    suspend fun delete(record: AttendanceRecordEntity)

    @Query("SELECT * FROM check_in_records WHERE id = :id AND schoolId = :schoolId")
    suspend fun getRecordById(id: String, schoolId: String): AttendanceRecordEntity?

    @Query("SELECT * FROM check_in_records WHERE id = :id")
    suspend fun getRecordByIdNoSchool(id: String): AttendanceRecordEntity?

    @Query("SELECT * FROM check_in_records WHERE studentId = :studentId AND attendanceDate = :date AND schoolId = :schoolId")
    suspend fun getRecordByStudentAndDate(studentId: String, date: LocalDate, schoolId: String): AttendanceRecordEntity?

    @Query(
        """
        SELECT * FROM check_in_records 
        WHERE studentId = :studentId AND classId = :classId AND attendanceDate = :date AND schoolId = :schoolId 
        ORDER BY timestamp DESC LIMIT 1
    """,
    )
    suspend fun getLatestRecordForStudent(studentId: String, classId: String, date: LocalDate, schoolId: String): AttendanceRecordEntity?

    @Query("SELECT * FROM check_in_records WHERE studentId = :studentId AND schoolId = :schoolId ORDER BY timestamp DESC")
    suspend fun getStudentHistory(studentId: String, schoolId: String): List<AttendanceRecordEntity>

    @Query("SELECT * FROM check_in_records WHERE schoolId = :schoolId AND isSynced = 0")
    suspend fun getUnsyncedRecords(schoolId: String): List<AttendanceRecordEntity>

    @Query("SELECT COUNT(DISTINCT studentId) FROM check_in_records WHERE attendanceDate = :date AND schoolId = :schoolId AND status = 'H'")
    fun getTodayPresentCount(date: LocalDate, schoolId: String): Flow<Int>

    @Query("DELETE FROM check_in_records WHERE schoolId = :schoolId")
    suspend fun deleteAllBySchool(schoolId: String)

    @Query("SELECT COUNT(*) FROM check_in_records WHERE schoolId = :schoolId")
    fun getTotalCountFlow(schoolId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM check_in_records WHERE schoolId = :schoolId AND isSynced = 0")
    fun getUnsyncedRecordsCountFlow(schoolId: String): Flow<Int>

    @Query(
        """
        SELECT * FROM check_in_records 
        WHERE schoolId = :schoolId 
        AND (:nameFilter IS NULL OR name LIKE '%' || :nameFilter || '%')
        AND (:accountId IS NULL OR accountEmail = :accountId)
        AND (:classId IS NULL OR classId = :classId)
        ORDER BY timestamp DESC
    """,
    )
    fun getFilteredRecords(
        schoolId: String,
        nameFilter: String?,
        accountId: String?,
        classId: String?,
    ): Flow<List<AttendanceRecordEntity>>
}
