# 🗂️ NUSANTARA v3.1 — PROJECT FILE INDEX
⚡ *Auto-generated. Do not edit manually. Run `bash ~/gen_project_index.sh` to refresh.*

## 💾 Room Entities
| File | Package | Responsibility | SSOT Status |
|------|---------|---------------|-------------|
| `AccountClassAccessEntity.kt` | `core.data.local` | Room table schema; maps DB columns to Kotlin properties | ✅ Migrated |
| `AccessRequestEntity.kt` | `features.account.data.local` | Room table schema; maps DB columns to Kotlin properties | ✅ Migrated |
| `AccountEntity.kt" | `features.account.data.local` | Room table schema; maps DB columns to Kotlin properties | ✅ Migrated |
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

## 🧠 ViewModels
| File | Package | Responsibility | SSOT Status |
|------|---------|---------------|-------------|
| `BootViewModel.kt` | `core.boot` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `MainViewModel.kt` | `core.ui` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `SyncViewModel.kt` | `core.ui.sync` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `AdminViewModel.kt` | `features.account.ui.components` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `NetworkViewModel.kt` | `features.account.ui.components` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `WorkspaceViewModel.kt` | `features.account.ui.components` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `AccountManagementViewModel.kt` | `features.account.ui.management` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `MembershipViewModel.kt` | `features.account.ui.membership` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `ZoharAssistantViewModel.kt` | `features.ai.ui` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `AttendanceViewModel.kt` | `features.attendance.ui.capture` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `ScannerViewModel.kt` | `features.attendance.ui.components` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `AuthViewModel.kt` | `features.auth.ui` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `StudentAssignmentViewModel.kt` | `features.biometric.ui.assignment` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `BiometricViewModel.kt` | `features.biometric.ui.enroll` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `StudentBiometricViewModel.kt` | `features.biometric.ui.enroll` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `DashboardViewModel.kt` | `features.dashboard.ui` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `AuditLogViewModel.kt` | `features.reporting.ui.audit` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `DailyDetailViewModel.kt` | `features.reporting.ui.daily` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `ExportViewModel.kt` | `features.reporting.ui.export` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `DataIntegrityViewModel.kt` | `features.reporting.ui.integrity` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `AttendanceMatrixViewModel.kt" | `features.reporting.ui.matrix` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `ReportViewModel.kt` | `features.reporting.ui` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `PendingSchoolsViewModel.kt` | `features.school.ui.admin` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `ClassViewModel.kt` | `features.school.ui.classes` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `SchoolViewModel.kt` | `features.school.ui.list` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `RegisterViewModel.kt` | `features.student.ui.bulk` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `StudentFormViewModel.kt` | `features.student.ui.form` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `StudentRosterViewModel.kt` | `features.student.ui.roster` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |

## 🔗 UseCases (Legacy/Active)
| File | Package | Responsibility | SSOT Status |
|------|---------|---------------|-------------|

## 🏰 Repositories
| File | Package | Responsibility | SSOT Status |
|------|---------|---------------|-------------|
| `BootRepository.kt` | `core.domain.repository` | Mediates between ViewModel and data sources; enforces local-first logic | ✅ Migrated |
| `MainRepository.kt` | `core.domain.repository` | Mediates between ViewModel and data sources; enforces local-first logic | ✅ Migrated |
| `SecurityRepository.kt` | `core.domain.repository` | Mediates between ViewModel and data sources; enforces local-first logic | ✅ Migrated |
| `SyncRepository.kt` | `core.domain.repository` | Mediates between ViewModel and data sources; enforces local-first logic | ✅ Migrated |
| `AccountRepository.kt` | `features.account.domain.repository` | Mediates between ViewModel and data sources; enforces local-first logic | ✅ Migrated |
| `MembershipRepository.kt` | `features.account.domain.repository` | Mediates between ViewModel and data sources; enforces local-first logic | ✅ Migrated |
| `SchoolWorkspaceRepository.kt` | `features.account.domain.repository` | Mediates between ViewModel and data sources; enforces local-first logic | ✅ Migrated |
| `AccessRequestRepository.kt` | `features.account.domain.repository` | Mediates between ViewModel and data sources; enforces local-first logic | ✅ Migrated |
| `ZoharRepository.kt` | `features.ai.data.repo` | Mediates between ViewModel and data sources; enforces local-first logic | ✅ Migrated |
| `BiometricScannerRepository.kt` | `features.attendance.domain.repository` | Mediates between ViewModel and data sources; enforces local-first logic | ✅ Migrated |
| `AttendanceRepository.kt` | `features.attendance.domain.repository` | Mediates between ViewModel and data sources; enforces local-first logic | ✅ Migrated |
| `AuthRepository.kt` | `features.auth.domain.repository` | Mediates between ViewModel and data sources; enforces local-first logic | ✅ Migrated |
| `StudentBiometricRepository.kt` | `features.biometric.domain.repository` | Mediates between ViewModel and data sources; enforces local-first logic | ✅ Migrated |
| `AuditLogRepository.kt` | `features.reporting.data.repo` | Mediates between ViewModel and data sources; enforces local-first logic | ✅ Migrated |
| `DataIntegrityRepository.kt` | `features.reporting.data.repo` | Mediates between ViewModel and data sources; enforces local-first logic | ✅ Migrated |
| `ExportRepository.kt` | `features.reporting.data.repo` | Mediates between ViewModel and data sources; enforces local-first logic | ✅ Migrated |
| `ReportRepository.kt` | `features.reporting.data.repo` | Mediates between ViewModel and data sources; enforces local-first logic | ✅ Migrated |
| `SchoolRepository.kt` | `features.school.domain.repository` | Mediates between ViewModel and data sources; enforces local-first logic | ✅ Migrated |
| `StudentRegistrationRepository.kt` | `features.student.domain.repository` | Mediates between ViewModel and data sources; enforces local-first logic | ✅ Migrated |
| `StudentRepository.kt` | `features.student.domain.repository` | Mediates between ViewModel and data sources; enforces local-first logic | ✅ Migrated |

