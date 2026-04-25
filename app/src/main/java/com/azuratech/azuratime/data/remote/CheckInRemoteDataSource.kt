package com.azuratech.azuratime.data.remote

import com.azuratech.azuratime.data.local.CheckInRecordEntity
import com.azuratech.azuraengine.result.Result

interface CheckInRemoteDataSource {
    suspend fun getRecordUpdates(schoolId: String, lastSync: Long): Result<List<CheckInRecordEntity>>
    suspend fun syncRecord(record: CheckInRecordEntity): Result<Unit>
    suspend fun deleteRecord(schoolId: String, recordId: String): Result<Unit>
}
