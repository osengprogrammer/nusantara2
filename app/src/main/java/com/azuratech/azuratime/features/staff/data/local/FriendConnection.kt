package com.azuratech.azuratime.features.staff.data.local

/**
 * 🤝 SEDULURAN (FRIENDSHIP): Menyimpan status koneksi antar Guru.
 */
data class FriendConnection(
    val friendName: String,
    val friendEmail: String,
    val status: String // "REQUEST_SENT", "PENDING_APPROVAL", "FRIENDS"
)
