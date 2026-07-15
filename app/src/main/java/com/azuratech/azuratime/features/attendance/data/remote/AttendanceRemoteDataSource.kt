package com.azuratech.azuratime.features.attendance.data.remote

import com.azuratech.azuratime.core.result.Result
import com.azuratech.azuratime.core.data.local.AttendanceRecordEntity

interface AttendanceRemoteDataSource {
    suspend fun getRecordUpdates(schoolId: String, lastSync: Long): Result<List<AttendanceRecordEntity>>
    suspend fun syncRecord(record: AttendanceRecordEntity): Result<Unit>
    suspend fun deleteRecord(schoolId: String, recordId: String): Result<Unit>
}
