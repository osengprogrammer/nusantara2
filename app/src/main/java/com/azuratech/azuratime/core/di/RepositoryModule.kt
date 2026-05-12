package com.azuratech.azuratime.core.di

import com.azuratech.azuratime.data.repo.*
import com.azuratech.azuratime.features.student.domain.repository.StudentRepository
import com.azuratech.azuratime.features.student.data.repo.StudentRepositoryImpl
import com.azuratech.azuratime.features.attendance.domain.repository.CheckInRepository
import com.azuratech.azuratime.features.attendance.data.repo.CheckInRepositoryImpl
import com.azuratech.azuratime.domain.media.FileStorage
import com.azuratech.azuratime.domain.media.PhotoStorageUtils
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun provideStudentRepository(
        impl: StudentRepositoryImpl
    ): StudentRepository

    @Binds
    @Singleton
    abstract fun provideCheckInRepository(
        impl: CheckInRepositoryImpl
    ): CheckInRepository

    @Binds
    @Singleton
    abstract fun provideAccessRequestRepository(
        impl: AccessRequestRepositoryImpl
    ): com.azuratech.azuratime.data.repo.AccessRequestRepository

    @Binds
    @Singleton
    abstract fun provideFileStorage(
        impl: PhotoStorageUtils
    ): com.azuratech.azuratime.domain.media.FileStorage
}
