package com.azuratech.azuratime.core.di

import android.content.Context
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.AppDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // =====================================================
    // 🗄️ LOCAL DATABASE PROVIDER (Room)
    // =====================================================
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    // =====================================================
    // 🔑 SESSION MANAGER PROVIDER (DataStore/SharedPreferences)
    // =====================================================
    @Provides
    @Singleton
    fun provideSessionManager(@ApplicationContext context: Context): SessionManager {
        return SessionManager.getInstance(context)
    }

    // =====================================================
    // 🗡️ DAO PROVIDERS
    // =====================================================
    @Provides
    fun provideStudentDao(db: AppDatabase) = db.studentDao()

    @Provides
    fun provideFaceDao(db: AppDatabase): com.azuratech.azuratime.data.local.BiometricFaceDao = db.faceDao()

    @Provides
    fun provideFaceAssignmentDao(db: AppDatabase) = db.faceAssignmentDao()

    @Provides
    fun provideUserDao(db: AppDatabase): com.azuratech.azuratime.data.local.StaffAccountDao = db.userDao()

    @Provides
    fun provideClassDao(db: AppDatabase) = db.classDao()

    @Provides
    fun provideSchoolDao(db: AppDatabase) = db.schoolDao()

    @Provides
    fun provideUserClassAccessDao(db: AppDatabase) = db.userClassAccessDao()

    @Provides
    fun provideAccessRequestDao(db: AppDatabase) = db.accessRequestDao()

    @Provides
    fun provideAttendanceConflictDao(db: AppDatabase) = db.attendanceConflictDao()

    // =====================================================
    // ☁️ FIREBASE CLOUD PROVIDERS
    // =====================================================

    // ❌ FUNGSI provideFirebaseFirestore() TELAH DIHAPUS DARI SINI ❌
    // Sudah ditangani secara eksklusif oleh FirebaseModule.

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }
}