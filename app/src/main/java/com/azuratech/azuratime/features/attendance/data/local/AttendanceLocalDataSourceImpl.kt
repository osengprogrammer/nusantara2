package com.azuratech.azuratime.features.attendance.data.local

import com.azuratech.azuratime.core.data.local.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceLocalDataSourceImpl @Inject constructor(
    private val database: AppDatabase,
) : AttendanceLocalDataSource {

    private val attendanceRecordDao = database.attendanceRecordDao()

    override fun getFilteredRecords(
        nameFilter: String,
        startDate: LocalDate?,
        endDate: LocalDate?,
        accountId: String?,
        classId: String?,
        assignedIds: List<String>,
        schoolId: String,
        subjectId: String?,
    ): Flow<List<AttendanceRecordEntity>> {
        return attendanceRecordDao.getFilteredRecords(
            schoolId = schoolId,
            nameFilter = nameFilter.ifBlank { null },
            accountId = accountId,
            classId = classId,
            subjectId = subjectId,
        ).map { records ->
            records.filter { record ->
                val recordDate = record.attendanceDate
                val afterStart = startDate == null || !recordDate.isBefore(startDate)
                val beforeEnd = endDate == null || !recordDate.isAfter(endDate)
                afterStart && beforeEnd
            }
        }
    }

    override suspend fun insert(record: AttendanceRecordEntity) = attendanceRecordDao.insert(record)
    override suspend fun update(record: AttendanceRecordEntity) = attendanceRecordDao.update(record)
    override suspend fun delete(record: AttendanceRecordEntity) = attendanceRecordDao.delete(record)

    override suspend fun getRecordById(recordId: String, schoolId: String): AttendanceRecordEntity? =
        attendanceRecordDao.getRecordById(recordId, schoolId)

    override suspend fun getRecordByStudentAndDate(studentId: String, date: LocalDate, schoolId: String): AttendanceRecordEntity? =
        attendanceRecordDao.getRecordByStudentAndDate(studentId, date, schoolId)

    override suspend fun getLatestRecordForStudent(studentId: String, classId: String, date: LocalDate, schoolId: String): AttendanceRecordEntity? =
        attendanceRecordDao.getLatestRecordForStudent(studentId, classId, date, schoolId)

    override suspend fun getUnsyncedRecords(schoolId: String): List<AttendanceRecordEntity> =
        attendanceRecordDao.getUnsyncedRecords(schoolId)
}
