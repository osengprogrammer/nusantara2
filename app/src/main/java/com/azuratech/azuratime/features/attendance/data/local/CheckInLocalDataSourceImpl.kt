package com.azuratech.azuratime.features.attendance.data.local

import com.azuratech.azuratime.data.local.AppDatabase
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckInLocalDataSourceImpl @Inject constructor(
    private val database: AppDatabase
) : CheckInLocalDataSource {
    private val checkInRecordDao = database.checkInRecordDao()

    override fun getFilteredRecords(
        nameFilter: String,
        startDate: LocalDate?,
        endDate: LocalDate?,
        userId: String?,
        classId: String?,
        assignedIds: List<String>,
        schoolId: String
    ): Flow<List<CheckInRecordEntity>> = checkInRecordDao.getFilteredRecords(
        nameFilter = nameFilter,
        startDate = startDate,
        endDate = endDate,
        userId = userId,
        classId = classId,
        assignedIds = assignedIds,
        schoolId = schoolId
    )

    override suspend fun insert(record: CheckInRecordEntity) = checkInRecordDao.insert(record)

    override suspend fun update(record: CheckInRecordEntity) = checkInRecordDao.update(record)

    override suspend fun delete(record: CheckInRecordEntity) = checkInRecordDao.delete(record)

    override suspend fun getRecordById(recordId: String, schoolId: String): CheckInRecordEntity? =
        checkInRecordDao.getRecordById(recordId, schoolId)

    override suspend fun getRecordByFaceAndDate(faceId: String, date: LocalDate, schoolId: String): CheckInRecordEntity? =
        checkInRecordDao.getRecordByFaceAndDate(faceId, date, schoolId)

    override suspend fun getUnsyncedRecords(schoolId: String): List<CheckInRecordEntity> =
        checkInRecordDao.getUnsyncedRecords(schoolId)
}
