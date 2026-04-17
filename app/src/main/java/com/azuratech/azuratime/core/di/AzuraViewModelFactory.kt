package com.azuratech.azuratime.core.di

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.azuratech.azuratime.data.local.AppDatabase

/**
 * ⚠️ DEPRECATED: Pindah ke Hilt (hiltViewModel())
 * Gunakan file ini hanya jika masih ada ViewModel warisan (legacy) 
 * yang belum sempat di-migrasi ke @HiltViewModel.
 */
class AzuraViewModelFactory(
    private val application: Application,
    private val databaseProvider: () -> AppDatabase
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return try {
            // Mencoba instansiasi dengan parameter Application
            modelClass.getConstructor(Application::class.java).newInstance(application) as T
        } catch (e: Exception) {
            // Mencoba instansiasi tanpa parameter
            modelClass.getDeclaredConstructor().newInstance() as T
        }
    }
}