package com.azuratech.azuratime.features.attendance.data.local

import com.azuratech.azuratime.core.data.local.AppDatabase
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceLocalDataSourceImpl @Inject constructor(
    private val database: AppDatabase
) : AttendanceLocalDataSource {

    private val attendanceRecordDao = database.attendanceRecordDao()

    override fun getFilteredRecords(
        nameFilter: String,
        startDate: LocalDate?,
        endDate: LocalDate?,
        userId: String?,
        classId: String?,
        assignedIds: List<String>,
        schoolId: String
    ): Flow<List<AttendanceRecordEntity>> {
        // Simplified version for now
        return attendanceRecordDao.getFilteredRecords(schoolId, nameFilter.ifBlank { null }, classId)
    }

    override suspend fun insert(record: AttendanceRecordEntity) = attendanceRecordDao.insert(record)
    override suspend fun update(record: AttendanceRecordEntity) = attendanceRecordDao.update(record)
    override suspend fun delete(record: AttendanceRecordEntity) = attendanceRecordDao.delete(record)

    override suspend fun getRecordById(recordId: String, schoolId: String): AttendanceRecordEntity? =
        attendanceRecordDao.getRecordById(recordId, schoolId)

    override suspend fun getRecordByFaceAndDate(faceId: String, date: LocalDate, schoolId: String): AttendanceRecordEntity? =
        attendanceRecordDao.getRecordByFaceAndDate(faceId, date, schoolId)

    override suspend fun getLatestRecordForStudent(faceId: String, classId: String, date: LocalDate, schoolId: String): AttendanceRecordEntity? =
        attendanceRecordDao.getLatestRecordForStudent(faceId, classId, date, schoolId)

    override suspend fun getUnsyncedRecords(schoolId: String): List<AttendanceRecordEntity> =
        attendanceRecordDao.getUnsyncedRecords(schoolId)
}
