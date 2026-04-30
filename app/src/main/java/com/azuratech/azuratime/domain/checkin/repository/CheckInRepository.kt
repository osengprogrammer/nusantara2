package com.azuratech.azuratime.domain.checkin.repository

import com.azuratech.azuratime.domain.checkin.model.CheckInRecord
import com.azuratech.azuraengine.result.Result
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Repository interface for Check-In operations.
 * Following DIP, the interface lives in the domain layer.
 */
interface CheckInRepository {
    fun getCheckInRecords(
        name: String,
        startDate: LocalDate?,
        endDate: LocalDate?,
        userId: String?,
        classId: String?,
        assignedIds: List<String>,
        schoolId: String
    ): Flow<List<CheckInRecord>>

    suspend fun saveRecord(record: CheckInRecord): Result<Unit>
    suspend fun updateRecord(record: CheckInRecord): Result<Unit>
    suspend fun syncRecord(record: CheckInRecord): Result<Unit>
    suspend fun deleteRecord(recordId: String, schoolId: String): Result<Unit>
    fun getTodayPresentCount(date: LocalDate, schoolId: String): Flow<Int>
    fun getUnassignedStudentCount(schoolId: String): Flow<Int>
    fun getFacesByClass(classId: String, schoolId: String): Flow<List<com.azuratech.azuratime.data.local.FaceEntity>>
    fun getStudentCountInClass(classId: String, schoolId: String): Flow<Int>
    fun getClassIdsForFace(faceId: String, schoolId: String): Flow<List<String>>
    suspend fun getFaceById(faceId: String, schoolId: String): com.azuratech.azuratime.data.local.FaceEntity?
    suspend fun getUnsyncedRecords(schoolId: String): List<CheckInRecord>
    suspend fun getRecordUpdates(schoolId: String, lastSync: Long): Result<List<CheckInRecord>>
    suspend fun resolveConflict(conflictId: String, useCloud: Boolean): Result<Unit>
}
