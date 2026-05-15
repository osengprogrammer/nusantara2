package com.azuratech.azuratime.core.domain.media

interface StorageProvider {
    suspend fun save(path: String, data: ByteArray)
    suspend fun load(path: String): ByteArray?
    suspend fun delete(path: String)
}
