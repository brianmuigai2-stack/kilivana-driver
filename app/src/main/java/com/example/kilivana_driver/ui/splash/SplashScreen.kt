package com.example.kilivana_driver.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kilivana_driver.R
import com.example.kilivana_driver.ui.theme.Green700
import com.example.kilivana_driver.ui.theme.KilivanadriverTheme
import com.kilivana.driver.ui.components.ScreenScaffold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun KilivanaSplashScreen(
    modifier: Modifier = Modifier,
    onSplashFinished: () -> Unit = {}
) {
    val truckAlpha = remember { Animatable(1f) }
    val logoScale = remember { Animatable(0.4f) }
    val logoAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // The full-screen truck image stays on screen first.
        delay(1200)
        // Then it fades out...
        truckAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 600, easing = EaseOut)
        )
        // ...and the brand logo + name pop in.
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 700, easing = EaseOut)
            )
        }
        launch {
            logoAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 700, easing = EaseIn)
            )
        }
        delay(800)
        onSplashFinished()
    }

    ScreenScaffold(
        modifier = modifier,
        containerColor = Green700
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Full-screen truck image, shown first.
            Image(
                painter = painterResource(id = R.drawable.splashscreen),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(truckAlpha.value)
            )

            // Branded overlay, popped in after the image clears.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(logoAlpha.value)
                    .scale(logoScale.value)
                    .width(200.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.kilivana_logo),
                    contentDescription = stringResource(id = R.string.kilivana_logo_content_desc),
                    modifier = Modifier.size(200.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = stringResource(id = R.string.kilivana_app_name),
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun KilivanaSplashScreenPreview() {
    KilivanadriverTheme {
        KilivanaSplashScreen()
    }
}
