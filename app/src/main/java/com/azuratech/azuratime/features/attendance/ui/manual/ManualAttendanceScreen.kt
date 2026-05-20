package com.azuratech.azuratime.features.attendance.ui.manual

import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.features.biometric.ui.enroll.BiometricEnrollmentViewModel
import com.azuratech.azuratime.features.school.ui.classes.ClassViewModel
import com.azuratech.azuratime.features.account.ui.management.AccountManagementViewModel
import com.azuratech.azuratime.features.attendance.ui.AttendanceViewModel
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import com.azuratech.azuratime.core.domain.model.AccountRole
import com.azuratech.azuratime.core.domain.model.toAccountRole

@Composable
fun ManualAttendanceScreen(
    biometricViewModel: BiometricEnrollmentViewModel,
    attendanceViewModel: AttendanceViewModel,
    accountViewModel: AccountManagementViewModel,
    classViewModel: ClassViewModel,
    initialFaceId: String = "",
    initialDate: String = "",
    onNavigateBack: () -> Unit,
) {
    val faces by biometricViewModel.studentRosterFlow.collectAsStateWithLifecycle()
    val currentAccount by accountViewModel.currentAccountFlow.collectAsStateWithLifecycle()
    val assignedIds by accountViewModel.assignedClassIdsFlow.collectAsStateWithLifecycle()
    val classUiState by classViewModel.uiStateFlow.collectAsStateWithLifecycle()
    val globalClasses = classUiState.classes

    // Role-Based Class Access
    val isAdmin = currentAccount?.memberships?.get(currentAccount?.activeSchoolId)?.role.toAccountRole() == AccountRole.ADMIN
    val availableClasses = remember(globalClasses, assignedIds, isAdmin) {
        if (isAdmin) globalClasses else globalClasses.filter { classItem: ClassModel -> classItem.id in assignedIds }
    }

    val classOptions = remember(availableClasses) {
        listOf(null) + availableClasses
    }

    var selectedFace by remember(faces) {
        mutableStateOf(faces.find { it.biometric.studentId == initialFaceId })
    }
    var selectedStatus by remember { mutableStateOf("H") }
    var selectedDate by remember {
        mutableStateOf(if (initialDate.isNotEmpty()) LocalDate.parse(initialDate) else LocalDate.now())
    }
    var selectedTime by remember { mutableStateOf(LocalTime.now()) }
    var selectedClass by remember(availableClasses) {
        mutableStateOf(availableClasses.find { it.id == currentAccount?.activeClassId })
    }

    ManualAttendanceContent(
        selectedFace = selectedFace,
        onFaceSelected = { selectedFace = it },
        faces = faces,
        selectedStatus = selectedStatus,
        onStatusSelected = { selectedStatus = it },
        selectedDate = selectedDate,
        onDateSelected = { selectedDate = it },
        selectedTime = selectedTime,
        onTimeSelected = { selectedTime = it },
        selectedClass = selectedClass,
        onClassSelected = { selectedClass = it },
        availableClasses = classOptions,
        isLocked = false,
        onSave = {
            val face = selectedFace ?: return@ManualAttendanceContent
            val finalDateTime = java.time.LocalDateTime.of(selectedDate, selectedTime)
            val epochMillis = finalDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

            if (selectedClass != null) {
                // Process through capture engine
                attendanceViewModel.processManualAttendance(
                    scannedStudentId = face.biometric.studentId,
                    studentName = face.biometric.name,
                    studentClasses = face.classIds,
                    status = AttendanceStatus.fromCode(selectedStatus),
                    timestamp = epochMillis,
                ) { _, _ ->
                    onNavigateBack()
                }
            } else {
                // Direct Save
                val newRecord = com.azuratech.azuratime.features.attendance.domain.model.AttendanceRecord(
                    recordId = "man_${System.currentTimeMillis()}",
                    studentId = face.biometric.studentId,
                    studentName = face.biometric.name,
                    classId = face.classIds.firstOrNull() ?: "UNASSIGNED",
                    className = "Manual",
                    schoolId = currentAccount?.activeSchoolId ?: "",
                    accountEmail = currentAccount?.email ?: "admin@azuratech.com",
                    status = AttendanceStatus.fromCode(selectedStatus),
                    timestamp = epochMillis,
                )
                attendanceViewModel.addRecord(newRecord)
                onNavigateBack()
            }
        },
        onBack = onNavigateBack,
    )
}
