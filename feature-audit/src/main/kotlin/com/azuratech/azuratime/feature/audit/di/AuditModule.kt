package com.azuratech.azuratime.feature.audit.di

import android.content.Context
import androidx.room.Room
import com.azuratech.azuratime.core.api.audit.AuditRepository
import com.azuratech.azuratime.feature.audit.data.AuditRepositoryImpl
import com.azuratech.azuratime.feature.audit.data.local.AuditDao
import com.azuratech.azuratime.feature.audit.data.local.AuditDatabase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuditModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AuditDatabase {
        return Room.databaseBuilder(
            context,
            AuditDatabase::class.java,
            "audit_database"
        ).build()
    }

    @Provides
    fun provideAuditDao(database: AuditDatabase): AuditDao {
        return database.auditDao()
    }

    @Module
    @InstallIn(SingletonComponent::class)
    abstract class BindsModule {
        @Binds
        @Singleton
        abstract fun bindAuditRepository(
            auditRepositoryImpl: AuditRepositoryImpl
        ): AuditRepository
    }
}
