package com.azuratech.azuratime.features.session.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val subjectId: String,
    val name: String,
    val description: String? = null,
    val schoolId: String,
    val isSynced: Boolean = false,
)
