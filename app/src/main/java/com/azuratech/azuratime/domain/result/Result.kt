package com.azuratech.azuratime.domain.result

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Failure(val error: AppError) : Result<Nothing>()
    object Loading : Result<Nothing>()

    inline fun <R> fold(
        onSuccess: (T) -> R,
        onFailure: (AppError) -> R,
        onLoading: () -> R = { throw IllegalStateException("Result is Loading") }
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
}
