# 🏰 Project Architecture

This document serves as the architectural source of truth for the project.

## 🔄 Class Assignment Flow (Multi-Class Support)
- **SSOT**: `StudentProfile.classIds: List<String>` represents all class memberships
- **Persistence**: `FaceAssignmentEntity` stored in separate table with composite key [faceId, classId]
- **Atomic Updates**: `StudentRepository.saveProfile()` uses Clear-then-Insert pattern inside ` @Transaction`
- **UI Interaction**: 
  - `FaceListScreen` → `FaceListViewModel.onToggleStudentClassAssignment()` 
  - Triggers `AssignStudentToClassUseCase` / `RemoveStudentFromClassUseCase`
- **Sync**: Assignments synced independently to `schools/{schoolId}/face_assignments`

## 🔄 Membership/Follow System Flow
- **SSOT**: `UserEntity.memberships` map in Room is the source of truth for UI
- **Request Flow**: `RequestJoinSchoolUseCase` → `UserRepository.updateMembership()` → Room (PENDING_UPDATE) → UI auto-refreshes → `ProfileSyncWorker` pushes to Firestore
- **Approval Flow**: `AdminUseCase` updates Room first → triggers background sync → Firestore updated → other devices sync automatically
- **Offline Behavior**: Requests created offline show `CloudOff` icon; syncs when network available

## 🧪 Testing Checklist (Class Assignment)
- [ ] Add student to 2+ classes → verify all appear in UI
- [ ] Remove one class → verify only remaining classes shown (no orphans)
- [ ] Offline: toggle class → verify `CloudOff` icon appears
- [ ] Reinstall after online sync → verify classes restore from Firestore
- [ ] Rapid toggles → verify no duplicate assignments in Room

## 🧪 Testing Checklist (Membership)
- [ ] Follow school online → verify Firestore update within 30s
- [ ] Follow school offline → verify local save + CloudOff icon → reconnect → verify auto-sync
- [ ] Admin approves request offline → verify local update → reconnect → verify push to Firestore
- [ ] Rapid follow/unfollow → verify no duplicate entries in Room or Firestore
- [ ] Multi-device test: Follow on Device A → verify appears on Device B after sync

## ⚠️ Known Limitations & Mitigations
| Limitation | Mitigation |
|------------|-----------|
| Offline uninstall = lost pending assignments | Show `CloudOff` indicator; encourage sync before uninstall |
| Legacy code may use `student.classId` (single) | Use backward-compat getter; mark legacy paths ` @Deprecated` |
| Bulk class changes require multiple UseCase calls | Future: create `UpdateStudentClassesUseCase(list<String>)` for batch ops |

### Phase 7.1: Complete SSOT Migration (Remaining .await() Calls)

| File | Lines | Priority | Migration Pattern |
|------|-------|----------|------------------|
| MembershipRepository.kt | 39,42,47,50,67,150,153 | Medium | Read user status from Room → trigger ProfileSyncWorker if stale |
| AuthRepository.kt | 38,44,49,54,87,127,138 | Low | Auth boundary: Firebase Auth .await() acceptable; Firestore reads can be deferred to Worker |
| StudentRepositoryLegacyImpl.kt | 39 | Delete | Remove legacy stub in v4.0 |

## 🤖 Rules for AI Assistants
1. NEVER suggest direct `FaceAssignmentDao` calls in ViewModels — always go through Repository/UseCase
2. ALWAYS use `StudentProfile.classIds` (list) in domain logic, not legacy `classId`
3. For sync status checks, use `SyncStatus` enum, not boolean flags
4. When suggesting UI changes, ensure reactive Flow patterns are preserved
5. Membership changes MUST go through `RequestJoinSchoolUseCase` / `CancelJoinRequestUseCase` — NEVER direct Firestore writes in ViewModels
6. `ProfileSyncWorker` is the ONLY component that writes memberships to Firestore — all other code treats remote sync as a side-effect
7. Use `MembershipStatus` enum for request state, `SyncStatus` enum for sync state — never raw strings or booleans
8. When suggesting sync logic, always use WorkManager with `ExistingWorkPolicy.REPLACE` to prevent duplicate jobs
