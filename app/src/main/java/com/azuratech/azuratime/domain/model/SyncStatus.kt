package com.azuratech.azuratime.domain.model

/**
 * Represents the synchronization state of a domain object with the remote server.
 */
enum class SyncStatus {
    SYNCED,           // Fully synchronized with remote
    PENDING_INSERT,   // New record locally, waiting for remote upload
    PENDING_UPDATE,   // Modified locally, waiting for remote sync
    PENDING_DELETE,   // Deleted locally (soft-delete), waiting for remote deletion
    ERROR             // Sync failed after multiple attempts
}
