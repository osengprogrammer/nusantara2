package com.azuratech.azuratime.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        SchoolEntity::class,
        ClassEntity::class,
        SchoolClassAssignment::class,
        StudentBiometricEntity::class,
        StudentClassAssignmentEntity::class,
        AttendanceRecordEntity::class,
        AccountEntity::class,
        AccountClassAccessEntity::class,
        StudentEntity::class,
        AccessRequestEntity::class,
        AttendanceConflictEntity::class,
        AuditLogEntity::class,
        ExportJobEntity::class,
        ReportEntity::class
    ],
    version = 14,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun studentDao(): StudentDao
    abstract fun attendanceRecordDao(): AttendanceRecordDao
    abstract fun biometricDao(): StudentBiometricDao
    abstract fun studentClassAssignmentDao(): StudentClassAssignmentDao
    abstract fun accountDao(): AccountDao
    abstract fun schoolDao(): SchoolDao
    abstract fun classDao(): ClassDao
    abstract fun schoolClassDao(): SchoolClassDao
    abstract fun accountClassAccessDao(): AccountClassAccessDao
    abstract fun accessRequestDao(): AccessRequestDao
    abstract fun attendanceConflictDao(): AttendanceConflictDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun exportJobDao(): ExportJobDao
    abstract fun reportDao(): ReportDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "local_db2.sqlite"
                )
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .fallbackToDestructiveMigration() 
                .build()
                .also { INSTANCE = it }
            }
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }
}
