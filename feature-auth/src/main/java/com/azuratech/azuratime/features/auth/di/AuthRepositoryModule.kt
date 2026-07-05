package com.azuratech.azuratime.features.auth.di

import com.azuratech.azuratime.core.auth.api.repository.AuthRepository
import com.azuratech.azuratime.features.auth.data.repo.AuthRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module for Auth Repository binding.
 * 
 * This module is placed in :feature-auth (not a separate :core-auth-impl)
 * because the implementation depends on infrastructure (AppDatabase, SessionManager)
 * that is currently only available in the :app module.
 * 
 * Future refactoring: Once AppDatabase, SessionManager, AccountRepository, etc.
 * are moved to proper core modules, the implementation can be moved to
 * :core-auth-impl and this binding can follow.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository
}