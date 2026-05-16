# 📏 NAMING CONSISTENCY REPORT
⚡ *Auto-generated. Run `bash ~/scan_naming.sh` to refresh.*

## ⚠️ PascalCase File Violations

## ⚠️ camelCase Function Violations (PascalCase used)
- app/src/main/java/com/azuratech/azuratime/core/ui/navigation/graphs/AttendanceGraph.kt:42:private fun TextPlaceholder(text: String) {
- app/src/main/java/com/azuratech/azuratime/core/ui/RootScreen.kt:23:fun RootScreen() {
- app/src/main/java/com/azuratech/azuratime/core/ui/theme/AzuraGradients.kt:105:fun AzuraTheme(
- app/src/main/java/com/azuratech/azuratime/core/ui/MainScreen.kt:29:fun MainScreen() {
- app/src/main/java/com/azuratech/azuratime/core/ui/MainScreen.kt:86:fun BottomNav(navController: NavHostController) {
- app/src/main/java/com/azuratech/azuratime/core/ui/MainScreen.kt:115:fun LoadingPlaceholder() {
- app/src/main/java/com/azuratech/azuratime/core/ui/designsystem/FaceOverlay.kt:24:fun FaceOverlay(
- app/src/main/java/com/azuratech/azuratime/core/ui/designsystem/AzuraTextField.kt:18:fun AzuraTextField(
- app/src/main/java/com/azuratech/azuratime/core/ui/designsystem/AzuraTextField.kt:55:fun PreviewAzuraTextField() {
- app/src/main/java/com/azuratech/azuratime/core/ui/designsystem/AzuraButton.kt:19:fun AzuraButton(
- app/src/main/java/com/azuratech/azuratime/core/ui/designsystem/AzuraButton.kt:69:fun PreviewAzuraButton() {
- app/src/main/java/com/azuratech/azuratime/core/ui/designsystem/AppTopBar.kt:11:fun AppTopBar(
- app/src/main/java/com/azuratech/azuratime/core/ui/designsystem/ZoharChatSheet.kt:19:fun ZoharChatSheet(
- app/src/main/java/com/azuratech/azuratime/core/ui/designsystem/AzuraDatePickerButton.kt:23:fun AzuraDatePickerButton(
- app/src/main/java/com/azuratech/azuratime/core/ui/designsystem/ConflictResolverDialog.kt:22:fun ConflictResolverDialog(
- app/src/main/java/com/azuratech/azuratime/core/ui/designsystem/ConflictResolverDialog.kt:126:private fun ConflictSide(
- app/src/main/java/com/azuratech/azuratime/core/ui/designsystem/AzuraCard.kt:16:fun AzuraCard(
- app/src/main/java/com/azuratech/azuratime/core/ui/designsystem/AzuraCard.kt:52:fun PreviewAzuraCard() {
- app/src/main/java/com/azuratech/azuratime/core/ui/designsystem/AzuraUserRow.kt:18:fun AzuraUserRow(
- app/src/main/java/com/azuratech/azuratime/core/ui/designsystem/StudentAvatar.kt:9:fun StudentAvatar(

## ⚠️ snake_case Variable Violations
- app/src/main/java/com/azuratech/azuratime/core/navigation/Screen.kt:31:        fun createRoute(studentId: String) = "edit_student/$studentId"
- app/src/main/java/com/azuratech/azuratime/core/navigation/Screen.kt:43:            return "daily_detail/$studentId/$safeName/$date"
- app/src/main/java/com/azuratech/azuratime/core/navigation/Screen.kt:49:            "manual_attendance?studentId=$studentId&date=$date"
- app/src/main/java/com/azuratech/azuratime/core/navigation/Screen.kt:55:        fun createRoute(dataType: String) = "data_management/$dataType"
- app/src/main/java/com/azuratech/azuratime/core/navigation/Screen.kt:62:        fun createRoute(accountId: String) = "school_list/$accountId"
- app/src/main/java/com/azuratech/azuratime/core/navigation/Screen.kt:65:        fun createRoute(schoolId: String) = "class_list/$schoolId"
- app/src/main/java/com/azuratech/azuratime/core/navigation/Screen.kt:69:        fun createRoute(accountId: String) = "class_management/$accountId"
- app/src/main/java/com/azuratech/azuratime/core/navigation/Screen.kt:76:            return "class_detail/$classId/$safeName"
- app/src/main/java/com/azuratech/azuratime/core/navigation/Screen.kt:84:                append("my_assigned_classes")
- app/src/main/java/com/azuratech/azuratime/core/navigation/NavigationRoutes.kt:7:    const val SCHOOL_REGISTRATION = "school_registration"
- app/src/main/java/com/azuratech/azuratime/core/navigation/NavigationRoutes.kt:9:    const val FIND_SCHOOL = "find_school"
- app/src/main/java/com/azuratech/azuratime/core/navigation/NavigationRoutes.kt:10:    const val CREATE_SCHOOL = "create_school"
- app/src/main/java/com/azuratech/azuratime/core/navigation/NavigationRoutes.kt:11:    const val SETUP_WIZARD = "setup_wizard"
- app/src/main/java/com/azuratech/azuratime/core/navigation/NavigationRoutes.kt:15:    const val ADMIN_DASHBOARD = "admin_dashboard"
- app/src/main/java/com/azuratech/azuratime/core/navigation/NavigationRoutes.kt:18:    const val ATTENDANCE_CAPTURE = "attendance_capture"

## ℹ️ Flow Variables Missing 'Flow' Suffix
- app/src/main/java/com/azuratech/azuratime/core/data/repo/MainRepository.kt:49:    fun observeRevokeStatus(uid: String): Flow<Boolean> = callbackFlow {
- app/src/main/java/com/azuratech/azuratime/core/data/local/AccountClassAccessDao.kt:15:    fun getAssignedClassIds(accountId: String, schoolId: String): Flow<List<String>>
- app/src/main/java/com/azuratech/azuratime/core/boot/BootViewModel.kt:25:    private val _state = MutableStateFlow<BootState>(BootState.Loading)
- app/src/main/java/com/azuratech/azuratime/core/session/SessionManager.kt:62:    private val _activeSchoolIdFlow = MutableStateFlow<String?>(getActiveSchoolId())
- app/src/main/java/com/azuratech/azuratime/core/session/SessionManager.kt:65:    private val _currentUserIdFlow = MutableStateFlow<String?>(
- app/src/main/java/com/azuratech/azuratime/features/auth/ui/AuthViewModel.kt:25:    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
- app/src/main/java/com/azuratech/azuratime/features/account/data/repo/MembershipRepository.kt:141:    fun observeMemberships(uid: String): Flow<List<com.azuratech.azuratime.features.account.data.local.Membership>> {
- app/src/main/java/com/azuratech/azuratime/features/account/data/repo/MembershipRepository.kt:148:    fun observeMembershipFlow(uid: String): Flow<MembershipDocUpdate> {
- app/src/main/java/com/azuratech/azuratime/features/account/data/repo/AccessRequestRepositoryImpl.kt:66:    override fun observeRequestsByUser(userId: String): Flow<List<AccessRequestEntity>> {
- app/src/main/java/com/azuratech/azuratime/features/account/data/repo/AdminRepository.kt:34:    fun observeAccountsForSchool(schoolId: String): Flow<List<AccountEntity>> =
- app/src/main/java/com/azuratech/azuratime/features/account/data/local/AccessRequestDao.kt:15:    fun observeRequestsByUser(userId: String): Flow<List<AccessRequestEntity>>
- app/src/main/java/com/azuratech/azuratime/features/account/data/local/AccountDao.kt:21:    fun observeAccountById(id: String): Flow<AccountEntity?>
- app/src/main/java/com/azuratech/azuratime/features/account/data/local/AccountDao.kt:24:    fun observeAllAccounts(): Flow<List<AccountEntity>>
- app/src/main/java/com/azuratech/azuratime/features/account/domain/repository/AccessRequestRepository.kt:12:    fun observeRequestsByUser(userId: String): Flow<List<AccessRequestEntity>>
- app/src/main/java/com/azuratech/azuratime/features/account/ui/management/AccountManagementViewModel.kt:69:    private val _selectedTargetUser = MutableStateFlow<AccountEntity?>(null)

