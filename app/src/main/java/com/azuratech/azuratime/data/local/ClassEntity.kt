package com.azuratech.azuratime.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "classes",
    indices = [Index(value = ["schoolId"])] // 🔥 Index for fast tenant/school filtering
)
data class ClassEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val schoolId: String = "", // 🔥 Added tenant scoping to isolate data
    val name: String,
    val displayOrder: Int = 0,
    val isSynced: Boolean = false
)