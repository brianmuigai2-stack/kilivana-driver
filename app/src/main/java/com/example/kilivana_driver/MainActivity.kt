package com.example.kilivana_driver

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kilivana_driver.data.network.SessionStore
import com.example.kilivana_driver.notifications.NotificationHelper
import com.example.kilivana_driver.ui.screens.dashboard.DashboardViewModel
import com.example.kilivana_driver.ui.screens.jobs.JobsViewModel
import com.example.kilivana_driver.ui.screens.login.LoginScreen
import com.example.kilivana_driver.ui.screens.login.LoginViewModel
import com.example.kilivana_driver.ui.screens.inspector.InspectorLoginScreen
import com.example.kilivana_driver.ui.screens.inspector.InspectorLoginViewModel
import com.example.kilivana_driver.ui.screens.inspector.InspectorWelcomeScreen
import com.example.kilivana_driver.ui.screens.inspector.InspectorShell
import com.example.kilivana_driver.ui.screens.main.MainScreen
import com.example.kilivana_driver.ui.screens.notifications.NotificationsViewModel
import com.example.kilivana_driver.ui.screens.profile.ProfileViewModel
import com.example.kilivana_driver.ui.screens.settings.SettingsViewModel
import com.example.kilivana_driver.ui.screens.splash.SplashScreen
import com.example.kilivana_driver.ui.theme.KilivanaGreen
import com.example.kilivana_driver.ui.theme.KilivanaGreenCard
import com.example.kilivana_driver.ui.theme.KilivanaGreenDark
import com.example.kilivana_driver.ui.theme.KilivanaTextMuted
import com.example.kilivana_driver.ui.theme.KilivanaTextPrimary
import com.example.kilivana_driver.ui.theme.KilivanaTheme
import com.example.kilivana_driver.ui.theme.KilivanaWhite
import com.yalantis.ucrop.UCrop
import java.io.File

/**
 * Single activity hosting the whole app.
 *
 * Owns all the feature ViewModels, drives the top-level screen switch
 * (splash → login → main), and applies the system-bar styling — the
 * status bar scrim and icon colour have to change together, so the
 * composables call back into [setSystemBars] rather than each painting
 * the bar themselves. Also hosts the UCrop launcher that square-crops
 * profile photos before they are uploaded.
 */
class MainActivity : ComponentActivity() {

    private val loginViewModel: LoginViewModel by viewModels()
    private val inspectorViewModel: InspectorLoginViewModel by viewModels()
    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val jobsViewModel: JobsViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val notificationsViewModel: NotificationsViewModel by viewModels()

    /** The signed-in user, so the personal info screen renders live API data. */
    private val sessionStoreUser = SessionStore.currentUser

