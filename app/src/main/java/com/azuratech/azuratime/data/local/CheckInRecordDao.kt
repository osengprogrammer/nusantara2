package com.azuratech.azuratime.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface CheckInRecordDao {
    @Query("SELECT * FROM check_in_records WHERE schoolId = :schoolId ORDER BY attendanceDate DESC, checkInTime DESC")
    fun getAllRecords(schoolId: String): Flow<List<CheckInRecordEntity>>

    @Query("SELECT * FROM check_in_records WHERE isSynced = 0 AND schoolId = :schoolId")
    suspend fun getUnsyncedRecords(schoolId: String): List<CheckInRecordEntity>

    @Query("SELECT * FROM check_in_records WHERE id = :recordId AND schoolId = :schoolId LIMIT 1")
    suspend fun getRecordById(recordId: String, schoolId: String): CheckInRecordEntity?

    @Query("""
        SELECT * FROM check_in_records 
        WHERE schoolId = :schoolId
        AND (:nameFilter = '' OR name LIKE '%' || :nameFilter || '%')
        AND (:startDate IS NULL OR attendanceDate >= :startDate)
        AND (:endDate IS NULL OR attendanceDate <= :endDate)
        AND (:userId IS NULL OR userId = :userId)
        AND (
            (:classId IS NOT NULL AND classId = :classId) 
            OR 
            (:classId IS NULL AND classId IN (:assignedIds))
            OR
            (:classId IS NULL)
        )
        ORDER BY attendanceDate DESC, checkInTime DESC
    """)
    fun getFilteredRecords(
        nameFilter: String = "", startDate: LocalDate? = null, endDate: LocalDate? = null,
        userId: String? = null, classId: String? = null, assignedIds: List<String> = emptyList(), schoolId: String
    ): Flow<List<CheckInRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: CheckInRecordEntity)

    @Update
    suspend fun update(record: CheckInRecordEntity)

    @Delete
    suspend fun delete(record: CheckInRecordEntity)
    
    @Query("SELECT COUNT(*) FROM check_in_records WHERE isSynced = 0 AND schoolId = :schoolId")
    fun getUnsyncedRecordsCountFlow(schoolId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM check_in_records WHERE attendanceDate = :date AND schoolId = :schoolId")
    fun getTodayPresentCount(date: LocalDate, schoolId: String): Flow<Int>

    @Query("DELETE FROM check_in_records WHERE schoolId = :schoolId")
    suspend fun deleteAllBySchool(schoolId: String)

    @Query("SELECT COUNT(*) FROM check_in_records WHERE schoolId = :schoolId")
    fun getTotalCountFlow(schoolId: String): Flow<Int>

    // 🔥 Added for ReportRepository
    @Query("SELECT * FROM check_in_records WHERE schoolId = :schoolId AND classId IN (:classIds) AND attendanceDate >= :start AND attendanceDate <= :end ORDER BY attendanceDate DESC, checkInTime DESC")
    fun getReportsByMultipleClasses(classIds: List<String>, start: LocalDate, end: LocalDate, schoolId: String): Flow<List<CheckInRecordEntity>>

    @Query("SELECT * FROM check_in_records WHERE schoolId = :schoolId AND attendanceDate >= :start AND attendanceDate <= :end ORDER BY attendanceDate DESC, checkInTime DESC")
    fun getReportsByDateRange(start: LocalDate, end: LocalDate, schoolId: String): Flow<List<CheckInRecordEntity>>
}
