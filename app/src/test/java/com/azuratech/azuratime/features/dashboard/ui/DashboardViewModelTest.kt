package com.azuratech.azuratime.features.dashboard.ui

import app.cash.turbine.test
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.data.local.AppDatabase
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.account.data.local.AccountEntity
import com.azuratech.azuratime.features.account.data.local.SchoolMembership
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import com.azuratech.azuratime.features.account.domain.repository.SchoolWorkspaceRepository
import com.azuratech.azuratime.features.attendance.domain.repository.AttendanceRepository
import com.azuratech.azuratime.features.attendance.data.local.AttendanceRecordEntity
import com.azuratech.azuratime.features.auth.domain.repository.AuthRepository
import com.azuratech.azuratime.features.biometric.domain.repository.BiometricRepository
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import com.azuratech.azuratime.features.student.domain.repository.StudentRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * 🚀 DASHBOARD VIEW MODEL TEST (Phase 23)
 * Verifies role-based class filtering logic.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var accountRepository: AccountRepository
    private lateinit var schoolRepository: SchoolRepository
    private lateinit var workspaceRepository: SchoolWorkspaceRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var attendanceRepository: AttendanceRepository
    private lateinit var biometricRepository: BiometricRepository
    private lateinit var studentRepository: StudentRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var database: AppDatabase

    private lateinit var viewModel: DashboardViewModel

    private val schoolId = "school_1"
    private val accountId = "user_1"

    private val allClasses = listOf(
        ClassModel("class_1", "school_1", "Kelas A", "10", "user_1", 10, emptyList(), 0L),
        ClassModel("class_2", "school_1", "Kelas B", "12", "user_1", 12, emptyList(), 0L),
        ClassModel("class_3", "school_1", "Kelas C", "8", "user_1", 8, emptyList(), 0L),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        accountRepository = mockk(relaxed = true)
        schoolRepository = mockk(relaxed = true)
        workspaceRepository = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        attendanceRepository = mockk(relaxed = true)
        biometricRepository = mockk(relaxed = true)
        studentRepository = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)
        database = mockk(relaxed = true)

        // Mock Session
        every { sessionManager.currentAccountIdFlow } returns MutableStateFlow(accountId).asStateFlow()
        every { sessionManager.activeSchoolIdFlow } returns MutableStateFlow(schoolId).asStateFlow()
        every { sessionManager.getActiveSchoolId() } returns schoolId

        // Mock Repositories
        every { schoolRepository.observeClassesFlow(schoolId) } returns flowOf(Result.Success(allClasses))
        every { schoolRepository.observeSchoolByIdFlow(schoolId) } returns flowOf(Result.Success(null))
        every {
            attendanceRepository.getAttendanceRecordsFlow(any(), any(), any(), any(), any(), any(), any())
        } returns flowOf(Result.Success<List<AttendanceRecordEntity>>(emptyList()))
        every { accountRepository.observePendingRequestsCountFlow(any()) } returns flowOf(0)
        every { studentRepository.getStudentProfilesFlow() } returns flowOf(Result.Success(emptyList()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `supervisor only sees assigned classes in ui state`() = runTest {
        // 1. GIVEN: Supervisor with only class_1 assigned
        val supervisor = AccountEntity(
            accountId = accountId,
            email = "teacher@school.com",
            name = "Teacher X",
            role = "USER", // membership role is what matters
            memberships = mapOf(
                schoolId to SchoolMembership(
                    schoolName = "My School",
                    role = "SUPERVISOR",
                    assignedClassIds = listOf("class_1"),
                ),
            ),
        )
        every { accountRepository.observeAccountEntityFlow(accountId) } returns flowOf(Result.Success(supervisor))

        viewModel = createViewModel()

        // 2. WHEN & THEN: Assert filtered classes
        viewModel.uiStateFlow.test {
            // Wait for state to be ready and populated
            var state = awaitItem()
            while (state.isLoading || state.account == null || state.assignedClasses.isEmpty()) {
                state = awaitItem()
            }

            assertEquals(1, state.assignedClasses.size)
            assertEquals("class_1", state.assignedClasses[0].id)

            // In Phase 23, we also filter allClasses for Supervisors in the VM
            assertEquals(1, state.allClasses.size)
            assertEquals("class_1", state.allClasses[0].id)
        }
    }

    @Test
    fun `admin sees all classes in ui state`() = runTest {
        // 1. GIVEN: Admin account
        val admin = AccountEntity(
            accountId = accountId,
            email = "admin@school.com",
            name = "Admin One",
            memberships = mapOf(
                schoolId to SchoolMembership(
                    schoolName = "My School",
                    role = "ADMIN",
                ),
            ),
        )
        every { accountRepository.observeAccountEntityFlow(accountId) } returns flowOf(Result.Success(admin))

        viewModel = createViewModel()

        // 2. WHEN & THEN: Assert all classes visible
        viewModel.uiStateFlow.test {
            // Wait for state to be ready and populated
            var state = awaitItem()
            while (state.isLoading || state.account == null || state.assignedClasses.isEmpty()) {
                state = awaitItem()
            }

            assertEquals(3, state.assignedClasses.size)
            assertEquals(3, state.allClasses.size)
        }
    }

    private fun createViewModel() = DashboardViewModel(
        accountRepository,
        schoolRepository,
        workspaceRepository,
        authRepository,
        attendanceRepository,
        biometricRepository,
        studentRepository,
        sessionManager,
        database,
    )
}
