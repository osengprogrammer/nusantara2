package com.azuratech.azuratime.feature.ims.di

import android.content.Context
import androidx.room.Room
import com.azuratech.azuratime.core.api.ims.ImsRepository
import com.azuratech.azuratime.feature.ims.data.ImsRepositoryImpl
import com.azuratech.azuratime.feature.ims.data.local.AzuraDatabase
import com.azuratech.azuratime.feature.ims.data.local.ProductDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ImsModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AzuraDatabase {
        return Room.databaseBuilder(
            context,
            AzuraDatabase::class.java,
            "ims_database"
        ).build()
    }

    @Provides
    fun provideProductDao(database: AzuraDatabase): ProductDao {
        return database.productDao()
    }

    @Provides
    @Singleton
    fun provideImsRepository(productDao: ProductDao): ImsRepository {
        return ImsRepositoryImpl(productDao)
    }
}
