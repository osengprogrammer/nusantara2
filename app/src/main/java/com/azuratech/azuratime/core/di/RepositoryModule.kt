package com.azuratech.azuratime.core.di

import com.azuratech.azuratime.data.repo.StudentRepository
import com.azuratech.azuratime.data.repo.StudentRepositoryImpl
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
    abstract fun bindFileStorage(
        impl: PhotoStorageUtils
    ): FileStorage
}
