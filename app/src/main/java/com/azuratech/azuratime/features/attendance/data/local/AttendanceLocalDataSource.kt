package com.azuratech.azuratime.features.attendance.data.local

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface AttendanceLocalDataSource {
    fun getFilteredRecords(
        nameFilter: String,
        startDate: LocalDate?,
        endDate: LocalDate?,
        userId: String?,
        classId: String?,
        assignedIds: List<String>,
        schoolId: String
    ): Flow<List<AttendanceRecordEntity>>

    suspend fun insert(record: AttendanceRecordEntity)
    suspend fun update(record: AttendanceRecordEntity)
    suspend fun delete(record: AttendanceRecordEntity)
    suspend fun getRecordById(recordId: String, schoolId: String): AttendanceRecordEntity?
    suspend fun getRecordByFaceAndDate(faceId: String, date: LocalDate, schoolId: String): AttendanceRecordEntity?
    suspend fun getLatestRecordForStudent(faceId: String, classId: String, date: LocalDate, schoolId: String): AttendanceRecordEntity?
    suspend fun getUnsyncedRecords(schoolId: String): List<AttendanceRecordEntity>
}
