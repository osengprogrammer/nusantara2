package com.azuratech.azuratime.core.data.local

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
    val syncStatus: String = "SYNCED",
) {
    fun toDomain(): School = School(
        id = id,
        accountId = accountId,
        name = name,
        timezone = timezone,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        fun fromDomain(school: School): SchoolEntity = SchoolEntity(
            id = school.id,
            accountId = school.accountId,
            name = school.name,
            timezone = school.timezone,
            status = school.status,
            createdAt = school.createdAt,
            updatedAt = school.updatedAt,
            syncStatus = "SYNCED",
        )
    }
}
