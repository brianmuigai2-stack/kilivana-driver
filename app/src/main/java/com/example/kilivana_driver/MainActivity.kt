package com.example.kilivana_driver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.kilivana_driver.ui.splash.KilivanaSplashScreen
import com.example.kilivana_driver.ui.theme.KilivanadriverTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KilivanadriverTheme {
                KilivanaSplashScreen(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainActivityPreview() {
    KilivanadriverTheme {
        KilivanaSplashScreen(
            modifier = Modifier.fillMaxSize()
        )
    }
}