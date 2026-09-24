package com.example.kilivana_driver

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.kilivana_driver.ui.screens.dashboard.DashboardViewModel
import com.example.kilivana_driver.ui.screens.jobs.JobsViewModel
import com.example.kilivana_driver.ui.screens.login.LoginScreen
import com.example.kilivana_driver.ui.screens.login.LoginViewModel
import com.example.kilivana_driver.ui.screens.main.MainScreen
import com.example.kilivana_driver.ui.screens.splash.SplashScreen
import com.example.kilivana_driver.ui.theme.KilivanaTheme

class MainActivity : ComponentActivity() {

    private val loginViewModel: LoginViewModel by viewModels()
    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val jobsViewModel: JobsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSystemBars(darkHeader = true)
        setContent {
            KilivanaTheme {
                var showSplash by rememberSaveable { mutableStateOf(true) }
                val loginState by loginViewModel.uiState.collectAsState()
                val dashboardState by dashboardViewModel.uiState.collectAsState()
                val jobsState by jobsViewModel.uiState.collectAsState()

                // Only the splash sits on a dark photo (white status icons);
                // login and the main app are light (dark status icons).
                LaunchedEffect(showSplash) { setSystemBars(darkHeader = showSplash) }

                when {
                    showSplash -> SplashScreen(onFinished = { showSplash = false })

                    loginState.isLoggedIn -> MainScreen(
                        dashboardState = dashboardState,
                        jobsState = jobsState,
                        onJobStatusSelected = jobsViewModel::onStatusSelected,
                        onAcceptJob = jobsViewModel::acceptJob
                    )

                    else -> LoginScreen(
                        uiState = loginState,
                        onPhoneChange = loginViewModel::onPhoneChange,
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

    private fun setSystemBars(darkHeader: Boolean) {
        enableEdgeToEdge(
            statusBarStyle = if (darkHeader) {
                SystemBarStyle.dark(Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
            },
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
    }
}