## 🧩 Managers
| File | Package | Responsibility | SSOT Status |
|------|---------|---------------|-------------|
| `PhotoManager.kt` | `core.domain.media` | Cross-cutting concern: session, sync orchestration, media handling | ✅ Migrated |
| `SessionManager.kt` | `core.session` | Cross-cutting concern: session, sync orchestration, media handling | ✅ Migrated |
| `SyncManager.kt` | `core.sync` | Cross-cutting concern: session, sync orchestration, media handling | ✅ Migrated |

## 📦 Domain Models
| File | Package | Responsibility | SSOT Status |
|------|---------|---------------|-------------|
| `RawStudentProfile.kt` | `core.data.local` | Domain model for UI; decouples presentation from persistence | ✅ Migrated |
| `AccessRequestProfile.kt` | `features.account.domain.model` | Domain model for UI; decouples presentation from persistence | ✅ Migrated |
| `AttendanceProfile.kt` | `features.attendance.domain.model` | Domain model for UI; decouples presentation from persistence | ✅ Migrated |
| `AuthProfile.kt` | `features.auth.domain.model` | Domain model for UI; decouples presentation from persistence | ✅ Migrated |
| `BiometricEnrollmentProfile.kt` | `features.biometric.domain.model` | Domain model for UI; decouples presentation from persistence | ✅ Migrated |
| `ExportJobProfile.kt` | `features.reporting.domain.model` | Domain model for UI; decouples presentation from persistence | ✅ Migrated |
| `StudentProfile.kt` | `features.student.domain.model` | Domain model for UI; decouples presentation from persistence | ✅ Migrated |

