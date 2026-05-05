package com.azuratech.azuratime.core.di

import com.azuratech.azuratime.data.repo.StudentRepositoryImpl
import com.azuratech.azuratime.domain.student.repository.StudentRepository
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
    abstract fun bindAccessRequestRepository(
        impl: com.azuratech.azuratime.data.repo.AccessRequestRepositoryImpl
    ): com.azuratech.azuratime.data.repo.AccessRequestRepository
}
