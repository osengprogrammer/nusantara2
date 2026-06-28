package com.azuratech.azuratime.features.account.domain.model

import com.azuratech.azuratime.features.account.data.local.AccountClassAccessEntity

/**
5:  * 🎓 SUPERVISOR ASSIGNMENT (v3.8.0)
6:  * Domain model mapping from AccountClassAccessEntity.
7:  */
data class SupervisorAssignment(
    val accountId: String,
    val classId: String,
    val subjectId: String,
    val schoolId: String,
    val isActive: Boolean,
    val assignedAt: Long,
)

fun AccountClassAccessEntity.toDomain(): SupervisorAssignment {
    return SupervisorAssignment(
        accountId = this.accountId,
        classId = this.classId,
        subjectId = this.subjectId,
        schoolId = this.schoolId,
        isActive = this.isActive,
        assignedAt = this.assignedAt,
    )
}

fun SupervisorAssignment.toEntity(): AccountClassAccessEntity {
    return AccountClassAccessEntity(
        accountId = this.accountId,
        classId = this.classId,
        subjectId = this.subjectId,
        schoolId = this.schoolId,
        isActive = this.isActive,
        assignedAt = this.assignedAt,
    )
}
