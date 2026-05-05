package com.azuratech.azuratime.ui.checkin

import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle 
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.ui.add.FaceViewModel
import com.azuratech.azuratime.ui.classes.ClassViewModel
import com.azuratech.azuratime.ui.user.UserManagementViewModel
import com.azuratech.azuratime.core.util.AttendanceService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Composable
fun ManualAttendanceScreen(
    faceViewModel: FaceViewModel,
    checkInViewModel: CheckInViewModel,
    userViewModel: UserManagementViewModel,
    classViewModel: ClassViewModel, 
    initialFaceId: String = "",
    initialDate: String = "",
    onBack: () -> Unit
) {
    val faces by faceViewModel.faceList.collectAsStateWithLifecycle(emptyList())
    val currentUser by userViewModel.currentUser.collectAsStateWithLifecycle()
    val assignedIds by userViewModel.assignedClassIds.collectAsStateWithLifecycle(emptyList())
    val globalClasses by classViewModel.classes.collectAsStateWithLifecycle(emptyList())

    // Role-Based Class Access
    val isAdmin = currentUser?.memberships?.get(currentUser?.activeSchoolId)?.role == "ADMIN"
    val availableClasses = remember(globalClasses, assignedIds, isAdmin) {
        if (isAdmin) globalClasses else globalClasses.filter { it.id in assignedIds }
    }

    // 🔥 Make Class Selection Optional
    val classOptions = remember(availableClasses) {
        listOf(null) + availableClasses
    }

    // --- State Management ---
    var selectedFace by remember(faces, initialFaceId) {
        mutableStateOf(faces.find { it.face.faceId == initialFaceId })
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
        isLocked = isLocked,
        onSave = {
            selectedFace?.let { faceWithDetails ->
                val finalDateTime = LocalDateTime.of(selectedDate, selectedTime)
                val newRecord = AttendanceService.createRecord(
                    face = faceWithDetails.face,
                    teacherEmail = currentUser?.email ?: "admin@azuratech.com",
                    activeClassId = selectedClass?.id,
                    activeClassName = selectedClass?.name ?: "Umum / Tanpa Kelas",
                    status = selectedStatus,
                    attendanceDate = selectedDate,
                    checkInTime = finalDateTime
                )
                checkInViewModel.addRecord(newRecord)
                onBack()
            }
        },
        onBack = onBack
    )
}
