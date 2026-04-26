package com.azuratech.azuratime.domain.result

import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultTest {

    @Test
    fun `test Result exhaustiveness`() {
        val result: Result<String> = Result.Success("test")
        
        val message = when (result) {
            is Result.Success -> "Success"
            is Result.Failure -> "Failure"
            is Result.Loading -> "Loading"
        }
        
        assertTrue(message == "Success")
    }

    @Test
    fun `test Result failure subtypes`() {
        val networkError: Result<Nothing> = Result.Failure(AppError.Network("No internet"))
        val dbError: Result<Nothing> = Result.Failure(AppError.LocalDB("Disk full"))
        
        assertTrue(networkError is Result.Failure && networkError.error is AppError.Network)
        assertTrue(dbError is Result.Failure && dbError.error is AppError.LocalDB)
    }

    @Test
    fun `test Result fold`() {
        val success: Result<String> = Result.Success("data")
        val failure: Result<String> = Result.Failure(AppError.Unknown("error"))

        val successValue = success.fold(
            onSuccess = { it },
            onFailure = { it.message ?: "" }
        )
        assertTrue(successValue == "data")

        val failureValue = failure.fold(
            onSuccess = { it },
            onFailure = { it.message ?: "" }
        )
        assertTrue(failureValue == "error")
    }

    @Test
    fun `test Result getOrNull`() {
        val success: Result<String> = Result.Success("data")
        val failure: Result<String> = Result.Failure(AppError.Unknown("error"))

        assertTrue(success.getOrNull() == "data")
        assertTrue(failure.getOrNull() == null)
    }
}
