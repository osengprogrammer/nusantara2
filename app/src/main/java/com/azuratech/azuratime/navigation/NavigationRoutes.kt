package com.azuratech.azuratime.navigation

object NavigationRoutes {
    // --- 🚪 AUTH & ONBOARDING ---
    const val LOGIN = "login"
    const val MEMBERSHIP = "membership"
    const val SCHOOL_REGISTRATION = "school_registration"
    const val ONBOARDING = "onboarding"
    const val FIND_SCHOOL = "find_school"
    const val CREATE_SCHOOL = "create_school"
    const val SETUP_WIZARD = "setup_wizard"

    // --- 🏠 CORE FEATURES ---
    const val DASHBOARD = "dashboard"
    const val ADMIN_DASHBOARD = "admin_dashboard"

    // --- 📊 ATTENDANCE & REPORT ---
    const val ATTENDANCE_CAPTURE = "attendance_capture"
    const val BARCODE_SCAN = "barcode_scan"
    const val CHECKIN_HISTORY = "checkin_history"
    const val ATTENDANCE_MATRIX = "attendance_matrix"
    const val DAILY_DETAIL = "daily_detail/{studentId}/{name}/{date}"
    const val MANUAL_ATTENDANCE = "manual_attendance?studentId={studentId}&date={date}"

    // --- 👤 REGISTRATION & FACE DATA ---
    const val REGISTRATION_MENU = "registration_menu"
    const val ADD_STUDENT = "add_student"
    const val BULK_REGISTER = "bulk_register"
    const val STUDENT_ROSTER = "student_roster"
    const val BIOMETRIC_MANAGEMENT = "biometric_management"
    const val STUDENT_ROSTER_BARCODE = "student_roster_barcode"
    const val EDIT_STUDENT = "edit_student/{studentId}"

    // --- 🏫 CLASS MANAGEMENT ---
    const val CLASS_LIST = "class_list/{schoolId}"
    const val CLASS_MANAGEMENT = "class_management/{accountId}"
    const val CLASS_DETAIL = "class_detail/{classId}/{className}"
    const val MY_ASSIGNED_CLASSES = "my_assigned_classes?targetUserId={targetUserId}&schoolId={schoolId}"

    // --- 🗄️ DATA & SYSTEM ---
    const val DATA_DASHBOARD = "data_dashboard"
    const val DATA_MANAGEMENT = "data_management/{dataType}"
    const val PENDING_SCHOOLS = "pending_schools"
    const val USER_PROFILE = "user_profile"
    const val SCHOOL_LIST = "school_list/{accountId}"
    const val NETWORK = "network"
    const val DEBUG = "debug"

    // --- 🗺️ NAVIGATION GRAPHS ---
    const val DASHBOARD_GRAPH = "dashboard_graph"
    const val ATTENDANCE_GRAPH = "attendance_graph"
    const val MANAGEMENT_GRAPH = "management_graph"
    const val REPORTING_GRAPH = "reporting_graph"
    const val USER_GRAPH = "user_graph"
}
