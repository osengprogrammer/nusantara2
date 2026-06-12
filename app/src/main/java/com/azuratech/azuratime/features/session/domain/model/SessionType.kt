package com.azuratech.azuratime.features.session.domain.model

/**
 * 📅 SESSION TYPE (v3.7.0-base)
 * Defines the scope and hierarchy of an attendance session.
 */
enum class SessionType {
    ACADEMIC, // Class + Subject (Default legacy behavior)
    CLASS_WIDE, // Class only (e.g., Homeroom, Class Ceremony)
    GLOBAL, // School-wide (e.g., Flag Ceremony, General Seminar)
}
