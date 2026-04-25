package com.azuratech.azuraengine.sync

import kotlinx.serialization.Serializable

@Serializable
data class CsvStudentData(
    val faceId: String,
    val name: String = "",
    val photoUrl: String = "",
    // Metadata mentah (String) dari CSV untuk diproses ke FaceAssignment & FaceSalaryConfig
    val rawMetadata: Map<String, String> = emptyMap() 
)

@Serializable
data class CsvParseResult(
    val students: List<CsvStudentData>,
    val errors: List<String>,
    val totalRows: Int,
    val validRows: Int
)
