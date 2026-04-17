package com.azuratech.azuratime.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
// 🔥 FIX: Correctly targeting the nested JournalMode enum

@Database(
    entities = [
        ClassEntity::class,           // 🔥 NEW: Pure class table
        FaceEntity::class,
        FaceAssignmentEntity::class,
        CheckInRecordEntity::class,
        UserEntity::class,
        UserClassAccessEntity::class
    ],
    version = 22, // 🚀 BUMP TO 22: Removing Salary and Options for good!
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun faceDao(): FaceDao
    abstract fun faceAssignmentDao(): FaceAssignmentDao
    abstract fun classDao(): ClassDao // 🔥 NEW DAO
    abstract fun checkInRecordDao(): CheckInRecordDao
    abstract fun userDao(): UserDao
    abstract fun userClassAccessDao(): UserClassAccessDao

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