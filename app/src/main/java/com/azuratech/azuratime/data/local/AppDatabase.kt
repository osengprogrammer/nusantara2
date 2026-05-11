package com.azuratech.azuratime.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
// 🔥 FIX: Correctly targeting the nested JournalMode enum

@Database(
    entities = [
        SchoolEntity::class,
        ClassEntity::class,           // 🔥 NEW: Pure class table
        SchoolClassAssignment::class, // 🔥 NEW: Join table
        BiometricFaceEntity::class,
        FaceAssignmentEntity::class,
        CheckInRecordEntity::class,
        StaffAccountEntity::class,
        UserClassAccessEntity::class,
        StudentEntity::class,          // 🔥 NEW: Student Identity
        AccessRequestEntity::class,    // 🔥 NEW: Access Request SSOT
        AttendanceConflictEntity::class, // 🔥 NEW: Conflict Resolution
        AuditLogEntity::class,         // 🔥 NEW: Audit Log
        ExportJobEntity::class,          // 🔥 NEW: Export Jobs
        ReportEntity::class            // 🔥 NEW: Reports
    ],
    version = 32, // 🚀 BUMP TO 32: Adding ReportEntity
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun faceDao(): BiometricFaceDao
    abstract fun faceAssignmentDao(): FaceAssignmentDao
    abstract fun classDao(): ClassDao // 🔥 NEW DAO
    abstract fun schoolDao(): SchoolDao // 🔥 NEW DAO
    abstract fun schoolClassDao(): SchoolClassDao
    abstract fun checkInRecordDao(): CheckInRecordDao
    abstract fun userDao(): com.azuratech.azuratime.data.local.StaffAccountDao
    abstract fun userClassAccessDao(): UserClassAccessDao
    abstract fun studentDao(): StudentDao // 🔥 NEW DAO
    abstract fun accessRequestDao(): AccessRequestDao // 🔥 NEW DAO
    abstract fun attendanceConflictDao(): AttendanceConflictDao // 🔥 NEW DAO
    abstract fun auditLogDao(): AuditLogDao // 🔥 NEW DAO
    abstract fun exportJobDao(): ExportJobDao // 🔥 NEW DAO
    abstract fun reportDao(): ReportDao // 🔥 NEW DAO

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "azura.db"
                )
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                /**
                 * 💡 PRO TIP: Because the jump from 21 to 22 involves deleting 
                 * tables and changing primary keys, destructive migration is 
                 * the safest way for you to test during this dev phase.
                 */
                .fallbackToDestructiveMigration() 
                .build()
                .also { INSTANCE = it }
            }
        }

        fun destroyInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}