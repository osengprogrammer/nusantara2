# 🗂️ NUSANTARA v3.2 — PROJECT FILE INDEX (AI-NATIVE 100%)
⚡ *Status: 100% MVI & Effect-Driven Architecture Standardized.*

## 💾 Room Entities
| File | Package | Responsibility | SSOT Status |
|------|---------|---------------|-------------|
| `AccountClassAccessEntity.kt` | `core.data.local` | Room table schema; maps DB columns to Kotlin properties | ✅ Migrated |
| `AccessRequestEntity.kt` | `features.account.data.local` | Room table schema; maps DB columns to Kotlin properties | ✅ Migrated |
| `AccountEntity.kt` | `features.account.data.local` | Room table schema; maps DB columns to Kotlin properties | ✅ Migrated |
| `AttendanceConflictEntity.kt` | `features.attendance.data.local` | Room table schema; maps DB columns to Kotlin properties | ✅ Migrated |
| `AttendanceRecordEntity.kt` | `features.attendance.data.local` | Room table schema; maps DB columns to Kotlin properties | ✅ Migrated |
| `StudentBiometricEntity.kt` | `features.biometric.data.local` | Room table schema; maps DB columns to Kotlin properties | ✅ Migrated |
| `StudentClassAssignmentEntity.kt` | `features.biometric.data.local` | Room table schema; maps DB columns to Kotlin properties | ✅ Migrated |
| `AuditLogEntity.kt` | `features.reporting.data.local` | Room table schema; maps DB columns to Kotlin properties | ✅ Migrated |
| `ExportJobEntity.kt` | `features.reporting.data.local` | Room table schema; maps DB columns to Kotlin properties | ✅ Migrated |
| `ReportEntity.kt` | `features.reporting.data.local` | Room table schema; maps DB columns to Kotlin properties | ✅ Migrated |
| `ClassEntity.kt` | `features.school.data.local` | Room table schema; maps DB columns to Kotlin properties | ✅ Migrated |
| `SchoolEntity.kt` | `features.school.data.local` | Room table schema; maps DB columns to Kotlin properties | ✅ Migrated |
| `StudentEntity.kt` | `features.student.data.local` | Room table schema; maps DB columns to Kotlin properties | ✅ Migrated |

## 🧠 ViewModels (100% Effect-Driven MVI)
| File | Package | Responsibility | SSOT Status |
|------|---------|---------------|-------------|
| `BootViewModel.kt` | `core.boot` | Exposes reactive state; handles boot logic | ✅ Migrated |
| `MainViewModel.kt` | `core.ui` | Main entry state management | ✅ Migrated |
| `AttendanceViewModel.kt` | `features.attendance.ui` | Unified attendance management; uses `UiEffect` | ✅ Migrated |
| `StudentRosterViewModel.kt` | `features.student.ui.roster` | Roster state management; uses `UiEffect` | ✅ Migrated |
| `StudentFormViewModel.kt` | `features.student.ui.form` | Profile creation/edit logic; uses `UiEffect` | ✅ Migrated |
| `RegisterViewModel.kt` | `features.student.ui.bulk` | Bulk CSV import engine; uses `UiEffect` | ✅ Migrated |
| `DashboardViewModel.kt` | `features.dashboard.ui` | Global hub state; uses `UiEffect` | ✅ Migrated |
| `ZoharAssistantViewModel.kt` | `features.ai.ui` | AI assistant state; uses `UiEffect` | ✅ Migrated |
| `DailyDetailViewModel.kt` | `features.reporting.ui.daily` | Specific day audit logic; uses `UiEffect` | ✅ Migrated |
| `DataIntegrityViewModel.kt` | `features.reporting.ui.integrity` | Health monitoring; uses typed combine | ✅ Migrated |

## ⚡ UI Effects (Transient Event Stream)
| File | Package | Responsibility |
|------|---------|---------------|
| `AttendanceUiEffect.kt` | `features.attendance.ui` | Decouples Toasts, Navigation from State |
| `StudentRosterUiEffect.kt` | `features.student.ui.roster` | Decouples Toasts, Navigation from State |
| `StudentFormUiEffect.kt` | `features.student.ui.form` | Decouples Toasts, Navigation from State |
| `RegisterUiEffect.kt` | `features.student.ui.bulk` | Decouples Toasts, Navigation from State |
| `DailyDetailUiEffect.kt` | `features.reporting.ui.daily` | Decouples Toasts, Navigation from State |
| `ZoharUiEffect.kt` | `features.ai.ui` | Decouples Toasts, Navigation from State |
| `DashboardUiEffect.kt` | `features.dashboard.ui` | Decouples Toasts, Navigation from State |

## 🏰 Repositories (DRY standard)
| File | Package | Responsibility | DRY Status |
|------|---------|---------------|------------|
| `AttendanceRepository.kt` | `features.attendance.domain.repository` | SSOT Guardian; implements CSV Export | ✅ asLocalResult |
| `StudentRepository.kt` | `features.student.domain.repository` | SSOT Guardian; local-first profile sync | ✅ asLocalResult |
| `BiometricRepository.kt` | `features.biometric.domain.repository` | SSOT Guardian; biometric data engine | ✅ asLocalResult |
| `SchoolRepository.kt` | `features.school.domain.repository` | SSOT Guardian; multi-tenant workspace mgmt | ✅ asLocalResult |
| `AccountRepository.kt` | `features.account.domain.repository` | SSOT Guardian; account & membership mgmt | ✅ asLocalResult |
| `AuditLogRepository.kt` | `features.reporting.data.repo` | SSOT Guardian; system traceability | ✅ asLocalResult |

## 🧪 Verification
| File | Package | Responsibility |
|------|---------|---------------|
| `AttendanceViewModelTest.kt` | `features.attendance.ui` | Gold-Standard MVI Contract Test |
| `ArchitectureTest.kt` | `(various)` | Deleted redundant legacy boilerplate |
