package com.azuratech.azuratime.core.data.local

import android.content.Context
import androidx.room.*
import com.azuratech.azuratime.features.school.data.local.*
import com.azuratech.azuratime.features.student.data.local.*
import com.azuratech.azuratime.features.attendance.data.local.*
import com.azuratech.azuratime.features.biometric.data.local.*
import com.azuratech.azuratime.features.staff.data.local.*
import com.azuratech.azuratime.features.reporting.data.local.*

@Database(
    entities = [
        SchoolEntity::class,
        ClassEntity::class,
        SchoolClassAssignment::class,
        BiometricFaceEntity::class,
        FaceAssignmentEntity::class,
        AttendanceRecordEntity::class,
        StaffAccountEntity::class,
        UserClassAccessEntity::class,
        StudentEntity::class,
        AccessRequestEntity::class,
        AttendanceConflictEntity::class,
        AuditLogEntity::class,
        ExportJobEntity::class,
        ReportEntity::class
    ],
    version = 12,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun faceDao(): BiometricFaceDao
    abstract fun faceAssignmentDao(): FaceAssignmentDao
    abstract fun classDao(): ClassDao
    abstract fun schoolDao(): SchoolDao
    abstract fun schoolClassDao(): SchoolClassDao
    abstract fun attendanceRecordDao(): AttendanceRecordDao
    abstract fun userDao(): StaffAccountDao
    abstract fun userClassAccessDao(): UserClassAccessDao
    abstract fun studentDao(): StudentDao
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
                    "azura.db"
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