    /** Initializes system bars and notification channels, then hosts the app navigation and photo picker. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSystemBars(darkHeader = true)
        NotificationHelper.ensureChannels(this)

        setContent {
            val settingsState by settingsViewModel.uiState.collectAsState()

            KilivanaTheme(darkTheme = settingsState.darkModeEnabled) {
                var showSplash by rememberSaveable { mutableStateOf(true) }
                var showRoleChoice by rememberSaveable { mutableStateOf(false) }
                var showInspectorLogin by rememberSaveable { mutableStateOf(false) }
                var showInspectorWelcome by rememberSaveable { mutableStateOf(false) }
                var inspectorUserName by rememberSaveable { mutableStateOf("") }
                val loginState by loginViewModel.uiState.collectAsState()
                val isSignedIn by loginViewModel.isSignedIn.collectAsState()
                val restoreComplete by loginViewModel.restoreComplete.collectAsState()
                val inspectorUiState by inspectorViewModel.uiState.collectAsState()
                val dashboardState by dashboardViewModel.uiState.collectAsState()
                val jobsState by jobsViewModel.uiState.collectAsState()
                val driver by profileViewModel.driver.collectAsState()
                val profileState by profileViewModel.uiState.collectAsState()
                val currentUser by sessionStoreUser.collectAsState()
                val notificationsState by notificationsViewModel.uiState.collectAsState()

                // The header avatar reads the driver's photo URL, which lives on
                // the profile (not the dashboard view model), so fold it in here
                // rather than making the dashboard reach into profile state.
                val dashboardStateWithPhoto = dashboardState.copy(
                    photoUrl = driver.photoUrl
                )

                val cropDest = remember {
                    Uri.fromFile(File(cacheDir, "profile_crop.jpg"))
                }

                val cropLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    val cropped = UCrop.getOutput(result.data ?: return@rememberLauncherForActivityResult)
                    if (result.resultCode == RESULT_OK && cropped != null) {
                        profileViewModel.uploadImage(cropped)
                    }
                }

                val pickImage = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia()
                ) { uri ->
                    if (uri != null) {
                        val intent = UCrop.of(uri, cropDest)
                            .withAspectRatio(1f, 1f)
                            .withMaxResultSize(800, 800)
                            .getIntent(this@MainActivity)
                        cropLauncher.launch(intent)
                    }
                }

                // Re-apply whenever either input changes, so the scrim tracks the
                // header as the user moves between screens.
                LaunchedEffect(
                    showSplash,
                    isSignedIn,
                    showInspectorLogin,
                    showInspectorWelcome,
                    currentUser?.role
                ) {
                    val inspectorScreen = showInspectorLogin || showInspectorWelcome ||
                        (isSignedIn && currentUser?.role == "INSPECTOR")
                    if (inspectorScreen) {
                        setSystemBars(darkHeader = true, greenBand = true)
                    } else if (showSplash || !isSignedIn) {
                        setSystemBars(darkHeader = true, greenBand = false)
                    }
                }

                // After the splash (and any remembered-session restore) finishes,
                // if nobody is signed in, ask who wants to log in: driver or
                // inspector. This keeps the two roles completely separate.
                LaunchedEffect(showSplash, restoreComplete, isSignedIn) {
                    if (!showSplash && restoreComplete && !isSignedIn) {
                        showRoleChoice = true
                    }
                }

                // When an inspector finishes logging in, move to the welcome
                // screen. Inspectors never reach the driver dashboard.
                LaunchedEffect(inspectorUiState.isLoggedIn) {
                    if (inspectorUiState.isLoggedIn) {
                        inspectorViewModel.signedInUser()?.let { user ->
                            inspectorUserName = user.name.takeIf { it.isNotBlank() } ?: "Inspector"
                            showInspectorLogin = false
                            showInspectorWelcome = true
                        }
                    }
                }

                val userRole = currentUser?.role.orEmpty()

                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        showSplash -> SplashScreen(onFinished = { showSplash = false })

                        // Hold the splash until the remembered-session check finishes,
                        // otherwise a returning driver sees a flash of the login screen.
                        !restoreComplete -> SplashScreen(onFinished = { showSplash = false })

                        showInspectorLogin -> InspectorLoginScreen(
                            uiState = inspectorUiState,
                            onEmailChange = inspectorViewModel::onEmailChange,
                            onPasswordChange = inspectorViewModel::onPasswordChange,
                            onTogglePasswordVisibility = inspectorViewModel::onTogglePasswordVisibility,
                            onLoginClick = inspectorViewModel::onLoginClick,
                            onBackToDriverLogin = {
                                showInspectorLogin = false
                                showRoleChoice = true
                            }
                        )

                        showInspectorWelcome -> InspectorShell(
                            onLogout = {
                                showInspectorWelcome = false
                                inspectorViewModel.onLogout()
                                showRoleChoice = true
                            }
                        )

                        // When a role-choice popup is open, show nothing behind it
                        // so the two roles stay strictly separated.
                        showRoleChoice -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(KilivanaGreenDark),
                            contentAlignment = Alignment.Center
                        ) {
                            RoleChoicePopup(
                                onDismiss = { showRoleChoice = false },
                                onDriver = { showRoleChoice = false },
                                onInspector = {
                                    showRoleChoice = false
                                    showInspectorLogin = true
                                }
                            )
                        }

                        // Only drivers reach the driver dashboard. Inspectors stay
                        // on their own welcome screen and never see driver tabs.
                        isSignedIn && !showRoleChoice && userRole == "DRIVER" -> MainScreen(
                            dashboardState = dashboardStateWithPhoto,
                            jobsState = jobsState,
                            driver = driver,
                            settingsState = settingsState,
                            notificationsState = notificationsState,
                             onJobStatusSelected = jobsViewModel::onStatusSelected,
                             onAcceptJob = jobsViewModel::acceptJob,
                             onDeclineJob = jobsViewModel::declineJob,
                             onCompleteJob = jobsViewModel::completeJob,
                             onRefreshJobs = jobsViewModel::refresh,
                            onPushNotificationsChange = settingsViewModel::onPushNotificationsChange,
                            onNotificationSoundChange = settingsViewModel::onNotificationSoundChange,
                            onDarkModeChange = settingsViewModel::onDarkModeChange,
                            onLanguageSelected = settingsViewModel::onLanguageSelected,
                            onNotificationClick = notificationsViewModel::markRead,
                            onMarkAllNotificationsRead = notificationsViewModel::markAllRead,
                            onSendTestNotification = {
                                NotificationHelper.postJobAlert(
                                    context = this@MainActivity,
                                    title = "New job available",
                                    message = "This is a test notification from Kilivana Driver.",
                                    pushEnabled = settingsState.pushNotificationsEnabled,
                                    soundEnabled = settingsState.notificationSoundEnabled
                                )
                            },
                            onTestApiConnection = settingsViewModel::testApiConnection,
                            onLogout = { loginViewModel.onLogout(); showRoleChoice = true },
                            profileUiState = profileState,
                            currentUser = currentUser,
                            onPickImage = {
                                pickImage.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onDeleteImage = profileViewModel::deleteImage,
                            onDeletePhoto = profileViewModel::deletePrimaryPhoto,
                            onDismissUploadMessage = profileViewModel::dismissMessage,
                            onSetSystemBars = { dark, green -> setSystemBars(dark, green) }
                        )

                        isSignedIn && !showRoleChoice && userRole == "INSPECTOR" -> InspectorShell(
                            onLogout = {
                                inspectorViewModel.onLogout()
                                showRoleChoice = true
                            }
                        )

                        else -> LoginScreen(
                            uiState = loginState,
                            onEmailChange = loginViewModel::onEmailChange,
                            onPasswordChange = loginViewModel::onPasswordChange,
                            onTogglePasswordVisibility = loginViewModel::onTogglePasswordVisibility,
                            onRememberMeChange = loginViewModel::onRememberMeChange,
                            onLoginClick = loginViewModel::onLoginClick,
                            onForgotPassword = loginViewModel::onForgotPasswordClick,
                            onForgotPasswordEmailChange = loginViewModel::onForgotPasswordEmailChange,
                            onForgotPasswordSubmit = loginViewModel::onForgotPasswordSubmit,
                            onForgotPasswordDismiss = loginViewModel::onForgotPasswordDismiss,
                            onContactSupport = { /* TODO: open dialer or WhatsApp to dispatch */ },
                            onInspectorLoginClick = {
                                showRoleChoice = false
                                showInspectorLogin = true
                            }
                        )
                    }
                }
            }
        }
    }

    /**
     * Configures the system bars.
     *
     * [greenBand] must be true whenever a [com.example.kilivana_driver.ui.components.KilivanaHeader]
     * is on screen. The status bar scrim is then set to the same green as the
     * band, because on API < 29 the scrim is what actually paints the bar — with
     * a transparent scrim the status bar falls back to the window background and
     * the band looks like it stops short of the top of the screen. Newer
     * versions ignore the scrim and let the band show through, so setting it
     * makes both cases agree.
     */
    private fun setSystemBars(darkHeader: Boolean, greenBand: Boolean = false) {
        val scrim = if (greenBand) KilivanaGreenCard.toArgb() else Color.TRANSPARENT
        enableEdgeToEdge(
            statusBarStyle = if (darkHeader) {
                SystemBarStyle.dark(scrim)
            } else {
                SystemBarStyle.light(scrim, scrim)
            },
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
    }
}

/**
 * Popup shown after the splash when nobody is signed in. Asks the user who they
 * want to log in as — driver or inspector — keeping the two roles strictly
 * separated so an inspector can never reach the driver dashboard.
 */
@Composable
fun RoleChoicePopup(
    onDismiss: () -> Unit,
    onDriver: () -> Unit,
    onInspector: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(0.88f)
            .background(ComposeColor.White)
            .clip(RoundedCornerShape(16.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
            Text(
                text = "Log in as",
                color = KilivanaTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Choose who you want to log in as.",
                color = KilivanaTextMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onInspector,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KilivanaGreen,
                    contentColor = ComposeColor.White
                )
            ) {
                Text(
                    text = "Inspector",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onDriver,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KilivanaGreen,
                    contentColor = ComposeColor.White
                )
            ) {
                Text(
                    text = "Driver",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
}