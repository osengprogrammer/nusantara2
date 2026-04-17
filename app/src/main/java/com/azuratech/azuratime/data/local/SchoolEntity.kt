package com.azuratech.azuratime.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schools")
data class SchoolEntity(
    @PrimaryKey val id: String, // Matches the Firestore document ID
    val name: String,
    val address: String? = null,
    val logoUrl: String? = null,
    
    // Optional: Global settings for this specific school that the app needs to know
    val timezone: String = "Asia/Jakarta", 
    val isActive: Boolean = true,
    
    val lastUpdated: Long = System.currentTimeMillis()
)