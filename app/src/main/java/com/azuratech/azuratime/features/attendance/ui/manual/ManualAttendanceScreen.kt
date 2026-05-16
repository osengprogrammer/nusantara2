package com.azuratech.azuratime.features.attendance.ui.manual

import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle 
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.features.biometric.ui.enroll.StudentBiometricViewModel
import com.azuratech.azuratime.features.school.ui.classes.ClassViewModel
import com.azuratech.azuratime.features.account.ui.management.AccountManagementViewModel
import com.azuratech.azuratime.features.attendance.ui.capture.AttendanceViewModel
import com.azuratech.azuratime.core.util.AttendanceService
import com.azuratech.azuratime.core.data.local.StudentBiometricDetails
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Composable
fun ManualAttendanceScreen(
    biometricViewModel: StudentBiometricViewModel,
    attendanceViewModel: AttendanceViewModel,
    userViewModel: AccountManagementViewModel,
    classViewModel: ClassViewModel, 
    initialFaceId: String = "",
    initialDate: String = "",
    onNavigateBack: () -> Unit
) {
    val faces by biometricViewModel.studentRosterFlow.collectAsStateWithLifecycle()
    val currentUser by userViewModel.currentUser.collectAsStateWithLifecycle()
    val assignedIds by userViewModel.assignedClassIds.collectAsStateWithLifecycle()
    val globalClasses by classViewModel.classesStateFlow.collectAsStateWithLifecycle()

    // Role-Based Class Access
    val isAdmin = currentUser?.memberships?.get(currentUser?.activeSchoolId)?.role == "ADMIN"
    val availableClasses = remember(globalClasses, assignedIds, isAdmin) {
        if (isAdmin) globalClasses else globalClasses.filter { classItem: ClassModel -> classItem.id in assignedIds }
    }

    // 🔥 Make Class Selection Optional
    val classOptions = remember(availableClasses) {
        listOf(null) + availableClasses
    }

    // --- State Management ---
    var selectedStudent by remember(faces, initialFaceId) {
        mutableStateOf(faces.find { it.biometric.studentId == initialFaceId })
    }
    var selectedStatus by remember { mutableStateOf("H") }
    var selectedDate by remember {
        mutableStateOf(
            if (initialDate.isNotEmpty()) runCatching { LocalDate.parse(initialDate) }.getOrElse { LocalDate.now() }
            else LocalDate.now()
        )
    }
    var selectedTime by remember { mutableStateOf(LocalTime.now()) }
    var selectedClass by remember(availableClasses) { 
        mutableStateOf<ClassModel?>(null) 
    }

    val isLocked = initialFaceId.isNotEmpty()

    ManualAttendanceContent(
        selectedFace = selectedStudent,
        onFaceSelected = { selectedStudent = it },
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
        isLocked = isLocked,
        onSave = {
            selectedStudent?.let { biometricDetails ->
                val finalDateTime = LocalDateTime.of(selectedDate, selectedTime)
                val newRecord = AttendanceService.createRecord(
                    biometric = biometricDetails.biometric,
                    accountEmail = currentUser?.email ?: "admin@azuratech.com",
                    activeClassId = selectedClass?.id,
                    activeClassName = selectedClass?.name ?: "Umum / Tanpa Kelas",
                    status = selectedStatus,
                    attendanceTime = finalDateTime
                )
                attendanceViewModel.addRecord(newRecord)
                onNavigateBack()
            }
        },
        onBack = onNavigateBack
    )
}
