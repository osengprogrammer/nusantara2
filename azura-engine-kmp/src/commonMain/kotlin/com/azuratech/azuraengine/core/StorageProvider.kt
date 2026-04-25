package com.azuratech.azuraengine.core

interface StorageProvider {
    fun read(uriString: String): ByteArray
    fun save(data: ByteArray, filename: String, directory: String? = null): String
    fun delete(filename: String): Boolean
    
    // Backup & Share support
    fun getDatabasePath(name: String): String
    fun copyFile(sourcePath: String, destPath: String): Boolean
    fun shareFile(path: String, title: String, mimeType: String)
}
