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

## 🧪 Testing Checklist
- [ ] Add student to 2+ classes → verify all appear in UI
- [ ] Remove one class → verify only remaining classes shown (no orphans)
- [ ] Offline: toggle class → verify `CloudOff` icon appears
- [ ] Reinstall after online sync → verify classes restore from Firestore
- [ ] Rapid toggles → verify no duplicate assignments in Room

## ⚠️ Known Limitations & Mitigations
| Limitation | Mitigation |
|------------|-----------|
| Offline uninstall = lost pending assignments | Show `CloudOff` indicator; encourage sync before uninstall |
| Legacy code may use `student.classId` (single) | Use backward-compat getter; mark legacy paths ` @Deprecated` |
| Bulk class changes require multiple UseCase calls | Future: create `UpdateStudentClassesUseCase(list<String>)` for batch ops |

## 🤖 Rules for AI Assistants
1. NEVER suggest direct `FaceAssignmentDao` calls in ViewModels — always go through Repository/UseCase
2. ALWAYS use `StudentProfile.classIds` (list) in domain logic, not legacy `classId`
3. For sync status checks, use `SyncStatus` enum, not boolean flags
4. When suggesting UI changes, ensure reactive Flow patterns are preserved
