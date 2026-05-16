package com.azuratech.azuratime.features.attendance.domain.repository

import com.azuratech.azuratime.features.attendance.data.local.AttendanceRecordEntity
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceRecord
import com.azuratech.azuraengine.result.Result
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class ProcessAttendanceParams(
    val studentId: String,
    val studentName: String,
    val teacherEmail: String,
    val activeClassId: String?,
    val studentClassIds: List<String>
)

/**
 * Repository interface for Check-In operations.
 * Following DIP, the interface lives in the domain layer.
 */
interface AttendanceRepository {
    fun getAttendanceRecords(
        name: String,
        startDate: LocalDate?,
        endDate: LocalDate?,
        userId: String?,
        classId: String?,
        assignedIds: List<String>,
        schoolId: String
    ): Flow<List<AttendanceRecordEntity>>

    suspend fun saveRecord(record: AttendanceRecord): Result<Unit>
    suspend fun updateRecord(recordId: String, classId: String, className: String): Result<Unit>
    suspend fun updateRecordStatus(recordId: String, status: com.azuratech.azuratime.features.attendance.domain.model.AttendanceStatus, schoolId: String): Result<Unit>
    suspend fun deleteRecord(recordId: String, schoolId: String): Result<Unit>
    
    //  SYNC & MISC
    suspend fun syncRecord(record: AttendanceRecord): Result<Unit>
    fun getTodayPresentCount(date: LocalDate, schoolId: String): Flow<Int>
    fun getUnassignedStudentCount(schoolId: String): Flow<Int>
    fun getFacesByClass(classId: String, schoolId: String): Flow<List<com.azuratech.azuratime.features.biometric.data.local.BiometricFaceEntity>>
    fun getStudentCountInClass(classId: String, schoolId: String): Flow<Int>
    fun getClassIdsForFace(faceId: String, schoolId: String): Flow<List<String>>
    suspend fun getFaceById(faceId: String, schoolId: String): com.azuratech.azuratime.features.biometric.data.local.BiometricFaceEntity?
    suspend fun getUnsyncedRecords(schoolId: String): List<AttendanceRecord>
    suspend fun getRecordUpdates(schoolId: String, lastSync: Long): Result<List<AttendanceRecord>>
    suspend fun syncRecords(): Result<Unit>
    suspend fun resolveConflict(conflictId: String, useCloud: Boolean): Result<Unit>
    suspend fun processAttendance(params: ProcessAttendanceParams): Result<com.azuratech.azuratime.features.attendance.domain.model.AttendanceResult>
}
