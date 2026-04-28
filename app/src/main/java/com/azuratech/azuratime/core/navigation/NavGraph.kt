package com.azuratech.azuratime.core.navigation

import android.net.Uri

/**
 * 🗺️ AZURA NAVIGATION MAP (PURE-CLASS 2.0)
 * Cleaned of all legacy Options and UserClassManagement routes.
 */
sealed class Screen(val route: String) {

    // --- 🚪 AUTH & ONBOARDING ---
    data object Login : Screen("login")
    // 🔥 UBAH: Gabungkan rute validasi akun menjadi satu
    data object Membership : Screen("membership") 
    data object SchoolRegistration : Screen("school_registration")

    // --- 🏠 CORE FEATURES ---
    data object Dashboard : Screen("dashboard")
    data object CheckIn : Screen("check_in")
    data object BarcodeScan : Screen("barcode_scan") // 🔥 RUTE BARU DITAMBAHKAN DI SINI

    // --- 👤 REGISTRATION & FACE DATA ---
    data object RegistrationMenu : Screen("registration_menu")
    data object AddUser : Screen("add_user")
    data object BulkRegister : Screen("bulk_register")
    data object Manage : Screen("manage_faces")
    data object FaceListBarcode : Screen("face_list_barcode") // 🔥 RUTE BARU UNTUK GENERATOR BARCODE
    data object EditUser : Screen("edit_user/{faceId}") {
        fun createRoute(faceId: String) = "edit_user/$faceId"
    }

    // --- 📊 ATTENDANCE & REPORT ---
    data object AttendanceMatrix : Screen("attendance_matrix")
    data object CheckInRecordEntity : Screen("checkin_history")
    
    // 🔥 PERBAIKAN BUG: Gunakan Uri.encode() untuk nama agar tidak crash jika ada karakter "/"
    data object DailyDetail : Screen("daily_detail/{faceId}/{name}/{date}") {
        fun createRoute(faceId: String, name: String, date: String): String {
            val safeName = Uri.encode(name)
            return "daily_detail/$faceId/$safeName/$date"
        }
    }

    data object ManualAttendance : Screen("manual_attendance?faceId={faceId}&date={date}") {
        fun createRoute(faceId: String = "", date: String = "") = 
            "manual_attendance?faceId=$faceId&date=$date"
    }

    // --- 🗄️ DATA MANAGEMENT ---
    data object DataDashboard : Screen("data_dashboard")
    data object DataManagement : Screen("data_management/{dataType}") {
        fun createRoute(dataType: String) = "data_management/$dataType"
    }

    // --- 🏫 USER & CLASS MANAGEMENT ---
    data object AdminDashboard : Screen("admin_dashboard")
    data object Profile : Screen("user_profile")
    data object SchoolList : Screen("school_list/{accountId}") {
        fun createRoute(accountId: String) = "school_list/$accountId"
    }
    data object ClassList : Screen("class_list/{schoolId}") {
        fun createRoute(schoolId: String) = "class_list/$schoolId"
    }
    
    data object ClassManagement : Screen("class_management/{accountId}") {
        fun createRoute(accountId: String) = "class_management/$accountId"
    }

    // 🔥 PERBAIKAN BUG: Encode nama kelas jika nama kelasnya "12 / IPA"
    data object ClassDetail : Screen("class_detail/{classId}/{className}") {
        fun createRoute(classId: String, className: String): String {
            val safeName = Uri.encode(className)
            return "class_detail/$classId/$safeName"
        }
    }

    // 🔥 PERBAIKAN BUG: Encode nama siswa
    data object MyAssignedClass : Screen("my_assigned_classes?targetUserId={targetUserId}&schoolId={schoolId}") {
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
    data object FindSchool : Screen("find_school")
    data object Onboarding : Screen("onboarding")
    data object CreateSchool : Screen("create_school")
    data object SetupWizard : Screen("setup_wizard")

    // --- 🤝 JARINGAN SEDULUR ---
    data object Network : Screen("network")

    // --- 👑 ADMIN & MODERATION ---
    data object PendingSchools : Screen("pending_schools")

    // --- 🛠️ SYSTEM & DEBUG ---
    data object Debug : Screen("debug")
}