## ⚙️ Workers
| File | Package | Responsibility | SSOT Status |
|------|---------|---------------|-------------|
| `AccessSyncWorker.kt` | `core.sync` | Background job logic; handles sync, retry, WorkManager integration | ✅ Migrated |
| `ProfileSyncWorker.kt" | `core.sync` | Background job logic; handles sync, retry, WorkManager integration | ✅ Migrated |
| `SchoolSyncWorker.kt` | `core.sync` | Background job logic; handles sync, retry, WorkManager integration | ✅ Migrated |
| `SyncWorker.kt` | `core.sync` | Background job logic; handles sync, retry, WorkManager integration | ✅ Migrated |

## 🖥️ Screens
| File | Package | Responsibility | SSOT Status |
|------|---------|---------------|-------------|
| `Screen.kt` | `core.navigation` | Composable UI; collects StateFlow, renders state, handles user input | ✅ Migrated |
| `AzuraScreen.kt` | `core.ui.designsystem` | Composable UI; collects StateFlow, renders state, handles user input | ✅ Migrated |
| `MainScreen.kt` | `core.ui` | Composable UI; collects StateFlow, renders state, handles user input | ✅ Migrated |
| `RootScreen.kt` | `core.ui` | Composable UI; collects StateFlow, renders state, handles user input | ✅ Migrated |
| `FindSchoolScreen.kt` | `features.account.ui.components` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `MyAssignedClassScreen.kt` | `features.account.ui.components` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `NetworkScreen.kt` | `features.account.ui.components` | Composable UI; collects StateFlow, renders state, handles user input | ✅ Migrated |
| `DebugScreen.kt` | `features.account.ui.debug` | Composable UI; collects StateFlow, renders state, handles user input | ✅ Migrated |
| `MembershipScreen.kt` | `features.account.ui.membership` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `AccountProfileScreen.kt` | `features.account.ui.profile` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `BarcodeScreen.kt` | `features.attendance.ui.barcode` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `AttendanceCaptureScreen.kt` | `features.attendance.ui.capture` | Composable UI; collects StateFlow, renders state, handles user input | ✅ Migrated |
| `AttendanceHistoryScreen.kt` | `features.attendance.ui.history` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `ManualAttendanceScreen.kt` | `features.attendance.ui.manual` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `LoginScreen.kt` | `features.auth.ui` | Composable UI; collects StateFlow, renders state, handles user input | ✅ Migrated |
| `BiometricScreen.kt` | `features.biometric.ui.enroll` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `DashboardScreen.kt` | `features.dashboard.ui` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `AuditLogScreen.kt` | `features.reporting.ui.audit` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `DailyDetailScreen.kt` | `features.reporting.ui.daily` | Composable UI; collects StateFlow, renders state, handles user input | ✅ Migrated |
| `ExportScreen.kt` | `features.reporting.ui.export` | Composable UI; collects StateFlow, renders state, handles user input | ✅ Migrated |
| `DataIntegrityScreen.kt` | `features.reporting.ui.integrity` | Composable UI; collects StateFlow, renders state, handles user input | ✅ Migrated |
| `DataManagementScreen.kt` | `features.reporting.ui.integrity` | Composable UI; collects StateFlow, renders state, handles user input | ✅ Migrated |
| `AttendanceMatrixScreen.kt` | `features.reporting.ui.matrix` | Composable UI; collects StateFlow, renders state, handles user input | ✅ Migrated |
| `ReportScreen.kt` | `features.reporting.ui` | Composable UI; collects StateFlow, renders state, handles user input | ✅ Migrated |
| `PendingSchoolsScreen.kt` | `features.school.ui.admin` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `ClassDetailScreen.kt` | `features.school.ui.classes` | Composable UI; collects StateFlow, renders state, handles user input | ✅ Migrated |
| `ClassListScreen.kt` | `features.school.ui.classes` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `ClassManagementScreen.kt` | `features.school.ui.classes` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `SchoolListScreen.kt` | `features.school.ui.list` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `BulkRegistrationScreen.kt` | `features.student.ui.bulk` | Composable UI; collects StateFlow, renders state, handles user input | ✅ Migrated |
| `AddStudentScreen.kt` | `features.student.ui.form` | Composable UI; collects StateFlow, renders state, handles user input | ✅ Migrated |
| `EditStudentScreen.kt` | `features.student.ui.form` | Composable UI; collects StateFlow, renders state, handles user input | ✅ Migrated |
| `RegistrationMenuScreen.kt" | `features.student.ui` | Composable UI; collects StateFlow, renders state, handles user input | ✅ Migrated |
| `StudentRosterBarcodeScreen.kt` | `features.student.ui.roster` | Composable UI; collects StateFlow, renders state, handles user input | ✅ Migrated |
| `StudentRosterScreen.kt` | `features.student.ui.roster` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |

## 🗄️ DAOs
| File | Package | Responsibility | SSOT Status |
|------|---------|---------------|-------------|
| `AccountClassAccessDao.kt` | `core.data.local` | Room database access layer; defines SQL queries | ✅ Migrated |
| `AccessRequestDao.kt` | `features.account.data.local` | Room database access layer; defines SQL queries | ✅ Migrated |
| `AccountDao.kt` | `features.account.data.local` | Room database access layer; defines SQL queries | ✅ Migrated |
| `AttendanceConflictDao.kt` | `features.attendance.data.local` | Room database access layer; defines SQL queries | ✅ Migrated |
| `AttendanceRecordDao.kt` | `features.attendance.data.local` | Room database access layer; defines SQL queries | ✅ Migrated |
| `StudentBiometricDao.kt` | `features.biometric.data.local` | Room database access layer; defines SQL queries | ✅ Migrated |
| `StudentClassAssignmentDao.kt` | `features.biometric.data.local` | Room database access layer; defines SQL queries | ✅ Migrated |
| `AuditLogDao.kt` | `features.reporting.data.local` | Room database access layer; defines SQL queries | ✅ Migrated |
| `ExportJobDao.kt` | `features.reporting.data.local` | Room database access layer; defines SQL queries | ✅ Migrated |
| `ReportDao.kt` | `features.reporting.data.local` | Room database access layer; defines SQL queries | ✅ Migrated |
| `ClassDao.kt` | `features.school.data.local` | Room database access layer; defines SQL queries | ✅ Migrated |
| `SchoolClassDao.kt` | `features.school.data.local` | Room database access layer; defines SQL queries | ✅ Migrated |
| `SchoolDao.kt` | `features.school.data.local` | Room database access layer; defines SQL queries | ✅ Migrated |
| `StudentDao.kt` | `features.student.data.local` | Room database access layer; defines SQL queries | ✅ Migrated |
