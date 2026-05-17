package com.azuratech.azuraengine.core.domain.id

import kotlin.jvm.JvmInline

@JvmInline
value class AccountId(val value: String) {
    init {
        require(value.isNotBlank()) { "AccountId cannot be blank" }
    }
}

@JvmInline
value class SchoolId(val value: String) {
    init {
        require(value.isNotBlank()) { "SchoolId cannot be blank" }
    }
}

@JvmInline
value class StudentId(val value: String) {
    init {
        require(value.isNotBlank()) { "StudentId cannot be blank" }
    }
}

@JvmInline
value class ClassId(val value: String) {
    init {
        require(value.isNotBlank()) { "ClassId cannot be blank" }
    }
}
