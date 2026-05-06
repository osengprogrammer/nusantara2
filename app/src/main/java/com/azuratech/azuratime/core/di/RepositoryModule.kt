package com.azuratech.azuratime.core.di

import com.azuratech.azuratime.data.repo.*
import com.azuratech.azuratime.domain.student.repository.StudentRepository
import com.azuratech.azuratime.domain.checkin.repository.CheckInRepository
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
    abstract fun bindStudentRepository(
        impl: StudentRepositoryImpl
    ): StudentRepository

    @Binds
    @Singleton
    abstract fun bindLegacyStudentRepository(
        impl: StudentRepositoryLegacyImpl
    ): com.azuratech.azuratime.data.repo.StudentRepository

    @Binds
    @Singleton
    abstract fun bindCheckInRepository(
        impl: CheckInRepositoryImpl
    ): CheckInRepository

    @Binds
    @Singleton
    abstract fun bindAccessRequestRepository(
        impl: AccessRequestRepositoryImpl
    ): AccessRequestRepository

    @Binds
    @Singleton
    abstract fun bindFileStorage(
        impl: PhotoStorageUtils
    ): FileStorage
}
