import os
import subprocess

def migrate(old_path, new_path, old_pkg, new_path_pkg):
    try:
        content = subprocess.check_output(["git", "show", f"HEAD:{old_path}"], stderr=subprocess.STDOUT).decode("utf-8")
        content = "".join(i for i in content if ord(i) < 128)
        
        new_content = content.replace(f"package {old_pkg}", f"package {new_path_pkg}")
        
        # Global Renames (CheckIn -> Attendance)
        replacements = {
            "CheckInRepository": "AttendanceRepository",
            "CheckInUiState": "AttendanceUiState",
            "CheckInResult": "AttendanceResult",
            "CheckInRecord": "AttendanceRecord",
            "CheckInStatus": "AttendanceStatus",
            "CheckInSideEffect": "AttendanceSideEffect",
            "ScannerRepository": "AttendanceRepository",
            "ScannerViewModel": "AttendanceScannerViewModel",
            "checkInRecordDao": "attendanceRecordDao",
            "com.azuratech.azuratime.data.local": "com.azuratech.azuratime.core.data.local",
            "com.azuratech.azuratime.data.repo": "com.azuratech.azuratime.core.data.repo",
            "com.azuratech.azuratime.data.core": "com.azuratech.azuratime.core.data",
            "com.azuratech.azuratime.domain.model": "com.azuratech.azuratime.core.domain.model",
            "com.azuratech.azuratime.domain.media": "com.azuratech.azuratime.core.domain.media",
            "com.azuratech.azuratime.domain.sync": "com.azuratech.azuratime.core.domain.sync",
            "com.azuratech.azuratime.ui.core.designsystem": "com.azuratech.azuratime.core.ui.designsystem",
            "com.azuratech.azuratime.ui.theme": "com.azuratech.azuratime.core.ui.theme",
            "com.azuratech.azuratime.ui.util": "com.azuratech.azuratime.core.ui.util",
            "com.azuratech.azuratime.ui.core.navigation.graphs": "com.azuratech.azuratime.core.ui.navigation.graphs",
            "com.azuratech.azuratime.ui.core.preview": "com.azuratech.azuratime.core.ui.preview",
            "com.azuratech.azuratime.ui.core.util": "com.azuratech.azuratime.core.ui.util",
            "com.azuratech.azuratime.ui.dashboard": "com.azuratech.azuratime.features.dashboard.ui",
        }
        
        for old, new in replacements.items():
            new_content = new_content.replace(old, new)
            
        with open(new_path, "w", encoding="utf-8") as f:
            f.write(new_content)
        print(f"Migrated {old_path} -> {new_path}")
    except Exception as e:
        print(f"Failed to migrate {old_path}: {e}")

mappings = [
    # Core Local
    ("app/src/main/java/com/azuratech/azuratime/data/local/AppDatabase.kt", "app/src/main/java/com/azuratech/azuratime/core/data/local/AppDatabase.kt", "com.azuratech.azuratime.data.local", "com.azuratech.azuratime.core.data.local"),
    ("app/src/main/java/com/azuratech/azuratime/data/local/Converters.kt", "app/src/main/java/com/azuratech/azuratime/core/data/local/Converters.kt", "com.azuratech.azuratime.data.local", "com.azuratech.azuratime.core.data.local"),
    ("app/src/main/java/com/azuratech/azuratime/data/local/FaceCache.kt", "app/src/main/java/com/azuratech/azuratime/core/data/local/FaceCache.kt", "com.azuratech.azuratime.data.local", "com.azuratech.azuratime.core.data.local"),
    ("app/src/main/java/com/azuratech/azuratime/data/local/ProfileMappers.kt", "app/src/main/java/com/azuratech/azuratime/core/data/local/ProfileMappers.kt", "com.azuratech.azuratime.data.local", "com.azuratech.azuratime.core.data.local"),
    
    # DI
    ("app/src/main/java/com/azuratech/azuratime/core/di/AppModule.kt", "app/src/main/java/com/azuratech/azuratime/core/di/AppModule.kt", "com.azuratech.azuratime.core.di", "com.azuratech.azuratime.core.di"),
    ("app/src/main/java/com/azuratech/azuratime/core/di/DataSourceModule.kt", "app/src/main/java/com/azuratech/azuratime/core/di/DataSourceModule.kt", "com.azuratech.azuratime.core.di", "com.azuratech.azuratime.core.di"),
    ("app/src/main/java/com/azuratech/azuratime/core/di/RepositoryModule.kt", "app/src/main/java/com/azuratech/azuratime/core/di/RepositoryModule.kt", "com.azuratech.azuratime.core.di", "com.azuratech.azuratime.core.di"),
    
    # Feature Attendance
    ("app/src/main/java/com/azuratech/azuratime/features/attendance/data/repo/CheckInRepositoryImpl.kt", "app/src/main/java/com/azuratech/azuratime/features/attendance/data/repo/AttendanceRepositoryImpl.kt", "com.azuratech.azuratime.features.attendance.data.repo", "com.azuratech.azuratime.features.attendance.data.repo"),
    ("app/src/main/java/com/azuratech/azuratime/features/attendance/domain/repository/CheckInRepository.kt", "app/src/main/java/com/azuratech/azuratime/features/attendance/domain/repository/AttendanceRepository.kt", "com.azuratech.azuratime.features.attendance.domain.repository", "com.azuratech.azuratime.features.attendance.domain.repository"),
    ("app/src/main/java/com/azuratech/azuratime/features/attendance/data/local/CheckInLocalDataSourceImpl.kt", "app/src/main/java/com/azuratech/azuratime/features/attendance/data/local/AttendanceLocalDataSourceImpl.kt", "com.azuratech.azuratime.features.attendance.data.local", "com.azuratech.azuratime.features.attendance.data.local"),
]

for m in mappings:
    migrate(*m)
