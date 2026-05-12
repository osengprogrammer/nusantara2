package com.azuratech.azuratime.features.attendance.domain.repository

import com.azuratech.azuratime.features.attendance.data.local.CheckInRecordEntity
import com.azuratech.azuratime.features.attendance.domain.model.CheckInRecord
import com.azuratech.azuraengine.result.Result
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class ProcessCheckInParams(
    val faceId: String,
    val studentName: String,
    val teacherEmail: String,
    val activeClassId: String?,
    val studentClassIds: List<String>
)

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
    ): Flow<List<CheckInRecordEntity>>

    suspend fun saveRecord(record: CheckInRecord): Result<Unit>
    suspend fun updateRecord(recordId: String, classId: String, className: String): Result<Unit>
    suspend fun deleteRecord(recordId: String, schoolId: String): Result<Unit>
    
    // 🔥 SYNC & MISC
    suspend fun syncRecord(record: CheckInRecord): Result<Unit>
    fun getTodayPresentCount(date: LocalDate, schoolId: String): Flow<Int>
    fun getUnassignedStudentCount(schoolId: String): Flow<Int>
    fun getFacesByClass(classId: String, schoolId: String): Flow<List<com.azuratech.azuratime.features.biometric.data.local.BiometricFaceEntity>>
    fun getStudentCountInClass(classId: String, schoolId: String): Flow<Int>
    fun getClassIdsForFace(faceId: String, schoolId: String): Flow<List<String>>
    suspend fun getFaceById(faceId: String, schoolId: String): com.azuratech.azuratime.features.biometric.data.local.BiometricFaceEntity?
    suspend fun getUnsyncedRecords(schoolId: String): List<CheckInRecord>
    suspend fun getRecordUpdates(schoolId: String, lastSync: Long): Result<List<CheckInRecord>>
    suspend fun syncRecords(): Result<Unit>
    suspend fun resolveConflict(conflictId: String, useCloud: Boolean): Result<Unit>
    suspend fun processCheckIn(params: ProcessCheckInParams): Result<com.azuratech.azuratime.features.attendance.domain.model.CheckInResult>
}
