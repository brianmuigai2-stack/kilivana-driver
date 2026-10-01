package com.example.kilivana_driver.ui.screens.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.kilivana_driver.data.model.Driver
import com.example.kilivana_driver.data.model.JobStatus
import com.example.kilivana_driver.ui.components.BottomTab
import com.example.kilivana_driver.ui.components.KilivanaBottomBar
import com.example.kilivana_driver.ui.screens.dashboard.DashboardScreen
import com.example.kilivana_driver.ui.screens.dashboard.DashboardUiState
import com.example.kilivana_driver.ui.screens.history.DeliveryHistoryScreen
import com.example.kilivana_driver.ui.screens.history.sampleDeliveryHistory
import com.example.kilivana_driver.ui.screens.jobs.JobDetailsScreen
import com.example.kilivana_driver.ui.screens.jobs.JobRouteScreen
import com.example.kilivana_driver.ui.screens.jobs.JobsScreen
import com.example.kilivana_driver.ui.screens.jobs.JobsUiState
import com.example.kilivana_driver.ui.screens.map.MapScreen
import com.example.kilivana_driver.ui.screens.notifications.NotificationsScreen
import com.example.kilivana_driver.ui.screens.notifications.NotificationsUiState
import com.example.kilivana_driver.data.model.AuthUser
import com.example.kilivana_driver.ui.screens.profile.PersonalInfoScreen
import com.example.kilivana_driver.ui.screens.profile.ProfileImagesScreen
import com.example.kilivana_driver.ui.screens.profile.ProfileScreen
import com.example.kilivana_driver.ui.screens.profile.ProfileUiState
import com.example.kilivana_driver.ui.screens.profile.VehicleDetailsScreen
import com.example.kilivana_driver.ui.screens.settings.AppLanguage
import com.example.kilivana_driver.ui.screens.settings.SettingsScreen
import com.example.kilivana_driver.ui.screens.settings.SettingsUiState
import com.example.kilivana_driver.ui.theme.KilivanaBackground

