package com.azuratech.azuratime.core.ui.preview

import androidx.compose.ui.graphics.Color
import com.azuratech.azuratime.features.attendance.data.local.AttendanceRecordEntity
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.features.biometric.data.local.StudentBiometricEntity
import com.azuratech.azuratime.features.account.data.local.AccountEntity
import com.azuratech.azuratime.features.account.data.local.toDomain
import com.azuratech.azuratime.features.dashboard.ui.DashboardUiState
import com.azuratech.azuratime.features.reporting.ui.matrix.AttendanceMatrixUiState
import com.azuratech.azuratime.features.reporting.ui.matrix.MatrixCellModel
import com.azuratech.azuratime.features.reporting.ui.matrix.MatrixRowModel
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Centralized Mock Data for Compose Previews.
 * Prevents repetitive boilerplate and ensures consistent test data
 * across the entire application's preview suite.
 */
object PreviewMocks {

    val mockAccount = AccountEntity(
        accountId = "usr_123",
        email = "admin@azuratech.com",
        name = "Azura Admin",
        activeSchoolId = "sch_1",
        activeClassId = "cls_1",
    )

    val mockClasses = listOf(
        ClassModel(id = "cls_1", schoolId = "sch_1", name = "Kelas 10A", grade = "10", accountId = null, studentCount = 0, createdAt = 0L),
        ClassModel(id = "cls_2", schoolId = "sch_1", name = "Kelas 10B", grade = "10", accountId = null, studentCount = 0, createdAt = 0L),
        ClassModel(id = "cls_3", schoolId = "sch_1", name = "Garmen Shift Pagi", grade = "N/A", accountId = null, studentCount = 0, createdAt = 0L),
    )

    val mockStudents = listOf(
        StudentBiometricEntity(studentId = "face_1", name = "Budi Santoso", embedding = null),
        StudentBiometricEntity(studentId = "face_2", name = "Siti Aminah", embedding = null),
        StudentBiometricEntity(studentId = "face_3", name = "Agus Setiawan", embedding = null),
    )

    val mockRecentRecords = listOf(
        AttendanceRecordEntity(
            id = "1",
            studentId = "face_1",
            name = "Budi Santoso",
            classId = "cls_1",
            className = "Kelas 10A",
            status = "H",
            attendanceDate = LocalDate.now(),
            attendanceTime = LocalDateTime.now(),
            accountEmail = "admin@azuratech.com",
            schoolId = "sch_1",
            isSynced = true,
            timestamp = System.currentTimeMillis() - 300000,
        ),
        AttendanceRecordEntity(
            id = "2",
            studentId = "face_2",
            name = "Siti Aminah",
            classId = "cls_1",
            className = "Kelas 10A",
            status = "H",
            attendanceDate = LocalDate.now(),
            attendanceTime = LocalDateTime.now(),
            accountEmail = "admin@azuratech.com",
            schoolId = "sch_1",
            isSynced = true,
            timestamp = System.currentTimeMillis() - 720000,
        ),
    )

    val mockDashboardStateSuccess = DashboardUiState(
        account = mockAccount.toDomain(),
        assignedClasses = mockClasses,
        allClasses = mockClasses,
        recentRecords = mockRecentRecords,
        sessionStudents = mockStudents,
        isSyncing = false,
        isReady = true,
        currentRole = "ADMIN",
        isApproved = true,
        totalStudents = 145,
        unassignedStudents = 3,
        brokenAssignments = 0,
        unsyncedRecords = 12,
    )

    val mockDashboardStateLoading = DashboardUiState(account = null, isReady = false)

    val mockMatrixRows = listOf(
        MatrixRowModel(
            studentId = "face_1",
            studentName = "Budi Santoso",
            studentClass = "Kelas 10A",
            cells = listOf(
                MatrixCellModel("H", Color(0xFF2E7D32), Color(0xFFE8F5E9), true),
                MatrixCellModel("A", Color(0xFFC62828), Color(0xFFFFEBEE), true),
                MatrixCellModel("S", Color(0xFFF9A825), Color(0xFFFFF9C4), false),
            ),
            totalHours = "16j 0m",
            summaryH = "1",
            summaryT = "0",
            summaryS = "1",
            summaryI = "0",
            summaryA = "1",
            estimatedSalary = "Rp 0",
        ),
        MatrixRowModel(
            studentId = "face_2",
            studentName = "Siti Aminah",
            studentClass = "Kelas 10A",
            cells = listOf(
                MatrixCellModel("H", Color(0xFF2E7D32), Color(0xFFE8F5E9), true),
                MatrixCellModel("H", Color(0xFF2E7D32), Color(0xFFE8F5E9), true),
                MatrixCellModel("H", Color(0xFF2E7D32), Color(0xFFE8F5E9), true),
            ),
            totalHours = "24j 0m",
            summaryH = "3",
            summaryT = "0",
            summaryS = "0",
            summaryI = "0",
            summaryA = "0",
            estimatedSalary = "Rp 0",
        ),
    )

    val mockMatrixData = com.azuratech.azuratime.features.reporting.ui.matrix.AttendanceMatrixData(
        rows = mockMatrixRows,
        availableClasses = mockClasses,
        dateRange = listOf(LocalDate.now().minusDays(2), LocalDate.now().minusDays(1), LocalDate.now()),
        searchQuery = "",
        startDate = LocalDate.now().minusDays(2),
        endDate = LocalDate.now(),
        selectedClassId = "cls_1",
        policy = "SCHOOL",
    )

    val mockMatrixStateSuccess = com.azuratech.azuratime.features.reporting.ui.matrix.AttendanceMatrixUiState.Success(
        mockMatrixData,
    )

    val mockMatrixStateLoading = com.azuratech.azuratime.features.reporting.ui.matrix.AttendanceMatrixUiState.Loading

    // === Student Roster Mocks (delegates to feature-local mocks) ===
    fun studentRosterLoading() =
        com.azuratech.azuratime.features.student.ui.roster.StudentRosterPreviewMocks.loading()

    fun studentRosterPopulated() =
        com.azuratech.azuratime.features.student.ui.roster.StudentRosterPreviewMocks.populated()

    fun studentRosterError() =
        com.azuratech.azuratime.features.student.ui.roster.StudentRosterPreviewMocks.error()
}
