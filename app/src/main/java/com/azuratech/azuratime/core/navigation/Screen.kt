package com.azuratech.azuratime.core.navigation

import android.net.Uri

/**
 * 🗺️ AZURA NAVIGATION MAP (PURE-CLASS 2.0)
 * Cleaned of all legacy Options and Management routes.
 */
sealed class Screen(val route: String) {

    // --- 🚪 AUTH & ONBOARDING ---
    data object Login : Screen(NavigationRoutes.LOGIN)

    // 🔥 UBAH: Gabungkan rute validasi akun menjadi satu
    data object Membership : Screen(NavigationRoutes.MEMBERSHIP)
    data object SchoolRegistration : Screen(NavigationRoutes.SCHOOL_REGISTRATION)

    // --- 🏠 CORE FEATURES ---
    data object Dashboard : Screen(NavigationRoutes.DASHBOARD)
    data object AttendanceCapture : Screen(NavigationRoutes.ATTENDANCE_CAPTURE)
    data object BarcodeScan : Screen(NavigationRoutes.BARCODE_SCAN) // 🔥 RUTE BARU DITAMBAHKAN DI SINI

    // --- 👤 REGISTRATION & FACE DATA ---
    data object RegistrationMenu : Screen(NavigationRoutes.REGISTRATION_MENU)
    data object AddStudent : Screen(NavigationRoutes.ADD_STUDENT)
    data object BulkRegister : Screen(NavigationRoutes.BULK_REGISTER)
    data object StudentRoster : Screen(NavigationRoutes.STUDENT_ROSTER)
    data object BiometricManagement : Screen(NavigationRoutes.BIOMETRIC_MANAGEMENT)
    data object StudentRosterBarcode : Screen(NavigationRoutes.STUDENT_ROSTER_BARCODE)
    data object StudentAssignment : Screen(NavigationRoutes.STUDENT_ASSIGNMENT)
    data object EditStudent : Screen(NavigationRoutes.EDIT_STUDENT) {
        fun createRoute(studentId: String) = "editStudent/$studentId"
    }

    // --- 📊 ATTENDANCE & REPORT ---
    data object AttendanceMatrix : Screen(NavigationRoutes.ATTENDANCE_MATRIX)
    data object AttendanceHistory : Screen(NavigationRoutes.ATTENDANCE_HISTORY)
    data object AuditLog : Screen(NavigationRoutes.AUDIT_LOG)

    // 🔥 PERBAIKAN BUG: Gunakan Uri.encode() untuk nama agar tidak crash jika ada karakter "/"
    data object DailyDetail : Screen(NavigationRoutes.DAILY_DETAIL) {
        fun createRoute(studentId: String, name: String, date: String): String {
            val safeName = Uri.encode(name)
            return "dailyDetail/$studentId/$safeName/$date"
        }
    }

    data object ManualAttendance : Screen(NavigationRoutes.MANUAL_ATTENDANCE) {
        fun createRoute(studentId: String = "", date: String = "") =
            "manualAttendance?studentId=$studentId&date=$date"
    }

    // --- 🗄️ DATA MANAGEMENT ---
    data object DataDashboard : Screen(NavigationRoutes.DATA_DASHBOARD)
    data object DataManagement : Screen(NavigationRoutes.DATA_MANAGEMENT) {
        fun createRoute(dataType: String) = "dataManagement/$dataType"
    }

    // --- 🏫 ACCOUNT & CLASS MANAGEMENT ---
    data object AdminDashboard : Screen(NavigationRoutes.ADMIN_DASHBOARD)
    data object Profile : Screen(NavigationRoutes.ACCOUNT_PROFILE)
    data object SchoolList : Screen(NavigationRoutes.SCHOOL_LIST) {
        fun createRoute(accountId: String) = "schoolList/$accountId"
    }
    data object ClassList : Screen(NavigationRoutes.CLASS_LIST) {
        fun createRoute(schoolId: String) = "classList/$schoolId"
    }

    data object ClassManagement : Screen(NavigationRoutes.CLASS_MANAGEMENT) {
        fun createRoute(accountId: String) = "classManagement/$accountId"
    }

    // 🔥 PERBAIKAN BUG: Encode nama kelas jika nama kelasnya "12 / IPA"
    data object ClassDetail : Screen(NavigationRoutes.CLASS_DETAIL) {
        fun createRoute(classId: String, className: String): String {
            val safeName = Uri.encode(className)
            return "classDetail/$classId/$safeName"
        }
    }

    // 🔥 PERBAIKAN BUG: Encode nama siswa
    data object MyAssignedClass : Screen(NavigationRoutes.MY_ASSIGNED_CLASSES) {
        fun createRoute(targetAccountId: String? = null, schoolId: String? = null) =
            buildString {
                append("myAssignedClasses")
                val params = mutableListOf<String>()
                if (targetAccountId != null) params.add("targetAccountId=$targetAccountId")
                if (schoolId != null) params.add("schoolId=$schoolId")
                if (params.isNotEmpty()) {
                    append("?")
                    append(params.joinToString("&"))
                }
            }
    }

    data object AssignClass : Screen(NavigationRoutes.ASSIGN_CLASS) {
        fun createRoute(targetAccountId: String, role: String) = "assignClass/$targetAccountId/$role"
    }
    data object FindSchool : Screen(NavigationRoutes.FIND_SCHOOL)
    data object Onboarding : Screen(NavigationRoutes.ONBOARDING)
    data object CreateSchool : Screen(NavigationRoutes.CREATE_SCHOOL)
    data object SetupWizard : Screen(NavigationRoutes.SETUP_WIZARD)

    // --- 🤝 FOLLOWING & CONNECTIONS ---
    data object Following : Screen(NavigationRoutes.FOLLOWING)

    // --- 👑 ADMIN & MODERATION ---
    data object PendingSchools : Screen(NavigationRoutes.PENDING_SCHOOLS)

    // --- 🛠️ SYSTEM & DEBUG ---
    data object Debug : Screen(NavigationRoutes.DEBUG)
    data object AiMusic : Screen(NavigationRoutes.AI_MUSIC)
}
