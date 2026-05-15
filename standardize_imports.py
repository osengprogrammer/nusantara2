import os

replacements = {
    # Core Local
    "com.azuratech.azuratime.data.local.AppDatabase": "com.azuratech.azuratime.core.data.local.AppDatabase",
    "com.azuratech.azuratime.data.local.Converters": "com.azuratech.azuratime.core.data.local.Converters",
    "com.azuratech.azuratime.data.local.FaceCache": "com.azuratech.azuratime.core.data.local.FaceCache",
    "com.azuratech.azuratime.data.local.FaceWithDetails": "com.azuratech.azuratime.core.data.local.FaceWithDetails",
    "com.azuratech.azuratime.data.local.ProfileMappers": "com.azuratech.azuratime.core.data.local.ProfileMappers",
    "com.azuratech.azuratime.data.local.RawStudentProfile": "com.azuratech.azuratime.core.data.local.RawStudentProfile",
    "com.azuratech.azuratime.data.local.UserClassAccessDao": "com.azuratech.azuratime.core.data.local.UserClassAccessDao",
    "com.azuratech.azuratime.data.local.UserClassAccessEntity": "com.azuratech.azuratime.core.data.local.UserClassAccessEntity",
    "com.azuratech.azuratime.data.local.DatabaseSeeder": "com.azuratech.azuratime.core.data.local.DatabaseSeeder",
    "com.azuratech.azuratime.data.local.toProfile": "com.azuratech.azuratime.core.data.local.toProfile",
    "com.azuratech.azuratime.data.local.toEntity": "com.azuratech.azuratime.core.data.local.toEntity",

    # Core Repo
    "com.azuratech.azuratime.data.repo.BootRepository": "com.azuratech.azuratime.core.data.repo.BootRepository",
    "com.azuratech.azuratime.data.repo.MainRepository": "com.azuratech.azuratime.core.data.repo.MainRepository",
    "com.azuratech.azuratime.data.repo.SecurityRepository": "com.azuratech.azuratime.core.data.repo.SecurityRepository",
    "com.azuratech.azuratime.data.repo.WorkspaceRepository": "com.azuratech.azuratime.core.data.repo.WorkspaceRepository",
    "com.azuratech.azuratime.data.repo.DataIntegrityRepository": "com.azuratech.azuratime.features.reporting.data.repo.DataIntegrityRepository",

    # Core UI
    "com.azuratech.azuratime.ui.core.UiEvent": "com.azuratech.azuratime.core.ui.UiEvent",
    "com.azuratech.azuratime.ui.util.UiState": "com.azuratech.azuratime.core.ui.util.UiState",
    "com.azuratech.azuratime.ui.theme.": "com.azuratech.azuratime.core.ui.theme.",
    "com.azuratech.azuratime.ui.main.": "com.azuratech.azuratime.core.ui.",
    "com.azuratech.azuratime.ui.core.designsystem.": "com.azuratech.azuratime.core.ui.designsystem.",
    "com.azuratech.azuratime.ui.core.navigation.graphs.": "com.azuratech.azuratime.core.ui.navigation.graphs.",
    "com.azuratech.azuratime.ui.core.preview.": "com.azuratech.azuratime.core.ui.preview.",
    "com.azuratech.azuratime.ui.core.util.": "com.azuratech.azuratime.core.ui.util.",

    # Features - School
    "com.azuratech.azuratime.data.remote.SchoolRemoteDataSource": "com.azuratech.azuratime.features.school.data.remote.SchoolRemoteDataSource",
    "com.azuratech.azuratime.data.repo.SchoolRepository": "com.azuratech.azuratime.features.school.data.repo.SchoolRepository",
    "com.azuratech.azuratime.ui.school.": "com.azuratech.azuratime.features.school.ui.list.",

    # Features - Staff
    "com.azuratech.azuratime.data.repo.AdminRepository": "com.azuratech.azuratime.features.staff.data.repo.AdminRepository",
    "com.azuratech.azuratime.data.repo.StaffAccountRepository": "com.azuratech.azuratime.features.staff.data.repo.StaffAccountRepository",
    "com.azuratech.azuratime.data.repo.AccessRequestRepository": "com.azuratech.azuratime.features.staff.data.repo.AccessRequestRepository",
    "com.azuratech.azuratime.data.repo.MembershipRepository": "com.azuratech.azuratime.features.staff.data.repo.MembershipRepository",
    "com.azuratech.azuratime.ui.membership.": "com.azuratech.azuratime.features.staff.ui.membership.",

    # Features - Auth
    "com.azuratech.azuratime.ui.auth.": "com.azuratech.azuratime.features.auth.ui.",
    "com.azuratech.azuratime.data.repo.AuthRepository": "com.azuratech.azuratime.features.auth.data.repo.AuthRepository",

    # Domain models
    "com.azuratech.azuratime.domain.model.SyncStatus": "com.azuratech.azuratime.core.domain.model.SyncStatus",
    "com.azuratech.azuratime.domain.model.AccessRequestStatus": "com.azuratech.azuratime.features.staff.domain.model.AccessRequestStatus",
    "com.azuratech.azuratime.domain.model.AccessRequestProfile": "com.azuratech.azuratime.features.staff.domain.model.AccessRequestProfile",
    "com.azuratech.azuratime.domain.model.StudentProfile": "com.azuratech.azuratime.features.student.domain.model.StudentProfile",
    "com.azuratech.azuratime.domain.model.BiometricEnrollmentProfile": "com.azuratech.azuratime.features.biometric.domain.model.BiometricEnrollmentProfile",
    "com.azuratech.azuratime.domain.model.AttendanceProfile": "com.azuratech.azuratime.features.attendance.domain.model.AttendanceProfile",
    "com.azuratech.azuratime.domain.media.PhotoStorageUtils": "com.azuratech.azuratime.core.domain.media.PhotoStorageUtils",
    "com.azuratech.azuratime.domain.sync.ExportUtils": "com.azuratech.azuratime.core.domain.sync.ExportUtils",

    # Generic wildcards (be careful with these)
    "com.azuratech.azuratime.data.local.*": "com.azuratech.azuratime.core.data.local.*",
}

for root, dirs, files in os.walk("app/src/main/java"):
    for file in files:
        if file.endswith(".kt"):
            path = os.path.join(root, file)
            with open(path, "r", encoding="utf-8", errors="ignore") as f:
                content = f.read()
            
            new_content = content
            for old, new in replacements.items():
                new_content = new_content.replace(old, new)
            
            if content != new_content:
                print(f"Standardizing imports in {path}")
                with open(path, "w", encoding="utf-8") as f:
                    f.write(new_content)
