package com.azuratech.azuratime.core.domain.model

enum class AccountRole {
    SUPER_ADMIN,
    ADMIN,
    SUPERVISOR,
    USER,
    ;

    companion object {
        fun fromString(role: String): AccountRole {
            return try {
                when (role.uppercase()) {
                    "SUPER_ADMIN" -> SUPER_ADMIN
                    "ADMIN" -> ADMIN
                    "TEACHER", "SUPERVISOR" -> SUPERVISOR
                    "MEMBER", "USER", "OBSERVER" -> USER
                    else -> USER
                }
            } catch (e: Exception) {
                USER
            }
        }
    }
}

fun String?.toAccountRole(): AccountRole = AccountRole.fromString(this ?: "USER")
