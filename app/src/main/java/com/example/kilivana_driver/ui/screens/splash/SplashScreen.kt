package com.example.kilivana_driver.ui.screens.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kilivana_driver.R
import com.example.kilivana_driver.ui.theme.KilivanaGreenDark
import com.example.kilivana_driver.ui.theme.KilivanaTheme
import com.example.kilivana_driver.ui.theme.KilivanaWhite
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit = {}) {
    LaunchedEffect(Unit) {
        delay(2500)
        onFinished()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background photo
        Image(
            painter = painterResource(R.drawable.splashscreen),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Dark green overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            KilivanaGreenDark.copy(alpha = 0.80f),
                            KilivanaGreenDark.copy(alpha = 0.55f),
                            KilivanaGreenDark.copy(alpha = 0.80f)
                        )
                    )
                )
        )

        // Centre content
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LogoPill()
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "Farm to Market",
                color = KilivanaWhite,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Delivering fresh produce and essential inputs across Kenya",
                color = KilivanaWhite.copy(alpha = 0.9f),
                fontSize = 16.sp,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center
            )
        }

        // Bottom: dots + loading
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PageDots(activeIndex = 0, count = 3)
            Spacer(modifier = Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = KilivanaWhite,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Loading your journey...",
                    color = KilivanaWhite,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun LogoPill() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(KilivanaWhite.copy(alpha = 0.15f))
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.kilivana_logo),
            contentDescription = "Kilivana logo",
            modifier = Modifier.height(120.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun PageDots(activeIndex: Int, count: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        KilivanaWhite.copy(alpha = if (index == activeIndex) 1f else 0.5f)
                    )
            )
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun SplashScreenPreview() {
    KilivanaTheme {
        SplashScreen()
    }
}
