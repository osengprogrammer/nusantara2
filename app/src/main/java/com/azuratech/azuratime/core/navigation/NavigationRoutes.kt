package com.azuratech.azuratime.core.navigation

object NavigationRoutes {
    // --- 🚪 AUTH & ONBOARDING ---
    const val LOGIN = "login"
    const val MEMBERSHIP = "membership"
    const val SCHOOL_REGISTRATION = "schoolRegistration"
    const val ONBOARDING = "onboarding"
    const val FIND_SCHOOL = "findSchool"
    const val CREATE_SCHOOL = "createSchool"
    const val SETUP_WIZARD = "setupWizard"

    // --- 🏠 CORE FEATURES ---
    const val DASHBOARD = "dashboard"
    const val ADMIN_DASHBOARD = "adminDashboard"

    // --- 📊 ATTENDANCE & REPORT ---
    const val ATTENDANCE_CAPTURE = "attendanceCapture"
    const val BARCODE_SCAN = "barcodeScan"
    const val ATTENDANCE_HISTORY = "attendanceHistory"
    const val ATTENDANCE_MATRIX = "attendanceMatrix"
    const val DAILY_DETAIL = "dailyDetail/{studentId}/{name}/{date}"
    const val MANUAL_ATTENDANCE = "manualAttendance?studentId={studentId}&date={date}"
    const val AUDIT_LOG = "auditLog"

    // --- 👤 REGISTRATION & FACE DATA ---
    const val REGISTRATION_MENU = "registrationMenu"
    const val ADD_STUDENT = "addStudent"
    const val BULK_REGISTER = "bulkRegister"
    const val STUDENT_ROSTER = "studentRoster"
    const val BIOMETRIC_MANAGEMENT = "biometricManagement"
    const val STUDENT_ROSTER_BARCODE = "studentRosterBarcode"
    const val EDIT_STUDENT = "editStudent/{studentId}"
    const val STUDENT_ASSIGNMENT = "studentAssignment"

    // --- 🏫 CLASS MANAGEMENT ---
    const val CLASS_LIST = "classList/{schoolId}"
    const val CLASS_MANAGEMENT = "classManagement/{accountId}"
    const val CLASS_DETAIL = "classDetail/{classId}/{className}"
    const val MY_ASSIGNED_CLASSES = "myAssignedClasses?targetAccountId={targetAccountId}&schoolId={schoolId}"
    const val ASSIGN_CLASS = "assignClass/{targetAccountId}"

    // --- 🗄️ DATA & SYSTEM ---
    const val DATA_DASHBOARD = "dataDashboard"
    const val DATA_MANAGEMENT = "dataManagement/{dataType}"
    const val PENDING_SCHOOLS = "pendingSchools"
    const val ACCOUNT_PROFILE = "accountProfile"
    const val SCHOOL_LIST = "schoolList/{accountId}"
    const val FOLLOWING = "following"
    const val DEBUG = "debug"
    const val AI_MUSIC = "aiMusic"

    // --- 🗺️ NAVIGATION GRAPHS ---
    const val DASHBOARD_GRAPH = "dashboardGraph"
    const val ATTENDANCE_GRAPH = "attendanceGraph"
    const val MANAGEMENT_GRAPH = "managementGraph"
    const val REPORTING_GRAPH = "reportingGraph"
    const val ACCOUNT_GRAPH = "accountGraph"
}
