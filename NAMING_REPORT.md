# 📏 NAMING CONSISTENCY REPORT
⚡ *Auto-generated. Run `bash ~/scan_naming.sh` to refresh.*

## ⚠️ PascalCase File Violations

## ⚠️ camelCase Function Violations (PascalCase used)
- app/src/main/java/com/azuratech/azuratime/MainApp.kt:26:fun MainApp(onBootReady: () -> Unit = {}) {
- app/src/main/java/com/azuratech/azuratime/MainApp.kt:116:fun LoadingScreen(onRetry: () -> Unit) {
- app/src/main/java/com/azuratech/azuratime/MainApp.kt:138:fun SecurityAlertDialog(message: String, onReLogin: () -> Unit) {
- app/src/main/java/com/azuratech/azuratime/ui/auth/LoginScreen.kt:28:fun LoginScreen(
- app/src/main/java/com/azuratech/azuratime/ui/core/designsystem/FaceOverlay.kt:24:fun FaceOverlay(
- app/src/main/java/com/azuratech/azuratime/ui/core/designsystem/AzuraTextField.kt:18:fun AzuraTextField(
- app/src/main/java/com/azuratech/azuratime/ui/core/designsystem/AzuraTextField.kt:55:fun PreviewAzuraTextField() {
- app/src/main/java/com/azuratech/azuratime/ui/core/designsystem/AzuraButton.kt:19:fun AzuraButton(
- app/src/main/java/com/azuratech/azuratime/ui/core/designsystem/AzuraButton.kt:69:fun PreviewAzuraButton() {
- app/src/main/java/com/azuratech/azuratime/ui/core/designsystem/FaceAvatar.kt:36:fun FaceAvatar(
- app/src/main/java/com/azuratech/azuratime/ui/core/designsystem/FaceAvatar.kt:141:fun SmallFaceAvatar(
- app/src/main/java/com/azuratech/azuratime/ui/core/designsystem/FaceAvatar.kt:152:fun LargeFaceAvatar(
- app/src/main/java/com/azuratech/azuratime/ui/core/designsystem/AppTopBar.kt:11:fun AppTopBar(
- app/src/main/java/com/azuratech/azuratime/ui/core/designsystem/ZoharChatSheet.kt:19:fun ZoharChatSheet(
- app/src/main/java/com/azuratech/azuratime/ui/core/designsystem/FaceDialogs.kt:17:fun QuickEditFaceDialog(
- app/src/main/java/com/azuratech/azuratime/ui/core/designsystem/FaceDialogs.kt:45:fun MultiClassAssignmentDialog(
- app/src/main/java/com/azuratech/azuratime/ui/core/designsystem/AzuraDatePickerButton.kt:23:fun AzuraDatePickerButton(
- app/src/main/java/com/azuratech/azuratime/ui/core/designsystem/ConflictResolverDialog.kt:22:fun ConflictResolverDialog(
- app/src/main/java/com/azuratech/azuratime/ui/core/designsystem/ConflictResolverDialog.kt:126:private fun ConflictSide(
- app/src/main/java/com/azuratech/azuratime/ui/core/designsystem/AzuraCard.kt:16:fun AzuraCard(

## ⚠️ snake_case Variable Violations
- app/src/main/java/com/azuratech/azuratime/core/navigation/NavGraph.kt:15:    data object SchoolRegistration : Screen("school_registration")
- app/src/main/java/com/azuratech/azuratime/core/navigation/NavGraph.kt:19:    data object CheckIn : Screen("check_in")
- app/src/main/java/com/azuratech/azuratime/core/navigation/NavGraph.kt:23:    data object RegistrationMenu : Screen("registration_menu")
- app/src/main/java/com/azuratech/azuratime/core/navigation/NavGraph.kt:24:    data object AddUser : Screen("add_user")
- app/src/main/java/com/azuratech/azuratime/core/navigation/NavGraph.kt:25:    data object BulkRegister : Screen("bulk_register")
- app/src/main/java/com/azuratech/azuratime/core/navigation/NavGraph.kt:26:    data object Manage : Screen("manage_faces")
- app/src/main/java/com/azuratech/azuratime/core/navigation/NavGraph.kt:27:    data object BiometricManagement : Screen("biometric_management")
- app/src/main/java/com/azuratech/azuratime/core/navigation/NavGraph.kt:28:    data object FaceListBarcode : Screen("face_list_barcode")
- app/src/main/java/com/azuratech/azuratime/core/navigation/NavGraph.kt:29:    data object EditUser : Screen("edit_user/{faceId}") {
- app/src/main/java/com/azuratech/azuratime/core/navigation/NavGraph.kt:30:        fun createRoute(faceId: String) = "edit_user/$faceId"
- app/src/main/java/com/azuratech/azuratime/core/navigation/NavGraph.kt:34:    data object AttendanceMatrix : Screen("attendance_matrix")
- app/src/main/java/com/azuratech/azuratime/core/navigation/NavGraph.kt:35:    data object CheckInRecordEntity : Screen("checkin_history")
- app/src/main/java/com/azuratech/azuratime/core/navigation/NavGraph.kt:38:    data object DailyDetail : Screen("daily_detail/{faceId}/{name}/{date}") {
- app/src/main/java/com/azuratech/azuratime/core/navigation/NavGraph.kt:41:            return "daily_detail/$faceId/$safeName/$date"
- app/src/main/java/com/azuratech/azuratime/core/navigation/NavGraph.kt:45:    data object ManualAttendance : Screen("manual_attendance?faceId={faceId}&date={date}") {

## ℹ️ Flow Variables Missing 'Flow' Suffix
- app/src/main/java/com/azuratech/azuratime/core/boot/BootViewModel.kt:25:    private val _state = MutableStateFlow<BootState>(BootState.Loading)
- app/src/main/java/com/azuratech/azuratime/core/session/SessionManager.kt:57:    private val _activeSchoolIdFlow = MutableStateFlow<String?>(getActiveSchoolId())
- app/src/main/java/com/azuratech/azuratime/core/session/SessionManager.kt:60:    private val _currentUserIdFlow = MutableStateFlow<String?>(
- app/src/main/java/com/azuratech/azuratime/data/remote/SchoolRemoteDataSource.kt:11:    fun observeRemoteSchools(accountId: String): Flow<Result<List<School>>>
- app/src/main/java/com/azuratech/azuratime/data/remote/SchoolRemoteDataSourceImpl.kt:62:    override fun observeRemoteSchools(accountId: String): Flow<Result<List<School>>> = callbackFlow {
- app/src/main/java/com/azuratech/azuratime/data/repo/AttendanceRepository.kt:18:    fun observeAttendanceMatrix(schoolId: String): Flow<List<AttendanceEntity>> {
- app/src/main/java/com/azuratech/azuratime/data/repo/DataIntegrityRepository.kt:36:    val totalFaces: Flow<Int> = schoolIdFlow.flatMapLatest { id ->
- app/src/main/java/com/azuratech/azuratime/data/repo/DataIntegrityRepository.kt:41:    val totalRecords: Flow<Int> = schoolIdFlow.flatMapLatest { id ->
- app/src/main/java/com/azuratech/azuratime/data/repo/DataIntegrityRepository.kt:50:    val missingAssignment: Flow<Int> = schoolIdFlow.flatMapLatest { id ->
- app/src/main/java/com/azuratech/azuratime/data/repo/DataIntegrityRepository.kt:55:    val brokenAssignments: Flow<Int> = schoolIdFlow.flatMapLatest { id ->
- app/src/main/java/com/azuratech/azuratime/data/repo/DataIntegrityRepository.kt:64:    val globalUnsyncedCount: Flow<Int> = schoolIdFlow.flatMapLatest { id ->
- app/src/main/java/com/azuratech/azuratime/data/repo/DataIntegrityRepository.kt:75:    val conflicts: Flow<List<com.azuratech.azuratime.data.local.AttendanceConflictEntity>> =
- app/src/main/java/com/azuratech/azuratime/data/repo/DataIntegrityRepository.kt:85:    fun getIncompleteProfiles(type: String): Flow<List<FaceEntity>> = schoolIdFlow.flatMapLatest { id ->
- app/src/main/java/com/azuratech/azuratime/data/repo/MembershipRepository.kt:124:    fun observeMemberships(uid: String): Flow<List<com.azuratech.azuratime.data.local.Membership>> {
- app/src/main/java/com/azuratech/azuratime/data/repo/MembershipRepository.kt:131:    fun observeMembershipFlow(uid: String): Flow<MembershipDocUpdate> {

