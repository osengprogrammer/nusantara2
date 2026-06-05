package com.azuratech.azuratime.core.domain.sync

import com.azuratech.azuraengine.core.StorageProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupUtils @Inject constructor(
    private val storageProvider: StorageProvider,
) {
    fun backupAndShareDatabase() {
        try {
            // 1. Get original database path
            val dbPath = storageProvider.getDatabasePath(DB_NAME)

            // 2. Create copy in cache folder for easier sharing
            val backupFileName = "BACKUP_AZURA_${System.currentTimeMillis()}.db"
            val backupPath = storageProvider.save(ByteArray(0), backupFileName, "cache")

            if (storageProvider.copyFile(dbPath, backupPath)) {
                // 3. Share via StorageProvider
                storageProvider.shareFile(backupPath, "Save Azura Backup To...", "application/octet-stream")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 🔥 RESTORE DATABASE FROM FILE
     * Restore .db file from external URI and overwrite local database.
     */
    fun restoreDatabase(backupUriString: String, onComplete: () -> Unit) {
        try {
            // 1. Get destination database path
            val dbPath = storageProvider.getDatabasePath(DB_NAME)

            // 2. Copy file by reading URI via StorageProvider
            val backupBytes = storageProvider.read(backupUriString)
            if (backupBytes.isNotEmpty()) {
                storageProvider.save(backupBytes, DB_NAME, "databases")
                java.io.File(dbPath).writeBytes(backupBytes)
                onComplete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val DB_NAME = "azura.db" // Database name as per AppDatabase.kt
    }
}
