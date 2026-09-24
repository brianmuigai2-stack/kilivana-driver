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
import com.example.kilivana_driver.data.model.JobStatus
import com.example.kilivana_driver.ui.components.BottomTab
import com.example.kilivana_driver.ui.components.KilivanaBottomBar
import com.example.kilivana_driver.ui.screens.dashboard.DashboardScreen
import com.example.kilivana_driver.ui.screens.dashboard.DashboardUiState
import com.example.kilivana_driver.ui.screens.jobs.JobDetailsScreen
import com.example.kilivana_driver.ui.screens.jobs.JobsScreen
import com.example.kilivana_driver.ui.screens.jobs.JobsUiState
import com.example.kilivana_driver.ui.theme.KilivanaBackground
import com.example.kilivana_driver.ui.theme.KilivanaTextMuted

@Composable
fun MainScreen(
    dashboardState: DashboardUiState,
    jobsState: JobsUiState,
    onJobStatusSelected: (JobStatus) -> Unit,
    onAcceptJob: (String) -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(BottomTab.HOME) }
    var selectedJobId by rememberSaveable { mutableStateOf<String?>(null) }

    val selectedJob = selectedJobId?.let { id ->
        jobsState.jobs.firstOrNull { it.id == id }
    }

    // System back closes the job details first
    BackHandler(enabled = selectedJob != null) { selectedJobId = null }

    if (selectedJob != null) {
        JobDetailsScreen(
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
    } else {
        Scaffold(
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
                        onProfileClick = { /* TODO */ }
                    )
                    BottomTab.JOBS -> JobsScreen(
                        uiState = jobsState,
                        onStatusSelected = onJobStatusSelected,
                        onFilterClick = { /* TODO: filter sheet */ },
                        onJobClick = { job -> selectedJobId = job.id }
                    )
                    BottomTab.MAP -> ComingSoon("Map")
                    BottomTab.MORE -> ComingSoon("More")
                }
            }
        }
    }
}

@Composable
private fun ComingSoon(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$title coming soon 🚧",
            color = KilivanaTextMuted,
            fontSize = 16.sp
        )
    }
}
