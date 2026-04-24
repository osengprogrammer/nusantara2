package com.azuratech.azuratime.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * In‑memory cache for face embeddings to avoid repeated database reads.
 * 🔥 UPDATED: Tenant-aware. Automatically flushes if the active school changes.
 */
object FaceCache {
    private val cache = mutableListOf<Pair<String, FloatArray>>()

    // Tracks the currently cached tenant to prevent biometric cross-contamination
    private var currentSchoolId: String? = null

    /**
     * Loads all faces from the database on the IO dispatcher, caching them the first time.
     * Requires the active schoolId to ensure strict tenant isolation.
     */
    suspend fun load(context: Context, schoolId: String): List<Pair<String, FloatArray>> =
        withContext(Dispatchers.IO) {
            // 🔥 The "Invisible Wall": If the user switched schools, nuke the old cache.
            if (currentSchoolId != schoolId) {
                println("[FaceCache] School context switched: $currentSchoolId -> $schoolId. Flushing cache.")
                clear()
                currentSchoolId = schoolId
            }

            if (cache.isEmpty()) {
                println("[FaceCache] Cache is empty, loading from database for school: $schoolId")
                // Retrieve only faces that HAVE embeddings (Enrolled) for the ACTIVE school
                val faces = AppDatabase
                    .getInstance(context)
                    .faceDao()
                    .getAllFacesForScanningList(schoolId)

                println("[FaceCache] Loaded ${faces.size} enrolled faces from database")

                // Map faceId to embedding
                val pairs = faces.mapNotNull { faceEntity ->
                    faceEntity.embedding?.let { faceEntity.faceId to it }
                }

                cache.addAll(pairs)
                println("[FaceCache] Added ${pairs.size} faces to cache")
            } else {
                println("[FaceCache] Using cached data: ${cache.size} faces for school: $schoolId")
            }
            cache
        }

    /**
     * Loads all enrolled faces with their face IDs from the database.
     * Bypasses the in-memory cache but still strictly filters by schoolId.
     */
    suspend fun loadWithfaceIds(context: Context, schoolId: String): List<Triple<String, String, FloatArray>> =
        withContext(Dispatchers.IO) {
            // Retrieve only enrolled faces for the ACTIVE school
            val faces = AppDatabase
                .getInstance(context)
                .faceDao()
                .getAllFacesForScanningList(schoolId)

            // Map entities to faceId-name-embedding triples
            faces.mapNotNull { faceEntity ->
                faceEntity.embedding?.let { Triple(faceEntity.faceId, faceEntity.name, it) }
            }
        }

    /**
     * Clears the in-memory cache.
     */
    fun clear() {
        println("[FaceCache] Clearing cache (had ${cache.size} faces)")
        cache.clear()
        // We do not clear currentSchoolId here so refresh() knows we are just reloading the same school.
    }

    /**
     * Refreshes the cache by clearing and reloading from database for the specified school.
     */
    suspend fun refresh(context: Context, schoolId: String): List<Pair<String, FloatArray>> {
        clear()
        return load(context, schoolId)
    }
}
