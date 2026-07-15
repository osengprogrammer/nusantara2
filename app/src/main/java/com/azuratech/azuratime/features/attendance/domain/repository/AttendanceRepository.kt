package com.azuratech.azuratime.features.attendance.domain.repository

import com.azuratech.azuratime.core.result.Result
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceRecord
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceStatus
import com.azuratech.azuratime.features.session.domain.model.SessionType
import com.azuratech.azuratime.core.data.local.StudentBiometricEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class ProcessAttendanceParams(
    val studentId: String,
    val studentName: String,
    val accountEmail: String,
    val activeClassId: String?,
    val studentClassIds: List<String>,
    val status: AttendanceStatus = AttendanceStatus.PRESENT,
    val timestamp: Long? = null,
    val sessionId: String? = null,
)

/**
 * 📝 ATTENDANCE REPOSITORY (v3.2.0-ai-native)
 * The single source of truth for Check-In operations.
 */
interface AttendanceRepository {
    fun getAttendanceRecordsFlow(
        name: String,
        startDate: LocalDate?,
        endDate: LocalDate?,
        accountId: String?,
        classId: String?,
        assignedIds: List<String>,
        schoolId: String,
    ): Flow<Result<List<AttendanceRecord>>>

    suspend fun saveRecord(record: AttendanceRecord, sessionId: String? = null): Result<Unit>
    suspend fun updateRecord(recordId: String, classId: String, className: String): Result<Unit>
    suspend fun updateRecordStatus(recordId: String, status: AttendanceStatus, schoolId: String): Result<Unit>
    suspend fun deleteRecord(recordId: String, schoolId: String): Result<Unit>
    suspend fun getStudentHistory(studentId: String): Result<List<AttendanceRecord>>

    //  SYNC & MISC
    suspend fun syncRecord(record: AttendanceRecord): Result<Unit>
    fun getTodayPresentCountFlow(date: LocalDate, schoolId: String): Flow<Result<Int>>
    fun getUnassignedStudentCountFlow(schoolId: String): Flow<Result<Int>>
    fun getStudentsByClassFlow(classId: String, schoolId: String): Flow<Result<List<StudentBiometricEntity>>>
    fun getStudentCountInClassFlow(classId: String, schoolId: String): Flow<Result<Int>>
    fun getClassIdsForStudentFlow(studentId: String, schoolId: String): Flow<Result<List<String>>>
    suspend fun getStudentBiometricById(studentId: String, schoolId: String): Result<StudentBiometricEntity>
    suspend fun getUnsyncedRecords(schoolId: String): Result<List<AttendanceRecord>>
    suspend fun getRecordUpdates(schoolId: String, lastSync: Long): Result<List<AttendanceRecord>>
    suspend fun syncRecords(): Result<Unit>
    suspend fun resolveConflict(conflictId: String, useCloud: Boolean): Result<Unit>
    suspend fun processAttendance(params: ProcessAttendanceParams): Result<com.azuratech.azuratime.features.attendance.domain.model.AttendanceResult>

    fun getAttendanceByTierFlow(schoolId: String, sessionType: SessionType): Flow<Result<List<AttendanceRecord>>>
    fun getTierSummaryCountFlow(schoolId: String, sessionType: SessionType, date: LocalDate): Flow<Result<Int>>

    /**
     * 🔥 THE EXPORTER: Export raw attendance logs to CSV.
     */
    suspend fun exportLogs(records: List<AttendanceRecord>): Result<String>
}
