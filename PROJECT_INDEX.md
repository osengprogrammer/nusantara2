# 🗂️ NUSANTARA v3.1 — PROJECT FILE INDEX
⚡ *Auto-generated. Do not edit manually. Run `bash ~/gen_project_index.sh` to refresh.*

## 💾 Room Entities
| File | Package | Responsibility | SSOT Status |
|------|---------|---------------|-------------|
| `AccessRequestEntity.kt` | `data.local` | Room table schema; maps DB columns to Kotlin properties | ✅ Migrated |
| `AttendanceConflictEntity.kt` | `data.local` | Room table schema; maps DB columns to Kotlin properties | ✅ Migrated |
| `AttendanceEntity.kt` | `data.local` | Room table schema; maps DB columns to Kotlin properties | ✅ Migrated |
| `AuditLogEntity.kt` | `data.local` | Room table schema; maps DB columns to Kotlin properties | ✅ Migrated |
| `CheckInRecordEntity.kt` | `data.local` | Room table schema; maps DB columns to Kotlin properties | ✅ Migrated |
| `ClassEntity.kt` | `data.local` | Room table schema; maps DB columns to Kotlin properties | ✅ Migrated |
| `ExportJobEntity.kt` | `data.local` | Room table schema; maps DB columns to Kotlin properties | ✅ Migrated |
| `FaceAssignmentEntity.kt` | `data.local` | Room table schema; maps DB columns to Kotlin properties | ✅ Migrated |
| `FaceEntity.kt` | `data.local` | Room table schema; maps DB columns to Kotlin properties | ✅ Migrated |
| `ReportEntity.kt` | `data.local` | Room table schema; maps DB columns to Kotlin properties | ✅ Migrated |
| `SchoolEntity.kt` | `data.local` | Room table schema; maps DB columns to Kotlin properties | ✅ Migrated |
| `StudentEntity.kt` | `data.local` | Room table schema; maps DB columns to Kotlin properties | ✅ Migrated |
| `UserClassAccessEntity.kt` | `data.local` | Room table schema; maps DB columns to Kotlin properties | ✅ Migrated |
| `UserEntity.kt` | `data.local` | Room table schema; maps DB columns to Kotlin properties | ✅ Migrated |

## 🧠 ViewModels
| File | Package | Responsibility | SSOT Status |
|------|---------|---------------|-------------|
| `BootViewModel.kt` | `core.boot` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `FaceListViewModel.kt` | `ui.add` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `FaceViewModel.kt` | `ui.add` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `RegisterViewModel.kt` | `ui.add` | Exposes reactive UI state via StateFlow; handles user actions | ⚠️ Legacy |
| `StudentFormViewModel.kt` | `ui.add` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `PendingSchoolsViewModel.kt` | `ui.admin` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `ZoharAssistantViewModel.kt` | `ui.ai` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `AttendanceMatrixViewModel.kt` | `ui.attendance` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `AuditLogViewModel.kt` | `ui.audit` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `AuthViewModel.kt` | `ui.auth` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `BiometricViewModel.kt` | `ui.biometric` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `CheckInViewModel.kt` | `ui.checkin` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `ScannerViewModel.kt` | `ui.checkin` | Exposes reactive UI state via StateFlow; handles user actions | ⚠️ Legacy |
| `ClassViewModel.kt` | `ui.classes` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `FaceAssignmentViewModel.kt` | `ui.classes` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `DashboardViewModel.kt` | `ui.dashboard` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `DataIntegrityViewModel.kt` | `ui.data` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `ExportViewModel.kt` | `ui.export` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `MainViewModel.kt` | `ui.main` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `MembershipViewModel.kt` | `ui.membership` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `DailyDetailViewModel.kt` | `ui.report` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `ReportViewModel.kt` | `ui.report` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `SchoolViewModel.kt` | `ui.school` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `SyncViewModel.kt` | `ui.sync` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `AdminViewModel.kt` | `ui.user` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `NetworkViewModel.kt` | `ui.user` | Exposes reactive UI state via StateFlow; handles user actions | ⚠️ Legacy |
| `UserManagementViewModel.kt` | `ui.user` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |
| `WorkspaceViewModel.kt` | `ui.user` | Exposes reactive UI state via StateFlow; handles user actions | ✅ Migrated |

