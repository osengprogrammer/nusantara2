package com.azuratech.azuratime.features.student.domain.model

data class StudentModel(
    val studentId: String,
    val schoolId: String,
    val name: String,
    val studentCode: String,
    val classId: String,
    val createdAt: Long,
    val isSynced: Boolean,
)
