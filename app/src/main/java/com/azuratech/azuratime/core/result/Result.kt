package com.azuratech.azuratime.core.result

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * A wrapper class for handling success and failure states in the domain layer.
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Failure(val error: AppError) : Result<Nothing>()
    data object Network : Result<Nothing>()
    data object Loading : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): T? = if (this is Success) data else null
    fun exceptionOrNull(): AppError? = if (this is Failure) error else null
}

// Extension functions for fluent error handling
inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T> Result<T>.onFailure(action: (AppError) -> Unit): Result<T> {
    if (this is Result.Failure) action(error)
    return this
}

// Monadic bind (flatMap)
inline fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> {
    return when (this) {
        is Result.Success -> transform(data)
        is Result.Failure -> this
        is Result.Network -> Result.Network
        is Result.Loading -> Result.Loading
    }
}

// Transform the success value
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> {
    return when (this) {
        is Result.Success -> Result.Success(transform(data))
        is Result.Failure -> this
        is Result.Network -> Result.Network
        is Result.Loading -> Result.Loading
    }
}

// Helper to safely execute blocking code and wrap it in a Result
inline fun <T> asLocalResult(block: () -> T): Result<T> {
    return try {
        Result.Success(block())
    } catch (e: Exception) {
        Result.Failure(AppError.LocalError(e.message ?: "Unknown local error"))
    }
}

// Flow extension: wraps each emission in a Result, catching upstream errors
fun <T> Flow<T>.asLocalResult(): Flow<Result<T>> {
    return this
        .map<T, Result<T>> { Result.Success(it) }
        .catch { e -> emit(Result.Failure(AppError.LocalError(e.message ?: "Unknown error"))) }
}