@Composable
fun MainScreen(
    dashboardState: DashboardUiState,
    jobsState: JobsUiState,
    driver: Driver,
    settingsState: SettingsUiState,
    notificationsState: NotificationsUiState,
    onJobStatusSelected: (JobStatus) -> Unit,
    onAcceptJob: (String) -> Unit,
    onCompleteJob: (String) -> Unit,
    onPushNotificationsChange: (Boolean) -> Unit,
    onNotificationSoundChange: (Boolean) -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onNotificationClick: (String) -> Unit,
    onMarkAllNotificationsRead: () -> Unit,
    onSendTestNotification: () -> Unit,
    onTestApiConnection: () -> Unit,
    onLogout: () -> Unit,
    profileUiState: ProfileUiState = ProfileUiState(),
    currentUser: AuthUser? = null,
    onPickImage: () -> Unit = {},
    onDeleteImage: (Long) -> Unit = {},
    onDismissUploadMessage: () -> Unit = {},
    onSetStatusBarDark: (Boolean) -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableStateOf(BottomTab.HOME) }
    var selectedJobId by rememberSaveable { mutableStateOf<String?>(null) }
    var activeTripJobId by rememberSaveable { mutableStateOf<String?>(null) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showNotifications by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showPersonalInfo by rememberSaveable { mutableStateOf(false) }
    var showVehicleDetails by rememberSaveable { mutableStateOf(false) }
    var showProfileImages by rememberSaveable { mutableStateOf(false) }
    var tabBeforeHistory by rememberSaveable { mutableStateOf(BottomTab.HOME) }

    // TODO: replace with real delivery history from a ViewModel / the API
    val historyRecords = remember { sampleDeliveryHistory() }

    val selectedJob = selectedJobId?.let { id -> jobsState.jobs.firstOrNull { it.id == id } }
    val activeTripJob = activeTripJobId?.let { id -> jobsState.jobs.firstOrNull { it.id == id } }
    val acceptedJob = jobsState.jobs.firstOrNull { it.status == JobStatus.ACCEPTED }

    val closeHistory: () -> Unit = {
        selectedTab = tabBeforeHistory
        showHistory = false
    }

    BackHandler(enabled = activeTripJob != null) { activeTripJobId = null }
    BackHandler(enabled = selectedJob != null && activeTripJob == null) { selectedJobId = null }
    BackHandler(enabled = showSettings && selectedJob == null && activeTripJob == null) { showSettings = false }
    BackHandler(
        enabled = showNotifications && !showSettings && selectedJob == null && activeTripJob == null
    ) { showNotifications = false }
    BackHandler(
        enabled = showHistory && !showNotifications && !showSettings &&
            selectedJob == null && activeTripJob == null
    ) { closeHistory() }

    BackHandler(enabled = showProfileImages) { showProfileImages = false }
    BackHandler(enabled = showVehicleDetails && !showProfileImages) { showVehicleDetails = false }
    BackHandler(enabled = showPersonalInfo && !showVehicleDetails && !showProfileImages) {
        showPersonalInfo = false
    }

    val isMapLikeVisible = (selectedTab == BottomTab.MAP || activeTripJob != null) &&
        !showSettings && !showNotifications && selectedJob == null
    LaunchedEffect(isMapLikeVisible) { onSetStatusBarDark(!isMapLikeVisible) }

    when {
        showProfileImages -> ProfileImagesScreen(
            images = profileUiState.images,
            isLoading = profileUiState.isLoading,
            isUploading = profileUiState.isUploading,
            isDeleting = profileUiState.isDeletingImage,
            onPickImage = onPickImage,
            onDeleteImage = { onDeleteImage(it.id) },
            onBack = { showProfileImages = false }
        )

        showVehicleDetails -> VehicleDetailsScreen(
            profile = profileUiState.profile,
            isLoading = profileUiState.isLoading,
            needsProfile = profileUiState.needsProfile,
            onBack = { showVehicleDetails = false },
            onManageImagesClick = { showProfileImages = true }
        )

        showPersonalInfo -> PersonalInfoScreen(
            user = currentUser,
            onBack = { showPersonalInfo = false }
        )

        activeTripJob != null -> JobRouteScreen(
            job = activeTripJob,
            onBack = { activeTripJobId = null },
            onDeliveryComplete = {
                onCompleteJob(activeTripJob.id)
                activeTripJobId = null
            }
        )

        showSettings -> SettingsScreen(
            uiState = settingsState,
            onBack = { showSettings = false },
            onPushNotificationsChange = onPushNotificationsChange,
            onNotificationSoundChange = onNotificationSoundChange,
            onDarkModeChange = onDarkModeChange,
            onLanguageSelected = onLanguageSelected,
            onChangePasswordClick = { /* TODO */ },
            onTermsClick = { /* TODO */ },
            onPrivacyClick = { /* TODO */ },
            onAboutClick = { /* TODO */ },
            onSendTestNotification = onSendTestNotification,
            onTestApiConnection = onTestApiConnection,
            onLogout = onLogout
        )

        showNotifications -> NotificationsScreen(
            uiState = notificationsState,
            onBack = { showNotifications = false },
            onNotificationClick = onNotificationClick,
            onMarkAllRead = onMarkAllNotificationsRead
        )

        selectedJob != null -> JobDetailsScreen(
            job = selectedJob,
            onBack = { selectedJobId = null },
            onAccept = {
                onAcceptJob(selectedJob.id)
                activeTripJobId = selectedJob.id
                selectedJobId = null
            },
            onDecline = {
                // TODO: tell the API the driver declined this job
                selectedJobId = null
            },
            onStartTrip = {
                activeTripJobId = selectedJob.id
                selectedJobId = null
            }
        )

        else -> Scaffold(
            containerColor = KilivanaBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                KilivanaBottomBar(
                    selected = selectedTab,
                    onTabSelected = { tab ->
                        showHistory = false
                        selectedTab = tab
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                if (showHistory) {
                    DeliveryHistoryScreen(
                        records = historyRecords,
                        onBack = closeHistory
                    )
                } else {
                    when (selectedTab) {
                        BottomTab.HOME -> DashboardScreen(
                            uiState = dashboardState.copy(
                                hasUnreadNotifications = notificationsState.unreadCount > 0
                            ),
                            onNotificationsClick = { showNotifications = true },
                            onMyJobsClick = { selectedTab = BottomTab.JOBS },
                            onMapRouteClick = { selectedTab = BottomTab.MAP },
                            onHistoryClick = {
                                tabBeforeHistory = selectedTab
                                selectedTab = BottomTab.JOBS
                                showHistory = true
                            },
                            onProfileClick = { selectedTab = BottomTab.MORE }
                        )
                        BottomTab.JOBS -> JobsScreen(
                            uiState = jobsState,
                            onStatusSelected = onJobStatusSelected,
                            onFilterClick = { /* TODO: filter sheet */ },
                            onJobClick = { job -> selectedJobId = job.id }
                        )
                        BottomTab.MAP -> MapScreen(
                            activeJob = acceptedJob,
                            onResumeActiveJob = {
                                acceptedJob?.let { activeTripJobId = it.id }
                            }
                        )
                        BottomTab.MORE -> ProfileScreen(
                            driver = driver,
                            uiState = profileUiState,
                            onPickImage = onPickImage,
                            onDismissUploadMessage = onDismissUploadMessage,
                            onSettingsClick = { showSettings = true },
                            onPersonalInfoClick = { showPersonalInfo = true },
                            onVehicleDetailsClick = { showVehicleDetails = true },
                            onBankDetailsClick = { /* TODO */ },
                            onChangePasswordClick = { /* TODO */ },
                            onNotificationsClick = { showNotifications = true },
                            onHelpClick = { /* TODO */ },
                            onLogout = onLogout
                        )
                    }
                }
            }
        }
    }
}
