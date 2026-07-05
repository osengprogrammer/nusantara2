package com.azuratech.azuratime.core.auth.impl.di

import com.azuratech.azuratime.core.auth.api.repository.AuthRepository
import com.azuratech.azuratime.core.auth.impl.repo.AuthRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module for Auth Repository binding.
 * 
 * Located in :core-auth-impl because this module contains the Firebase implementation.
 * This allows :feature-auth to simply depend on :core-auth-impl and get the binding automatically.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository
}