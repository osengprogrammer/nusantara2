package com.azuratech.azuratime.core.domain.sync

/**
 * A single row parsed from a CSV student import.
 */
data class CsvStudentData(
    val faceId: String,
    val name: String,
    val photoUrl: String = "",
    val rawMetadata: Map<String, String> = emptyMap(),
)
