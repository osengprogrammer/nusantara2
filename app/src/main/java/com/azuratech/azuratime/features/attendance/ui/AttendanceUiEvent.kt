package com.azuratech.azuratime.features.attendance.ui

import com.azuratech.azuratime.features.attendance.domain.model.AttendanceRecord
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceStatus
import com.azuratech.azuraengine.model.ClassModel

/**
 * 📝 ATTENDANCE UI EVENT (v3.2.0-ai-native)
 */
sealed class AttendanceUiEvent {
    data object LoadAttendance : AttendanceUiEvent()
    data class SelectClass(val classId: String?) : AttendanceUiEvent()
    data class UpdateSearchQuery(val query: String) : AttendanceUiEvent()
    data object Refresh : AttendanceUiEvent()
    data object SyncHistory : AttendanceUiEvent()
    data object ClearError : AttendanceUiEvent()

    // Actions
    data class DeleteRecord(val record: AttendanceRecord) : AttendanceUiEvent()
    data class UpdateRecordStatus(val record: AttendanceRecord, val status: AttendanceStatus) : AttendanceUiEvent()
    data class UpdateRecordClass(val record: AttendanceRecord, val classModel: ClassModel) : AttendanceUiEvent()
    data class ExportRecords(val records: List<AttendanceRecord>) : AttendanceUiEvent()
}
