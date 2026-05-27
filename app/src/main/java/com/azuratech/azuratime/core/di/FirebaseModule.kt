package com.azuratech.azuratime.core.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.azuratech.azuratime.R
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): com.google.firebase.auth.FirebaseAuth = com.google.firebase.auth.FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseRemoteConfig(): FirebaseRemoteConfig {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600) // 1 hour for testing
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        return remoteConfig
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): com.google.firebase.firestore.FirebaseFirestore {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        // 🔥 Konfigurasi Offline Persistence (100MB Cache)
        db.firestoreSettings = com.google.firebase.firestore.firestoreSettings {
            setLocalCacheSettings(
                com.google.firebase.firestore.persistentCacheSettings {
                    setSizeBytes(100 * 1024 * 1024)
                },
            )
        }
        return db
    }
}
