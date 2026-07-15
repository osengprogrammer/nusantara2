package com.azuratech.azuratime.core.result

/**
 * Represents all possible errors in the application domain.
 * Used for consistent error handling across repositories and use cases.
 */
sealed class AppError(message: String?) : Exception(message) {

    data class NetworkError(
        val errorMessage: String,
        val code: Int? = null
    ) : AppError(errorMessage)

    data class Network(
        val errorMessage: String? = null
    ) : AppError(errorMessage ?: "Network error")

    data class LocalError(
        val errorMessage: String
    ) : AppError(errorMessage)

    data class LocalDB(
        val errorMessage: String? = null
    ) : AppError(errorMessage ?: "Local database error")

    data class BusinessRule(
        val errorMessage: String? = null
    ) : AppError(errorMessage ?: "Business rule violation")

    data class Conflict(
        val errorMessage: String? = null
    ) : AppError(errorMessage ?: "Data conflict")

    data class ValidationError(
        val errorMessage: String
    ) : AppError(errorMessage)

    data object Unauthorized : AppError("Unauthorized access")

    data object Unknown : AppError("An unknown error occurred")
}
