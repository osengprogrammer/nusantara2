package com.azuratech.azuratime.domain.model

/**
 * 🎓 MEMBERSHIP STATUS
 * Tracks the state of a user's membership or join request in a school.
 */
enum class MembershipStatus {
    PENDING,  // Request sent, waiting for Admin approval
    ACTIVE,   // Approved and active member
    REJECTED, // Request denied by Admin
    LEFT      // User voluntarily left the school or access revoked
}
