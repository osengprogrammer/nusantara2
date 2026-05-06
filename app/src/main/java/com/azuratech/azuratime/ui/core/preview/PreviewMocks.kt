package com.azuratech.azuratime.ui.core.preview

import androidx.compose.ui.graphics.Color
import com.azuratech.azuratime.data.local.CheckInRecordEntity
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.data.local.FaceEntity
import com.azuratech.azuratime.data.local.UserEntity
import com.azuratech.azuratime.ui.dashboard.DashboardUiState
import com.azuratech.azuratime.ui.report.AttendanceMatrixUiState
import com.azuratech.azuratime.ui.report.MatrixCellModel
import com.azuratech.azuratime.ui.report.MatrixRowModel
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Centralized Mock Data for Compose Previews.
 * Prevents repetitive boilerplate and ensures consistent test data 
 * across the entire application's preview suite.
 */
object PreviewMocks {
    
    val mockUser = UserEntity(
        userId = "usr_123",
        email = "admin@azuratech.com",
        name = "Azura Admin",
        activeSchoolId = "sch_1",
        activeClassId = "cls_1"
    )

    val mockClasses = listOf(
        ClassModel(id = "cls_1", schoolId = "sch_1", name = "Kelas 10A", grade = "10", teacherId = null, studentCount = 0, createdAt = 0L),
        ClassModel(id = "cls_2", schoolId = "sch_1", name = "Kelas 10B", grade = "10", teacherId = null, studentCount = 0, createdAt = 0L),
        ClassModel(id = "cls_3", schoolId = "sch_1", name = "Garmen Shift Pagi", grade = "N/A", teacherId = null, studentCount = 0, createdAt = 0L)
    )

    val mockStudents = listOf(
        FaceEntity(faceId = "face_1", name = "Budi Santoso", embedding = null),
        FaceEntity(faceId = "face_2", name = "Siti Aminah", embedding = null),
        FaceEntity(faceId = "face_3", name = "Agus Setiawan", embedding = null)
    )

    val mockRecentRecords = listOf(
        CheckInRecordEntity(
            id = "1",
            faceId = "face_1",
            name = "Budi Santoso",
            classId = "cls_1",
            className = "Kelas 10A",
            status = "H",
            attendanceDate = LocalDate.now(),
            checkInTime = LocalDateTime.now(),
            userId = "admin@azuratech.com",
            schoolId = "sch_1",
            isSynced = true,
            timestamp = System.currentTimeMillis() - 300000
        ),
        CheckInRecordEntity(
            id = "2",
            faceId = "face_2",
            name = "Siti Aminah",
            classId = "cls_1",
            className = "Kelas 10A",
            status = "H",
            attendanceDate = LocalDate.now(),
            checkInTime = LocalDateTime.now(),
            userId = "admin@azuratech.com",
            schoolId = "sch_1",
            isSynced = true,
            timestamp = System.currentTimeMillis() - 720000
        )
    )

    val mockDashboardStateSuccess = DashboardUiState(
        user = mockUser,
        assignedClasses = mockClasses,
        allClasses = mockClasses,
        recentRecords = mockRecentRecords,
        sessionStudents = mockStudents,
        isSyncing = false,
        isReady = true,
        currentRole = "ADMIN",
        isApproved = true,
        totalFaces = 145,
        unassignedStudents = 3,
        brokenAssignments = 0,
        unsyncedRecords = 12
    )
    
    val mockDashboardStateLoading = DashboardUiState(user = null, isReady = false)

    val mockMatrixRows = listOf(
        MatrixRowModel(
            studentId = "face_1",
            studentName = "Budi Santoso",
            studentClass = "Kelas 10A",
            cells = listOf(
                MatrixCellModel("H", Color(0xFF2E7D32), Color(0xFFE8F5E9), true),
                MatrixCellModel("A", Color(0xFFC62828), Color(0xFFFFEBEE), true),
                MatrixCellModel("S", Color(0xFFF9A825), Color(0xFFFFF9C4), false)
            ),
            totalHours = "16j 0m",
            summaryH = "1",
            summaryS = "1",
            summaryI = "0",
            summaryA = "1",
            estimatedSalary = "Rp 0"
        ),
        MatrixRowModel(
            studentId = "face_2",
            studentName = "Siti Aminah",
            studentClass = "Kelas 10A",
            cells = listOf(
                MatrixCellModel("H", Color(0xFF2E7D32), Color(0xFFE8F5E9), true),
                MatrixCellModel("H", Color(0xFF2E7D32), Color(0xFFE8F5E9), true),
                MatrixCellModel("H", Color(0xFF2E7D32), Color(0xFFE8F5E9), true)
            ),
            totalHours = "24j 0m",
            summaryH = "3",
            summaryS = "0",
            summaryI = "0",
            summaryA = "0",
            estimatedSalary = "Rp 0"
        )
    )

    val mockMatrixData = com.azuratech.azuratime.ui.report.AttendanceMatrixData(
        rows = mockMatrixRows,
        availableClasses = mockClasses,
        dateRange = listOf(LocalDate.now().minusDays(2), LocalDate.now().minusDays(1), LocalDate.now()),
        searchQuery = "",
        startDate = LocalDate.now().minusDays(2),
        endDate = LocalDate.now(),
        selectedClassId = "cls_1",
        policy = "SCHOOL"
    )

    val mockMatrixStateSuccess = com.azuratech.azuratime.ui.report.AttendanceMatrixUiState.Success(
        mockMatrixData
    )
    
    val mockMatrixStateLoading = com.azuratech.azuratime.ui.report.AttendanceMatrixUiState.Loading

    // For FaceList
    val mockStudentDisplayItems = listOf(
        com.azuratech.azuratime.ui.add.StudentDisplayItem(
            faceWithDetails = com.azuratech.azuratime.data.local.FaceWithDetails(face = mockStudents[0], className = "Kelas 10A", classId = "cls_1"),
            assignedClassNames = "Kelas 10A",
            isBiometricReady = true,
            assignedClassIds = listOf("cls_1")
        ),
        com.azuratech.azuratime.ui.add.StudentDisplayItem(
            faceWithDetails = com.azuratech.azuratime.data.local.FaceWithDetails(face = mockStudents[1], className = "Kelas 10B", classId = "cls_2"),
            assignedClassNames = "Kelas 10B",
            isBiometricReady = false,
            assignedClassIds = listOf("cls_2")
        )
    )

    val mockFaceListData = com.azuratech.azuratime.ui.add.FaceListData(
        searchQuery = "",
        selectedClassName = null,
        students = mockStudentDisplayItems,
        allClasses = mockClasses,
        studentForClassAssignment = null,
        studentForQuickEdit = null
    )

    val mockFaceListStateSuccess = com.azuratech.azuratime.ui.add.FaceListUiState.Success(
        mockFaceListData
    )

    val mockFaceListStateLoading = com.azuratech.azuratime.ui.add.FaceListUiState.Loading
}
