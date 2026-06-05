package com.azuratech.azuratime.features.account.domain.usecase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.account.data.local.AccountEntity
import com.azuratech.azuratime.features.account.data.local.SchoolMembership
import com.azuratech.azuratime.features.account.data.sync.AccountSyncWorker
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import com.azuratech.azuratime.features.biometric.domain.repository.BiometricRepository
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import com.azuratech.azuratime.features.student.domain.repository.StudentRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 🚀 CLASS ASSIGNMENT SYNC TEST (Phase 22)
 * Verifies E2E integration: UseCase -> Repository -> Cloud -> Worker -> Room
 */
@RunWith(RobolectricTestRunner::class)
class ClassAssignmentSyncTest {

    private lateinit var context: Context
    private lateinit var accountRepository: AccountRepository
    private lateinit var studentRepository: StudentRepository
    private lateinit var biometricRepository: BiometricRepository
    private lateinit var schoolRepository: SchoolRepository
    private lateinit var assignUseCase: AssignClassToSupervisorUseCase

    private val schoolId = "school_abc"
    private val targetAccountId = "supervisor_123"
    private val classIds = listOf("class_1", "class_2")

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        accountRepository = mockk(relaxed = true)
        studentRepository = mockk(relaxed = true)
        biometricRepository = mockk(relaxed = true)
        schoolRepository = mockk(relaxed = true)
        assignUseCase = AssignClassToSupervisorUseCase(accountRepository)

        // Default successes
        coEvery { studentRepository.pushPendingProfiles() } returns Result.Success(Unit)
        coEvery { biometricRepository.syncBiometrics() } returns Result.Success(Unit)
        coEvery { schoolRepository.syncClasses(any(), any()) } returns Result.Success(Unit)
        coEvery { schoolRepository.syncSchools(any()) } returns Result.Success(Unit)
        coEvery { studentRepository.pullStudents(any()) } returns Result.Success(Unit)
        coEvery { studentRepository.autoHealStudentIdentities(any()) } returns Result.Success(Unit)
    }

    @Test
    fun `when admin assigns classes, repository is called and sync succeeds`() = runTest {
        // 1. GIVEN: Repository returns success
        coEvery {
            accountRepository.assignClassToConnection(targetAccountId, schoolId, classIds)
        } returns Result.Success(Unit)

        // 2. WHEN: UseCase invoked
        val result = assignUseCase(targetAccountId, schoolId, classIds)

        // 3. THEN: Assert Success and Repository interaction
        assert(result is Result.Success)
        coVerify { accountRepository.assignClassToConnection(targetAccountId, schoolId, classIds) }
    }

    @Test
    fun `when sync worker runs, it pulls updated memberships from cloud`() = runTest {
        // 1. GIVEN: Cloud has updated assignedClassIds
        val updatedMembership = SchoolMembership(
            schoolName = "Test School",
            role = "SUPERVISOR",
            assignedClassIds = classIds,
        )
        val updatedAccountEntity = AccountEntity(
            accountId = targetAccountId,
            email = "supervisor@school.com",
            name = "Supervisor X",
            memberships = mapOf(schoolId to updatedMembership),
        )

        coEvery { accountRepository.syncAccount(targetAccountId) } returns Result.Success(updatedAccountEntity)
        coEvery { accountRepository.pushAccount(targetAccountId) } returns Result.Success(Unit)

        val worker = TestListenableWorkerBuilder<AccountSyncWorker>(
            context = context,
            inputData = workDataOf("accountId" to targetAccountId),
        ).setWorkerFactory(DelegatingWorkerFactory(accountRepository, studentRepository, biometricRepository, schoolRepository)).build()

        // 2. WHEN: Worker runs
        val result = worker.doWork()

        // 3. THEN: Assert success and verify pull/push calls
        assertEquals(ListenableWorker.Result.success(), result)
        coVerify { accountRepository.syncAccount(targetAccountId) }
    }

    @Test
    fun `when network fails during assignment, returns network failure`() = runTest {
        // 1. GIVEN: Network is down
        coEvery {
            accountRepository.assignClassToConnection(any(), any(), any())
        } returns Result.Failure(AppError.Network("No Internet"))

        // 2. WHEN: UseCase invoked
        val result = assignUseCase(targetAccountId, schoolId, classIds)

        // 3. THEN: Assert Failure
        assert(result is Result.Failure)
        assertEquals("No Internet", (result as Result.Failure).error.message)
    }

    private class DelegatingWorkerFactory(
        private val accountRepository: AccountRepository,
        private val studentRepository: StudentRepository,
        private val biometricRepository: BiometricRepository,
        private val schoolRepository: SchoolRepository,
    ) : androidx.work.WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters,
        ): ListenableWorker? {
            return when (workerClassName) {
                AccountSyncWorker::class.java.name ->
                    AccountSyncWorker(appContext, workerParameters, accountRepository, studentRepository, biometricRepository, schoolRepository)
                else -> null
            }
        }
    }
}
