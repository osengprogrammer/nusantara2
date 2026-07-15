package com.azuratech.azuratime.core.domain.model

data class ClassModel(
    val id: String,
    val schoolId: String,
    val name: String,
    val grade: String,
    val accountId: String,
    val studentCount: Int,
    val studentIds: List<String> = emptyList(),
    val subjectIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    // Blueprint fields
    val level: Int? = null,
    val category: String? = null,
    val major: String? = null,
    val section: String? = null,
)
