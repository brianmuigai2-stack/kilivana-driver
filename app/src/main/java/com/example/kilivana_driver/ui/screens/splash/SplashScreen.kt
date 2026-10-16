package com.example.kilivana_driver.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onFinished: () -> Unit = {}) {

    val photoScale   = remember { Animatable(1f) }
    val logoAlpha    = remember { Animatable(0f) }
    val logoScale    = remember { Animatable(0.75f) }
    val textAlpha    = remember { Animatable(0f) }
    val textSlide    = remember { Animatable(20f) }
    val progress     = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Slow photo zoom
        launch {
            photoScale.animateTo(1.10f, tween(3000, easing = LinearEasing))
        }
        // Logo fades + scales in
        launch {
            logoAlpha.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        }
        launch {
            logoScale.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        }
        // Text slides up after logo
        delay(450)
        launch {
            textAlpha.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        }
        launch {
            textSlide.animateTo(0f, tween(500, easing = FastOutSlowInEasing))
        }
        // Progress bar
        launch {
            progress.animateTo(1f, tween(2400, easing = LinearEasing))
        }

        delay(2700)
        onFinished()
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // Full-screen background photo with slow zoom
        Image(
            painter = painterResource(R.drawable.splashscreen),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = photoScale.value
                    scaleY = photoScale.value
                },
            contentScale = ContentScale.Crop
        )

        // Dark green gradient overlay — heavier at top and bottom, lighter in middle
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            KilivanaGreenDark.copy(alpha = 0.88f),
                            KilivanaGreenDark.copy(alpha = 0.45f),
                            KilivanaGreenDark.copy(alpha = 0.92f)
                        )
                    )
                )
        )

        // Center: logo + tagline
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Circular logo badge
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = logoAlpha.value
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                    }
                    .shadow(16.dp, CircleShape)
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.kilivana_logo),
                    contentDescription = "Kilivana logo",
                    modifier = Modifier.size(72.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Tagline
            Column(
                modifier = Modifier.graphicsLayer {
                    alpha = textAlpha.value
                    translationY = textSlide.value.dp.toPx()
                },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "KILIVANA DRIVER",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.5.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Farm to Market.",
                    color = KilivanaWhite,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Delivering fresh produce and essential\ninputs across Kenya.",
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Bottom: slim progress bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 40.dp, end = 40.dp, bottom = 52.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Loading your journey...",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 12.sp,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.18f))
            ) {
                // Fill
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.value)
                        .height(3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White)
                )
            }
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
