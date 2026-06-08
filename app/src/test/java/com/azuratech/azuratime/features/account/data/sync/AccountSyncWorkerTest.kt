package com.azuratech.azuratime.features.account.data.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import androidx.work.testing.TestListenableWorkerBuilder
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result as DomainResult
import com.azuratech.azuratime.features.account.data.local.AccountEntity
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import com.azuratech.azuratime.features.biometric.domain.repository.BiometricRepository
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import com.azuratech.azuratime.features.student.domain.repository.StudentRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/*
 * 🚀 ACCOUNT SYNCWORKER INTEGRATION TEST (v3.2.0-ai-native)
 * Tests offline-first sync: Room ↔ Firestore via WorkManager
 * Pattern: TestDispatcher, FakeDataSource, Result<T> assertions
 */
@RunWith(RobolectricTestRunner::class)
class AccountSyncWorkerTest {

    private lateinit var context: Context
    private lateinit var accountRepository: AccountRepository
    private lateinit var studentRepository: StudentRepository
    private lateinit var biometricRepository: BiometricRepository
    private lateinit var schoolRepository: SchoolRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        accountRepository = mockk(relaxed = true)
        studentRepository = mockk(relaxed = true)
        biometricRepository = mockk(relaxed = true)
        schoolRepository = mockk(relaxed = true)

        // Default successes
        coEvery { accountRepository.pushAccount(any()) } returns DomainResult.Success(Unit)
        coEvery { studentRepository.pushPendingProfiles() } returns DomainResult.Success(Unit)
        coEvery { biometricRepository.syncBiometrics() } returns DomainResult.Success(Unit)
        coEvery { schoolRepository.syncClasses(any(), any()) } returns DomainResult.Success(Unit)
        coEvery { schoolRepository.syncSchools(any()) } returns DomainResult.Success(Unit)
        coEvery { studentRepository.pullStudents(any()) } returns DomainResult.Success(Unit)
        coEvery { studentRepository.autoHealStudentIdentities(any()) } returns DomainResult.Success(Unit)
    }

    @Test
    fun whenFullSyncSucceeds_returnsSuccess() = runTest {
        // Arrange
        val accountId = "test_account_123"
        val mockEntity = AccountEntity(
            accountId = accountId,
            email = "test@azura.com",
            name = "Test Account",
        )

        coEvery { accountRepository.syncAccount(accountId) } returns DomainResult.Success(mockEntity)

        val worker = TestListenableWorkerBuilder<AccountSyncWorker>(
            context = context,
            inputData = workDataOf("accountId" to accountId),
        ).setWorkerFactory(DelegatingWorkerFactory(accountRepository, studentRepository, biometricRepository, schoolRepository)).build()

        // Act
        val result = worker.doWork()

        // Assert
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun whenAccountPullFails_returnsFailure() = runTest {
        // Arrange
        val accountId = "test_account_123"
        coEvery { accountRepository.syncAccount(accountId) } returns DomainResult.Failure(AppError.BusinessRule("Not Found"))

        val worker = TestListenableWorkerBuilder<AccountSyncWorker>(
            context = context,
            inputData = workDataOf("accountId" to accountId),
        ).setWorkerFactory(DelegatingWorkerFactory(accountRepository, studentRepository, biometricRepository, schoolRepository)).build()

        // Act
        val result = worker.doWork()

        // Assert
        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun whenNetworkErrorOnPush_returnsRetry() = runTest {
        // Arrange
        val accountId = "test_account_123"
        val mockEntity = AccountEntity(accountId = accountId, email = "t@a.com", name = "T")
        coEvery { accountRepository.syncAccount(accountId) } returns DomainResult.Success(mockEntity)
        coEvery { accountRepository.pushAccount(accountId) } returns DomainResult.Failure(AppError.Network("Timeout"))

        val worker = TestListenableWorkerBuilder<AccountSyncWorker>(
            context = context,
            inputData = workDataOf("accountId" to accountId),
        ).setWorkerFactory(DelegatingWorkerFactory(accountRepository, studentRepository, biometricRepository, schoolRepository)).build()

        // Act
        val result = worker.doWork()

        // Assert
        assertEquals(ListenableWorker.Result.retry(), result)
    }

    /**
     * Helper factory to inject mock repositories into the HiltWorker during test.
     */
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
