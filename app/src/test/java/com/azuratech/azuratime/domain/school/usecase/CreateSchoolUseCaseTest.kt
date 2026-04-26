package com.azuratech.azuratime.domain.school.usecase

import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.data.repo.SchoolRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CreateSchoolUseCaseTest {

    @MockK
    lateinit var schoolRepository: SchoolRepository

    private lateinit var createSchoolUseCase: CreateSchoolUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        createSchoolUseCase = CreateSchoolUseCase(schoolRepository)
    }

    @Test
    fun `validation rejects blank name`() = runTest {
        val result = createSchoolUseCase("account1", "", "Asia/Jakarta")
        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).error is AppError.BusinessRule)
    }

    @Test
    fun `successful creation calls repository and returns id`() = runTest {
        coEvery { schoolRepository.saveSchool(any()) } returns Result.Success(Unit)
        
        val result = createSchoolUseCase("account1", "New School", "Asia/Jakarta")
        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).data.isNotEmpty())
    }
}
