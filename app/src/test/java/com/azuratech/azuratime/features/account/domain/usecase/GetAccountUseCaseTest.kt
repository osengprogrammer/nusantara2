package com.azuratech.azuratime.features.account.domain.usecase

import app.cash.turbine.test
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.domain.model.AccountRole
import com.azuratech.azuratime.features.account.domain.model.Account
import com.azuratech.azuratime.features.account.domain.model.SchoolMembership
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/*
 * 🚀 GET ACCOUNT USECASE TEST (v3.2.0-ai-native)
 * MVI Contract Test: Event -> State + Effect
 * Pattern: MockK + Turbine + Result<T> handling
 */
class GetAccountUseCaseTest {

    private lateinit var repository: AccountRepository
    private lateinit var getAccountUseCase: GetAccountUseCase

    @Before
    fun setup() {
        repository = mockk()
        getAccountUseCase = GetAccountUseCase(repository)
    }

    @Test
    fun `when repository returns Success, usecase emits Success`() = runTest {
        // Arrange
        val accountId = "test_id"
        val mockAccount = Account(
            accountId = accountId,
            email = "test@azura.com",
            name = "Test Account",
            photoUrl = "https://photo.url",
            role = AccountRole.SUPERVISOR,
            activeSchoolId = "school_123",
            memberships = mapOf(
                "school_123" to SchoolMembership(
                    schoolName = "Azura Academy",
                    role = "SUPERVISOR",
                    status = "ACTIVE",
                ),
            ),
        )
        every { repository.getAccountFlow(accountId) } returns flowOf(Result.Success(mockAccount))

        // Act & Assert
        getAccountUseCase(accountId).test {
            val result = awaitItem()
            assert(result is Result.Success)
            assertEquals(mockAccount, (result as Result.Success).data)
            awaitComplete()
        }
    }

    @Test
    fun `when repository returns Failure, usecase emits Failure`() = runTest {
        // Arrange
        val accountId = "test_id"
        val mockError = AppError.LocalDB("Not Found")
        every { repository.getAccountFlow(accountId) } returns flowOf(Result.Failure(mockError))

        // Act & Assert
        getAccountUseCase(accountId).test {
            val result = awaitItem()
            assert(result is Result.Failure)
            assertEquals(mockError, (result as Result.Failure).error)
            awaitComplete()
        }
    }
}
