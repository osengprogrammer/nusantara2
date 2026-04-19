package com.azuratech.azuratime.data.local

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface CheckInLocalDataSource {
    fun getFilteredRecords(
        nameFilter: String,
        startDate: LocalDate?,
        endDate: LocalDate?,
        userId: String?,
        classId: String?,
        assignedIds: List<String>,
        schoolId: String
    ): Flow<List<CheckInRecordEntity>>

    suspend fun insert(record: CheckInRecordEntity)
    suspend fun update(record: CheckInRecordEntity)
    suspend fun delete(record: CheckInRecordEntity)
    suspend fun getRecordById(recordId: String, schoolId: String): CheckInRecordEntity?
}
