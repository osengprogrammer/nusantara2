package com.azuratech.azuraengine.result

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch

sealed class AppError {
    abstract val message: String?
    data class Network(override val message: String?) : AppError()
    data class LocalDB(override val message: String?) : AppError()
    data class BusinessRule(override val message: String?) : AppError()
    data class Conflict(override val message: String?) : AppError()
    data class Unknown(override val message: String?) : AppError()
}

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Failure(val error: AppError) : Result<Nothing>()
    object Loading : Result<Nothing>()

    inline fun <R> fold(
        onSuccess: (T) -> R,
        onFailure: (AppError) -> R,
        onLoading: () -> R = { throw IllegalStateException("Result is Loading") },
    ): R = when (this) {
        is Success -> onSuccess(data)
        is Failure -> onFailure(error)
        is Loading -> onLoading()
    }

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> Failure(error)
        is Loading -> Loading
    }

    inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> transform(data)
        is Failure -> Failure(error)
        is Loading -> Loading
    }
}

inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T> Result<T>.onFailure(action: (AppError) -> Unit): Result<T> {
    if (this is Result.Failure) action(error)
    return this
}

/**
 * 🔥 AI Native: Map a Flow of data into a Flow of Result<T>.
 */
fun <T> Flow<T>.asResult(
    errorMapper: (Throwable) -> AppError = { AppError.Unknown(it.message) },
): Flow<Result<T>> = this
    .map<T, Result<T>> { Result.Success(it) }
    .catch { e -> emit(Result.Failure(errorMapper(e))) }

fun <T> Flow<T>.asLocalResult(): Flow<Result<T>> =
    asResult { AppError.LocalDB(it.message) }
