package com.example.kilivana_driver

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import com.example.kilivana_driver.data.network.SessionStore
import com.example.kilivana_driver.notifications.NotificationHelper
import com.example.kilivana_driver.ui.screens.dashboard.DashboardViewModel
import com.example.kilivana_driver.ui.screens.jobs.JobsViewModel
import com.example.kilivana_driver.ui.screens.login.LoginScreen
import com.example.kilivana_driver.ui.screens.login.LoginViewModel
import com.example.kilivana_driver.ui.screens.main.MainScreen
import com.example.kilivana_driver.ui.screens.notifications.NotificationsViewModel
import com.example.kilivana_driver.ui.screens.profile.ProfileViewModel
import com.example.kilivana_driver.ui.screens.settings.SettingsViewModel
import com.example.kilivana_driver.ui.screens.splash.SplashScreen
import com.example.kilivana_driver.ui.theme.KilivanaGreenCard
import com.example.kilivana_driver.ui.theme.KilivanaTheme

class MainActivity : ComponentActivity() {

    private val loginViewModel: LoginViewModel by viewModels()
    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val jobsViewModel: JobsViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val notificationsViewModel: NotificationsViewModel by viewModels()

    /** The signed-in user, so the personal info screen renders live API data. */
    private val sessionStoreUser = SessionStore.currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSystemBars(darkHeader = true)
        NotificationHelper.ensureChannels(this)

        setContent {
            val settingsState by settingsViewModel.uiState.collectAsState()

            KilivanaTheme(darkTheme = settingsState.darkModeEnabled) {
                var showSplash by rememberSaveable { mutableStateOf(true) }
                val loginState by loginViewModel.uiState.collectAsState()
                val isSignedIn by loginViewModel.isSignedIn.collectAsState()
                val restoreComplete by loginViewModel.restoreComplete.collectAsState()
                val dashboardState by dashboardViewModel.uiState.collectAsState()
                val jobsState by jobsViewModel.uiState.collectAsState()
                val driver by profileViewModel.driver.collectAsState()
                val profileState by profileViewModel.uiState.collectAsState()
                val currentUser by sessionStoreUser.collectAsState()
                val notificationsState by notificationsViewModel.uiState.collectAsState()

                val pickImage = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia()
                ) { uri ->
                    if (uri != null) profileViewModel.uploadImage(uri)
                }

                // Re-apply whenever either input changes, so the scrim tracks the
                // header as the user moves between screens.
                LaunchedEffect(showSplash, isSignedIn) {
                    if (showSplash || !isSignedIn) {
                        setSystemBars(darkHeader = true, greenBand = false)
                    }
                }

                when {
                    showSplash -> SplashScreen(onFinished = { showSplash = false })

                    isSignedIn -> MainScreen(
                        dashboardState = dashboardState,
                        jobsState = jobsState,
                        driver = driver,
                        settingsState = settingsState,
                        notificationsState = notificationsState,
                        onJobStatusSelected = jobsViewModel::onStatusSelected,
                        onAcceptJob = jobsViewModel::acceptJob,
                        onCompleteJob = jobsViewModel::completeJob,
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
                        onLogout = loginViewModel::onLogout,
                        profileUiState = profileState,
                        currentUser = currentUser,
                        onPickImage = {
                            pickImage.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onDeleteImage = profileViewModel::deleteImage,
                        onDismissUploadMessage = profileViewModel::dismissMessage,
                        onSetSystemBars = { dark, green -> setSystemBars(dark, green) }
                    )

                    // Hold the splash until the remembered-session check finishes,
                    // otherwise a returning driver sees a flash of the login screen.
                    !restoreComplete -> SplashScreen(onFinished = { showSplash = false })

                    else -> LoginScreen(
                        uiState = loginState,
                        onEmailChange = loginViewModel::onEmailChange,
                        onPasswordChange = loginViewModel::onPasswordChange,
                        onTogglePasswordVisibility = loginViewModel::onTogglePasswordVisibility,
                        onRememberMeChange = loginViewModel::onRememberMeChange,
                        onLoginClick = loginViewModel::onLoginClick,
                        onForgotPassword = { /* TODO: tell the driver to ask their admin for a reset */ },
                        onContactSupport = { /* TODO: open dialer or WhatsApp to dispatch */ }
                    )
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
