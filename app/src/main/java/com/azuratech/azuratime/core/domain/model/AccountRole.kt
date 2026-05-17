package com.azuratech.azuratime.core.domain.model

enum class AccountRole {
    ADMIN,
    MEMBER,
    TEACHER,
    OBSERVER,
    ;

    companion object {
        fun fromString(role: String): AccountRole {
            return try {
                valueOf(role.uppercase())
            } catch (e: Exception) {
                MEMBER
            }
        }
    }
}

fun String?.toAccountRole(): AccountRole = AccountRole.fromString(this ?: "MEMBER")
