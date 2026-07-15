package com.azuratech.azuratime.features.school.domain.model

/**
 * Domain model for School.
 */
data class School(
    val id: String,
    val accountId: String,
    val name: String,
    val timezone: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long
)
