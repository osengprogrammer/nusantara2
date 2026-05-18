package com.azuratech.azuratime.features.reporting.domain.repository

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.biometric.data.local.StudentBiometricEntity
import com.azuratech.azuratime.features.attendance.data.local.AttendanceConflictEntity
import kotlinx.coroutines.flow.Flow

interface DataIntegrityRepository {
    val totalStudentsFlow: Flow<Result<Int>>
    val totalRecordsFlow: Flow<Result<Int>>
    val missingAssignmentFlow: Flow<Result<Int>>
    val brokenAssignmentsFlow: Flow<Result<Int>>
    val globalUnsyncedCountFlow: Flow<Result<Int>>
    val conflictsFlow: Flow<Result<List<AttendanceConflictEntity>>>
    fun getIncompleteProfiles(type: String): Flow<Result<List<StudentBiometricEntity>>>
}
