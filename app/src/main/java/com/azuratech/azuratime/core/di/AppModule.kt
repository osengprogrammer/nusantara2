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
    fun provideFaceDao(db: AppDatabase) = db.faceDao()

    @Provides
    fun provideFaceAssignmentDao(db: AppDatabase) = db.faceAssignmentDao()

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