package com.example.kilivana_driver.ui.screens.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.example.kilivana_driver.data.model.Driver
import com.example.kilivana_driver.data.model.JobStatus
import com.example.kilivana_driver.ui.components.BottomTab
import com.example.kilivana_driver.ui.components.KilivanaBottomBar
import com.example.kilivana_driver.ui.screens.dashboard.DashboardScreen
import com.example.kilivana_driver.ui.screens.dashboard.DashboardUiState
import com.example.kilivana_driver.ui.screens.jobs.JobDetailsScreen
import com.example.kilivana_driver.ui.screens.jobs.JobsScreen
import com.example.kilivana_driver.ui.screens.jobs.JobsUiState
import com.example.kilivana_driver.ui.screens.map.MapScreen
import com.example.kilivana_driver.ui.screens.profile.ProfileScreen
import com.example.kilivana_driver.ui.screens.settings.AppLanguage
import com.example.kilivana_driver.ui.screens.settings.SettingsScreen
import com.example.kilivana_driver.ui.screens.settings.SettingsUiState
import com.example.kilivana_driver.ui.theme.KilivanaBackground
import com.example.kilivana_driver.ui.theme.KilivanaTextMuted

@Composable
fun MainScreen(
    dashboardState: DashboardUiState,
    jobsState: JobsUiState,
    driver: Driver,
    settingsState: SettingsUiState,
    onJobStatusSelected: (JobStatus) -> Unit,
    onAcceptJob: (String) -> Unit,
    onPushNotificationsChange: (Boolean) -> Unit,
    onNotificationSoundChange: (Boolean) -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(BottomTab.HOME) }
    var selectedJobId by rememberSaveable { mutableStateOf<String?>(null) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    val selectedJob = selectedJobId?.let { id ->
        jobsState.jobs.firstOrNull { it.id == id }
    }

    // System back closes whichever overlay is open first
    BackHandler(enabled = selectedJob != null) { selectedJobId = null }
    BackHandler(enabled = showSettings && selectedJob == null) { showSettings = false }

    when {
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
            onLogout = onLogout
        )

        selectedJob != null -> JobDetailsScreen(
            job = selectedJob,
            onBack = { selectedJobId = null },
            onAccept = {
                onAcceptJob(selectedJob.id)
                selectedJobId = null
            },
            onDecline = {
                // TODO: tell the API the driver declined this job
                selectedJobId = null
            },
            onStartTrip = { /* TODO: open the En Route screen */ }
        )

        else -> Scaffold(
            containerColor = KilivanaBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                KilivanaBottomBar(
                    selected = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                when (selectedTab) {
                    BottomTab.HOME -> DashboardScreen(
                        uiState = dashboardState,
                        onNotificationsClick = { /* TODO */ },
                        onMyJobsClick = { selectedTab = BottomTab.JOBS },
                        onMapRouteClick = { selectedTab = BottomTab.MAP },
                        onHistoryClick = { /* TODO */ },
                        onProfileClick = { selectedTab = BottomTab.MORE }
                    )
                    BottomTab.JOBS -> JobsScreen(
                        uiState = jobsState,
                        onStatusSelected = onJobStatusSelected,
                        onFilterClick = { /* TODO: filter sheet */ },
                        onJobClick = { job -> selectedJobId = job.id }
                    )
                    BottomTab.MAP -> MapScreen()
                    BottomTab.MORE -> ProfileScreen(
                        driver = driver,
                        onSettingsClick = { showSettings = true },
                        onPersonalInfoClick = { /* TODO */ },
                        onVehicleDetailsClick = { /* TODO */ },
                        onBankDetailsClick = { /* TODO */ },
                        onChangePasswordClick = { /* TODO */ },
                        onNotificationsClick = { /* TODO */ },
                        onHelpClick = { /* TODO */ },
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}
