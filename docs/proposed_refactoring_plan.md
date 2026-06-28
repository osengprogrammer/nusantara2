# Decoupling & Repository Isolation Refactoring Plan

## 📌 Context & Objective
As part of Phase 49 of the Azura Time codebase evolution, we isolated the UI / Presentation layer (ViewModels) from direct Room database dependencies (`AppDatabase` and specific DAOs). The goal was to align with the **Vertical Slice Architecture** by routing database requests through clean domain interfaces and strictly returning domain models via safe mappings (`toDomain()`).

This document details the refactoring plan implemented for:
1. **Dashboard Module** (Decoupling `DashboardViewModel` from direct `AccountDao` operations).
2. **AssignClass Module** (Decoupling `AssignClassViewModel` from direct `AccountDao` and `AccountClassAccessDao` operations).

---

## 🛠️ Refactoring Specifications

### 1. Dashboard Module Decoupling

#### 📋 [DashboardRepository](file:///home/max/azuratime/nusantara-main/app/src/main/java/com/azuratech/azuratime/features/dashboard/domain/repository/DashboardRepository.kt)
An interface defined in the domain layer to isolate account updates from the UI:
```kotlin
package com.azuratech.azuratime.features.dashboard.domain.repository

import com.azuratech.azuratime.features.account.domain.model.Account

interface DashboardRepository {
    suspend fun updateAccount(account: Account)
}
```

#### 📦 [DashboardRepositoryImpl](file:///home/max/azuratime/nusantara-main/app/src/main/java/com/azuratech/azuratime/features/dashboard/data/repo/DashboardRepositoryImpl.kt)
Concrete implementation mapping the domain models back into Room database Entities safely:
```kotlin
package com.azuratech.azuratime.features.dashboard.data.repo

import com.azuratech.azuratime.features.account.data.local.AccountDao
import com.azuratech.azuratime.features.account.domain.model.Account
import com.azuratech.azuratime.features.dashboard.domain.repository.DashboardRepository
import javax.inject.Inject

class DashboardRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao,
) : DashboardRepository {

    override suspend fun updateAccount(account: Account) {
        val existing = accountDao.getAccountById(account.accountId)
        if (existing != null) {
            val updated = existing.copy(
                email = account.email,
                name = account.name,
                photoUrl = account.photoUrl,
                status = account.status,
                role = account.role.name,
                activeSchoolId = account.activeSchoolId,
                activeClassId = account.activeClassId,
                memberships = account.memberships.mapValues { (_, membership) ->
                    com.azuratech.azuratime.features.account.data.local.SchoolMembership(
                        schoolName = membership.schoolName,
                        role = membership.role,
                        status = membership.status,
                        assignments = membership.assignments,
                    )
                },
                syncStatus = account.syncStatus,
            )
            accountDao.updateAccount(updated)
        }
    }
}
```

#### 💻 ViewModel Integration
[DashboardViewModel](file:///home/max/azuratime/nusantara-main/app/src/main/java/com/azuratech/azuratime/features/dashboard/ui/DashboardViewModel.kt) has been updated to inject `DashboardRepository` and remove the direct `AppDatabase` property dependency for updating account actions.

---

### 2. AssignClass Module Decoupling

#### 📋 [AssignClassRepository](file:///home/max/azuratime/nusantara-main/app/src/main/java/com/azuratech/azuratime/features/account/domain/repository/AssignClassRepository.kt)
Defines operations to query target accounts and observe reactive teacher assignments:
```kotlin
package com.azuratech.azuratime.features.account.domain.repository

import com.azuratech.azuratime.features.account.domain.model.Account
import com.azuratech.azuratime.features.account.domain.model.TeacherAssignment
import kotlinx.coroutines.flow.Flow

interface AssignClassRepository {
    suspend fun getAccountById(id: String): Account?
    fun observeAssignments(accountId: String, schoolId: String): Flow<List<TeacherAssignment>>
}
```

#### 📦 [AssignClassRepositoryImpl](file:///home/max/azuratime/nusantara-main/app/src/main/java/com/azuratech/azuratime/features/account/data/repo/AssignClassRepositoryImpl.kt)
Handles Room queries safely and automatically translates database records to the clean domain layer:
```kotlin
package com.azuratech.azuratime.features.account.data.repo

import com.azuratech.azuratime.features.account.data.local.AccountClassAccessDao
import com.azuratech.azuratime.features.account.data.local.AccountDao
import com.azuratech.azuratime.features.account.data.local.toDomain
import com.azuratech.azuratime.features.account.domain.model.Account
import com.azuratech.azuratime.features.account.domain.model.TeacherAssignment
import com.azuratech.azuratime.features.account.domain.repository.AssignClassRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AssignClassRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao,
    private val accountClassAccessDao: AccountClassAccessDao,
) : AssignClassRepository {

    override suspend fun getAccountById(id: String): Account? {
        return accountDao.getAccountById(id)?.toDomain()
    }

    override fun observeAssignments(accountId: String, schoolId: String): Flow<List<TeacherAssignment>> {
        return accountClassAccessDao.getAssignmentsFlow(accountId, schoolId).map { tuples ->
            tuples.map { tuple ->
                TeacherAssignment(
                    classId = tuple.classId,
                    subjectId = tuple.subjectId.takeIf { it.isNotEmpty() },
                )
            }
        }
    }
}
```

#### 💻 ViewModel Integration
[AssignClassViewModel](file:///home/max/azuratime/nusantara-main/app/src/main/java/com/azuratech/azuratime/features/account/ui/management/AssignClassViewModel.kt) has been updated to remove direct DAO access and utilize `AssignClassRepository` flow mechanisms.

---

## 🔒 Verification & Compliance
- **Compilation Check:** Verified clean compiling with `./gradlew compileDebugKotlin` to ensure no DI graph compilation errors exist.
- **Unit Tests:** Verified unit test suites for view models execute successfully against the new decoupled interface dependencies.
- **Domain Modeling Integrity:** All boundaries enforce mapping using extension helper methods (`toDomain()`) ensuring clean architecture.
