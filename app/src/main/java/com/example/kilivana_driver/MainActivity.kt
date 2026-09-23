package com.example.kilivana_driver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.kilivana_driver.ui.splash.KilivanaSplashScreen
import com.example.kilivana_driver.ui.theme.KilivanadriverTheme
import com.kilivana.driver.ui.screens.dashboard.DriverDashboard
import com.kilivana.driver.ui.screens.login.LoginScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KilivanadriverTheme {
                KilivanaApp(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun KilivanaApp(
    modifier: Modifier = Modifier
) {
    // Simple UI-only state machine: splash -> login -> dashboard.
    // No navigation component, no auth logic, no network code.
    var showLogin by remember { mutableStateOf(false) }
    var showDashboard by remember { mutableStateOf(false) }

    when {
        showDashboard -> {
            DriverDashboard(modifier = modifier)
        }
        showLogin -> {
            LoginScreen(
                modifier = modifier,
                onLoginSuccess = { showDashboard = true }
            )
        }
        else -> {
            KilivanaSplashScreen(
                modifier = modifier,
                onSplashFinished = { showLogin = true }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainActivityPreview() {
    KilivanadriverTheme {
        KilivanaApp(
            modifier = Modifier.fillMaxSize()
        )
    }
}