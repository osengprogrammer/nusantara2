package com.azuratech.azuratime.domain.result

sealed class AppError {
    abstract val message: String?
    data class Network(override val message: String?) : AppError()
    data class LocalDB(override val message: String?) : AppError()
    data class BusinessRule(override val message: String?) : AppError()
    data class Unknown(override val message: String?) : AppError()
}
