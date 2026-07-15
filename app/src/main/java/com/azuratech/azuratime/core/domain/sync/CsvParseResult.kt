package com.azuratech.azuratime.core.domain.sync

/**
 * The full result of parsing a CSV student file.
 */
data class CsvParseResult(
    val students: List<CsvStudentData>,
    val errors: List<String>,
    val totalRows: Int,
    val validRows: Int,
)
