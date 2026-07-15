package com.azuratech.azuratime.core.result

/**
 * Represents the outcome of a single bulk processing operation (e.g., CSV import).
 */
data class ProcessResult(
    val faceId: String,
    val name: String,
    val status: String,
)
