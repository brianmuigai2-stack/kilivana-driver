package com.example.kilivana_driver.ui.screens.main

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
import com.example.kilivana_driver.ui.components.BottomTab
import com.example.kilivana_driver.ui.components.KilivanaBottomBar
import com.example.kilivana_driver.ui.screens.dashboard.DashboardScreen
import com.example.kilivana_driver.ui.screens.dashboard.DashboardUiState
import com.example.kilivana_driver.ui.theme.KilivanaBackground
import com.example.kilivana_driver.ui.theme.KilivanaTextMuted

@Composable
fun MainScreen(dashboardState: DashboardUiState) {
    var selectedTab by rememberSaveable { mutableStateOf(BottomTab.HOME) }

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
                BottomTab.JOBS -> ComingSoon("Jobs")
                BottomTab.MAP -> ComingSoon("Map")
                BottomTab.MORE -> ComingSoon("More")
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
