package com.azuratech.azuratime.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.azuratech.azuraengine.model.School
import java.util.UUID

@Entity(tableName = "schools")
data class SchoolEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val accountId: String,
    val name: String,
    val timezone: String,
    val status: String = "ACTIVE",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED" // com.azuratech.azuratime.domain.model.SyncStatus.name
) {
    fun toDomain(): School = School(
        id = id,
        accountId = accountId,
        name = name,
        timezone = timezone,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