## 🔗 UseCases (Legacy/Active)
| File | Package | Responsibility | SSOT Status |
|------|---------|---------------|-------------|
| `AdminUseCase.kt` | `domain.admin.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `AssignStudentToClassUseCase.kt` | `domain.assignment.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `RemoveStudentFromClassUseCase.kt` | `domain.assignment.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `SyncAssignmentsUseCase.kt` | `domain.assignment.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `ProcessCheckInUseCase.kt` | `domain.checkin.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `ResolveConflictUseCase.kt` | `domain.checkin.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `SyncCheckInRecordsUseCase.kt` | `domain.checkin.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `AssignClassToSchoolUseCase.kt` | `domain.classes.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `BackfillOrphanedClassesUseCase.kt` | `domain.classes.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `ImportClassesUseCase.kt` | `domain.classes.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `SyncClassesUseCase.kt` | `domain.classes.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `UnassignClassFromSchoolUseCase.kt` | `domain.classes.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `DeleteFaceUseCase.kt` | `domain.face.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `GetAssignmentsForFaceUseCase.kt` | `domain.face.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `GetEnrolledFacesUseCase.kt` | `domain.face.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `GetFacesInClassUseCase.kt` | `domain.face.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `GetFacesWithDetailsUseCase.kt` | `domain.face.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `GetFaceWithDetailsUseCase.kt` | `domain.face.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `SyncFacesUseCase.kt` | `domain.face.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `UpdateEmployeeClassUseCase.kt` | `domain.face.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `GetReportDataUseCase.kt` | `domain.report.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `ArchiveInactiveSchoolsUseCase.kt` | `domain.school.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `GetActiveSchoolContextUseCase.kt` | `domain.school.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `SuperAdminApprovalUseCase.kt` | `domain.school.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `SyncSchoolsUseCase.kt` | `domain.school.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `BackfillStudentsFromFacesUseCase.kt` | `domain.student.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `DeleteStudentProfileUseCase.kt` | `domain.student.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `DeleteStudentUseCase.kt` | `domain.student.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `GetStudentProfilesUseCase.kt` | `domain.student.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `PushStudentsUseCase.kt` | `domain.student.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `SaveStudentProfileUseCase.kt` | `domain.student.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `UpdateStudentClassUseCase.kt` | `domain.student.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `GetLocalDataCountUseCase.kt` | `domain.sync.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `ProcessCsvUseCase.kt` | `domain.sync.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `SyncMasterDataUseCase.kt` | `domain.sync.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `CancelJoinRequestUseCase.kt` | `domain.user.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `GetUserByIdUseCase.kt` | `domain.user.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `ObserveUserUseCase.kt` | `domain.user.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `RequestJoinSchoolUseCase.kt` | `domain.user.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `SyncUserUseCase.kt` | `domain.user.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `UpdateUserUseCase.kt` | `domain.user.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |
| `UserManagementUseCase.kt` | `domain.user.usecase` | [LEGACY] Business logic wrapper; being phased out for direct repo calls | 🗑️ Deprecated (if unused) |

## 🏰 Repositories
| File | Package | Responsibility | SSOT Status |
|------|---------|---------------|-------------|
| `AccessRequestRepository.kt` | `data.repo` | Mediates between ViewModel and data sources; enforces local-first logic | ✅ Migrated |
| `AdminRepository.kt` | `data.repo` | Mediates between ViewModel and data sources; enforces local-first logic | ⚠️ Legacy |
| `AttendanceRepository.kt` | `data.repo` | Mediates between ViewModel and data sources; enforces local-first logic | ✅ Migrated |
| `AuditLogRepository.kt` | `data.repo` | Mediates between ViewModel and data sources; enforces local-first logic | ✅ Migrated |
| `AuthRepository.kt` | `data.repo` | Mediates between ViewModel and data sources; enforces local-first logic | ⚠️ Legacy |
| `BootRepository.kt` | `repository` | Mediates between ViewModel and data sources; enforces local-first logic | ✅ Migrated |
| `DataIntegrityRepository.kt` | `data.repo` | Mediates between ViewModel and data sources; enforces local-first logic | ✅ Migrated |
| `ExportRepository.kt` | `data.repo` | Mediates between ViewModel and data sources; enforces local-first logic | ✅ Migrated |
| `FaceRepository.kt` | `data.repo` | Mediates between ViewModel and data sources; enforces local-first logic | ✅ Migrated |
| `MainRepository.kt` | `data.repo` | Mediates between ViewModel and data sources; enforces local-first logic | ⚠️ Legacy |
| `MembershipRepository.kt` | `data.repo` | Mediates between ViewModel and data sources; enforces local-first logic | ⚠️ Legacy |
| `RegistrationRepository.kt` | `data.repo` | Mediates between ViewModel and data sources; enforces local-first logic | ⚠️ Legacy |
| `ReportRepository.kt` | `data.repo` | Mediates between ViewModel and data sources; enforces local-first logic | ✅ Migrated |
| `ScannerRepository.kt` | `data.repo` | * Handles real-time face matching and attendance stamping. | ⚠️ Legacy |
| `SchoolRepository.kt` | `data.repo` | Mediates between ViewModel and data sources; enforces local-first logic | ✅ Migrated |
| `SecurityRepository.kt` | `data.repository` | Mediates between ViewModel and data sources; enforces local-first logic | ✅ Migrated |
| `SyncRepository.kt` | `data.repo` | Mediates between ViewModel and data sources; enforces local-first logic | ✅ Migrated |
| `UserRepository.kt` | `data.repo` | Mediates between ViewModel and data sources; enforces local-first logic | ✅ Migrated |
| `WorkspaceRepository.kt` | `data.repo` | Mediates between ViewModel and data sources; enforces local-first logic | ⚠️ Legacy |
| `ZoharRepository.kt` | `data.repo` | Mediates between ViewModel and data sources; enforces local-first logic | ✅ Migrated |
| `CheckInRepository.kt` | `domain.checkin.repository` | Mediates between ViewModel and data sources; enforces local-first logic | ✅ Migrated |
| `StudentRepository.kt` | `domain.student.repository` | Mediates between ViewModel and data sources; enforces local-first logic | ✅ Migrated |

## 🧩 Managers
| File | Package | Responsibility | SSOT Status |
|------|---------|---------------|-------------|
| `SessionManager.kt` | `core.session` | Cross-cutting concern: session, sync orchestration, media handling | ⚠️ Legacy |
| `SyncManager.kt` | `core.sync` | Cross-cutting concern: session, sync orchestration, media handling | ✅ Migrated |
| `PhotoManager.kt` | `domain.media` | Cross-cutting concern: session, sync orchestration, media handling | ✅ Migrated |

## 📦 Domain Models
| File | Package | Responsibility | SSOT Status |
|------|---------|---------------|-------------|
| `RawStudentProfile.kt` | `data.local` | Domain model for UI; decouples presentation from persistence | ✅ Migrated |
| `AccessRequestProfile.kt` | `domain.model` | Domain model for UI; decouples presentation from persistence | ✅ Migrated |
| `AttendanceProfile.kt` | `domain.model` | Domain model for UI; decouples presentation from persistence | ✅ Migrated |
| `AuditLogProfile.kt` | `domain.model` | Domain model for UI; decouples presentation from persistence | ✅ Migrated |
| `ExportJobProfile.kt` | `domain.model` | Domain model for UI; decouples presentation from persistence | ✅ Migrated |
| `FaceEnrollmentProfile.kt` | `domain.model` | Domain model for UI; decouples presentation from persistence | ✅ Migrated |
| `ReportSummaryProfile.kt` | `domain.model` | Domain model for UI; decouples presentation from persistence | ✅ Migrated |
| `StudentProfile.kt` | `domain.model` | Domain model for UI; decouples presentation from persistence | ⚠️ Legacy |

## ⚙️ Workers
| File | Package | Responsibility | SSOT Status |
|------|---------|---------------|-------------|
| `AccessSyncWorker.kt` | `core.sync` | Background job logic; handles sync, retry, WorkManager integration | ⚠️ Legacy |
| `ProfileSyncWorker.kt` | `core.sync` | Background job logic; handles sync, retry, WorkManager integration | ⚠️ Legacy |
| `SchoolSyncWorker.kt` | `core.sync` | Background job logic; handles sync, retry, WorkManager integration | ⚠️ Legacy |
| `SyncWorker.kt` | `core.sync` | Background job logic; handles sync, retry, WorkManager integration | ⚠️ Legacy |

## 🖥️ Screens
| File | Package | Responsibility | SSOT Status |
|------|---------|---------------|-------------|
| `AddUserScreen.kt` | `ui.add` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `BulkRegistrationScreen.kt` | `ui.add` | Composable UI; collects StateFlow, renders state, handles user input | ✅ Migrated |
| `EditUserScreen.kt` | `ui.add` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `FaceCaptureScreen.kt` | `ui.add` | Composable UI; collects StateFlow, renders state, handles user input | ✅ Migrated |
| `FaceListBarcodeScreen.kt` | `ui.add` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `FaceListScreen.kt` | `ui.add` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `RegistrationMenuScreen.kt` | `ui.add` | Composable UI; collects StateFlow, renders state, handles user input | ✅ Migrated |
| `PendingSchoolsScreen.kt` | `ui.admin` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `AttendanceMatrixScreen.kt` | `ui.attendance` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `AuditLogScreen.kt` | `ui.audit` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `LoginScreen.kt` | `ui.auth` | Composable UI; collects StateFlow, renders state, handles user input | ✅ Migrated |
| `BiometricScreen.kt` | `ui.biometric` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `BarcodeScreen.kt` | `ui.checkin` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `CheckInRecordScreen.kt` | `ui.checkin` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `CheckInScreen.kt` | `ui.checkin` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `ManualAttendanceScreen.kt` | `ui.checkin` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `ClassDetailScreen.kt` | `ui.classes` | Composable UI; collects StateFlow, renders state, handles user input | ✅ Migrated |
| `ClassListScreen.kt` | `ui.classes` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `ClassManagementScreen.kt` | `ui.classes` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `AzuraScreen.kt` | `ui.core.designsystem` | Composable UI; collects StateFlow, renders state, handles user input | ✅ Migrated |
| `DashboardScreen.kt` | `ui.dashboard` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `DataIntegrityScreen.kt` | `ui.data` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `DataManagementScreen.kt` | `ui.data` | Composable UI; collects StateFlow, renders state, handles user input | ✅ Migrated |
| `DebugScreen.kt` | `ui.debug` | Composable UI; collects StateFlow, renders state, handles user input | ✅ Migrated |
| `ExportScreen.kt` | `ui.export` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `MainScreen.kt` | `ui.main` | Composable UI; collects StateFlow, renders state, handles user input | ✅ Migrated |
| `MembershipScreen.kt` | `ui.membership` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `DailyDetailScreen.kt` | `ui.report` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `ReportScreen.kt` | `ui.report` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `RootScreen.kt` | `ui.root` | Composable UI; collects StateFlow, renders state, handles user input | ✅ Migrated |
| `SchoolListScreen.kt` | `ui.school` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `FindSchoolScreen.kt` | `ui.user` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `MyAssignedClassScreen.kt` | `ui.user` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `NetworkScreen.kt` | `ui.user` | Composable UI; collects StateFlow, renders state, handles user input | ✅ UI Bound |
| `UserProfileScreen.kt` | `ui.user` | Composable UI; collects StateFlow, renders state, handles user input | ✅ Migrated |

## 🗄️ DAOs
| File | Package | Responsibility | SSOT Status |
|------|---------|---------------|-------------|
| `AccessRequestDao.kt` | `data.local` | Room database access layer; defines SQL queries | ✅ Migrated |
| `AuditLogDao.kt` | `data.local` | Room database access layer; defines SQL queries | ✅ Migrated |
| `CheckInRecordDao.kt` | `data.local` | Room database access layer; defines SQL queries | ✅ Migrated |
| `ClassDao.kt` | `data.local` | Room database access layer; defines SQL queries | ✅ Migrated |
| `ExportJobDao.kt` | `data.local` | Room database access layer; defines SQL queries | ✅ Migrated |
| `FaceAssignmentDao.kt` | `data.local` | Room database access layer; defines SQL queries | ✅ Migrated |
| `FaceDao.kt` | `data.local` | Room database access layer; defines SQL queries | ✅ Migrated |
| `ReportDao.kt` | `data.local` | Room database access layer; defines SQL queries | ✅ Migrated |
| `SchoolClassDao.kt` | `data.local` | Room database access layer; defines SQL queries | ✅ Migrated |
| `SchoolDao.kt` | `data.local` | Room database access layer; defines SQL queries | ✅ Migrated |
| `StudentDao.kt` | `data.local` | Room database access layer; defines SQL queries | ✅ Migrated |
| `UserClassAccessDao.kt` | `data.local` | Room database access layer; defines SQL queries | ✅ Migrated |
| `UserDao.kt` | `data.local` | Room database access layer; defines SQL queries | ✅ Migrated |

