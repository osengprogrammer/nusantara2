package com.azuratech.azuratime.feature.audit.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [AuditEntity::class], version = 1, exportSchema = false)
abstract class AuditDatabase : RoomDatabase() {
    abstract fun auditDao(): AuditDao
}
