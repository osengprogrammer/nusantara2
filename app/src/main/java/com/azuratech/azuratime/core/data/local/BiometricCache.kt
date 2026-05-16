package com.azuratech.azuratime.core.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Inmemory cache for face embeddings to avoid repeated database reads.
 *  UPDATED: Tenant-aware. Automatically flushes if the active school changes.
 */
object BiometricCache {
    private val cache = mutableListOf<Pair<String, FloatArray>>()

    // Tracks the currently cached tenant to prevent biometric cross-contamination
    private var currentSchoolId: String? = null

    /**
     * Clear the cache.
     */
    fun clear() {
        cache.clear()
        currentSchoolId = null
    }

    /**
     * Load embeddings for a specific school. If the schoolId is different from 
     * what's currently in memory, the cache is flushed first.
     */
    suspend fun load(context: Context, schoolId: String): List<Pair<String, FloatArray>> = withContext(Dispatchers.IO) {
        if (currentSchoolId != schoolId) {
            clear()
            currentSchoolId = schoolId
        }

        if (cache.isNotEmpty()) return@withContext cache

        val db = AppDatabase.getInstance(context)
        val entities = db.biometricDao().getAllStudentsForScanningList(schoolId)
        
        val loaded = entities.mapNotNull { entity ->
            val embedding = entity.embedding
            if (embedding != null) {
                entity.studentId to embedding
            } else {
                null
            }
        }
        
        cache.addAll(loaded)
        return@withContext cache
    }

    /**
     * Forces a refresh of the cache by clearing and reloading from database for the specified school.
     */
    suspend fun refresh(context: Context, schoolId: String): List<Pair<String, FloatArray>> {
        clear()
        return load(context, schoolId)
    }
}
