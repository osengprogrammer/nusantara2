package com.azuratech.azuratime.core.navigation

import android.net.Uri
import com.azuratech.azuratime.navigation.NavigationRoutes

/**
 * 🗺️ AZURA NAVIGATION MAP (PURE-CLASS 2.0)
 * Cleaned of all legacy Options and UserClassManagement routes.
 */
sealed class Screen(val route: String) {

    // --- 🚪 AUTH & ONBOARDING ---
    data object Login : Screen(NavigationRoutes.LOGIN)
    // 🔥 UBAH: Gabungkan rute validasi akun menjadi satu
    data object Membership : Screen(NavigationRoutes.MEMBERSHIP) 
    data object SchoolRegistration : Screen(NavigationRoutes.SCHOOL_REGISTRATION)

    // --- 🏠 CORE FEATURES ---
    data object Dashboard : Screen(NavigationRoutes.DASHBOARD)
    data object CheckIn : Screen(NavigationRoutes.CHECK_IN)
    data object BarcodeScan : Screen(NavigationRoutes.BARCODE_SCAN) // 🔥 RUTE BARU DITAMBAHKAN DI SINI

    // --- 👤 REGISTRATION & FACE DATA ---
    data object RegistrationMenu : Screen(NavigationRoutes.REGISTRATION_MENU)
    data object AddStudent : Screen(NavigationRoutes.ADD_STUDENT)
    data object BulkRegister : Screen(NavigationRoutes.BULK_REGISTER)
    data object StudentRoster : Screen(NavigationRoutes.STUDENT_ROSTER)
    data object BiometricManagement : Screen(NavigationRoutes.BIOMETRIC_MANAGEMENT)
    data object StudentRosterBarcode : Screen(NavigationRoutes.STUDENT_ROSTER_BARCODE)
    data object EditStudent : Screen(NavigationRoutes.EDIT_STUDENT) {
        fun createRoute(faceId: String) = "edit_student/$faceId"
    }

    // --- 📊 ATTENDANCE & REPORT ---
    data object AttendanceMatrix : Screen(NavigationRoutes.ATTENDANCE_MATRIX)
    data object CheckInRecordEntity : Screen(NavigationRoutes.CHECKIN_HISTORY)
    
    // 🔥 PERBAIKAN BUG: Gunakan Uri.encode() untuk nama agar tidak crash jika ada karakter "/"
    data object DailyDetail : Screen(NavigationRoutes.DAILY_DETAIL) {
        fun createRoute(faceId: String, name: String, date: String): String {
            val safeName = Uri.encode(name)
            return "daily_detail/$faceId/$safeName/$date"
        }
    }

    data object ManualAttendance : Screen(NavigationRoutes.MANUAL_ATTENDANCE) {
        fun createRoute(faceId: String = "", date: String = "") = 
            "manual_attendance?faceId=$faceId&date=$date"
    }

    // --- 🗄️ DATA MANAGEMENT ---
    data object DataDashboard : Screen(NavigationRoutes.DATA_DASHBOARD)
    data object DataManagement : Screen(NavigationRoutes.DATA_MANAGEMENT) {
        fun createRoute(dataType: String) = "data_management/$dataType"
    }

    // --- 🏫 USER & CLASS MANAGEMENT ---
    data object AdminDashboard : Screen(NavigationRoutes.ADMIN_DASHBOARD)
    data object Profile : Screen(NavigationRoutes.USER_PROFILE)
    data object SchoolList : Screen(NavigationRoutes.SCHOOL_LIST) {
        fun createRoute(accountId: String) = "school_list/$accountId"
    }
    data object ClassList : Screen(NavigationRoutes.CLASS_LIST) {
        fun createRoute(schoolId: String) = "class_list/$schoolId"
    }
    
    data object ClassManagement : Screen(NavigationRoutes.CLASS_MANAGEMENT) {
        fun createRoute(accountId: String) = "class_management/$accountId"
    }

    // 🔥 PERBAIKAN BUG: Encode nama kelas jika nama kelasnya "12 / IPA"
    data object ClassDetail : Screen(NavigationRoutes.CLASS_DETAIL) {
        fun createRoute(classId: String, className: String): String {
            val safeName = Uri.encode(className)
            return "class_detail/$classId/$safeName"
        }
    }

    // 🔥 PERBAIKAN BUG: Encode nama siswa
    data object MyAssignedClass : Screen(NavigationRoutes.MY_ASSIGNED_CLASSES) {
        fun createRoute(targetUserId: String? = null, schoolId: String? = null) =
            buildString {
                append("my_assigned_classes")
                val params = mutableListOf<String>()
                if (targetUserId != null) params.add("targetUserId=$targetUserId")
                if (schoolId != null) params.add("schoolId=$schoolId")
                if (params.isNotEmpty()) {
                    append("?")
                    append(params.joinToString("&"))
                }
            }
    }
    data object FindSchool : Screen(NavigationRoutes.FIND_SCHOOL)
    data object Onboarding : Screen(NavigationRoutes.ONBOARDING)
    data object CreateSchool : Screen(NavigationRoutes.CREATE_SCHOOL)
    data object SetupWizard : Screen(NavigationRoutes.SETUP_WIZARD)

    // --- 🤝 JARINGAN SEDULUR ---
    data object Network : Screen(NavigationRoutes.NETWORK)

    // --- 👑 ADMIN & MODERATION ---
    data object PendingSchools : Screen(NavigationRoutes.PENDING_SCHOOLS)

    // --- 🛠️ SYSTEM & DEBUG ---
    data object Debug : Screen(NavigationRoutes.DEBUG)
}