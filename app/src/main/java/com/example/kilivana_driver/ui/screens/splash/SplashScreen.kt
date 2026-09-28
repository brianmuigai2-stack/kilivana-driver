package com.example.kilivana_driver.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay

private val PollenColor = Color(0xFFE6F5B0)

/** One drifting speck of light. Positions are fractions so it works on any screen size. */
private class Particle(
    val x: Float,          // 0..1 horizontal position
    val phase: Float,      // 0..1 where it starts in its rise
    val speed: Int,        // full rises per cycle (whole numbers keep the loop seamless)
    val radiusDp: Float,
    val swayDp: Float,
    val swayCycles: Int,
    val swayPhase: Float
)

@Composable
fun SplashScreen(onFinished: () -> Unit = {}) {
    LaunchedEffect(Unit) {
        delay(2500)
        onFinished()
    }

    // One-shot animations that play while the splash is on screen
    val zoom = remember { Animatable(1f) }
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(Unit) { zoom.animateTo(1.12f, tween(3000, easing = LinearEasing)) }
    LaunchedEffect(Unit) { sweep.animateTo(1f, tween(2500, easing = FastOutSlowInEasing)) }

    // Looping clock that drives the floating particles
    val drift = rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val particles = remember {
        val random = Random(42)
        List(28) {
            Particle(
                x = random.nextFloat(),
                phase = random.nextFloat(),
                speed = if (random.nextFloat() < 0.6f) 1 else 2,
                radiusDp = 1.5f + random.nextFloat() * 2.5f,
                swayDp = 8f + random.nextFloat() * 18f,
                swayCycles = 1 + random.nextInt(2),
                swayPhase = random.nextFloat()
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background photo: slow zoom that drifts slightly to the left
        Image(
            painter = painterResource(R.drawable.splashscreen),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val scale = zoom.value
                    scaleX = scale
                    scaleY = scale
                    translationX = -(scale - 1f) * size.width * 0.35f
                },
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

        // Sunlight sweep + floating pollen, drawn on one canvas.
        // The animated values are read inside the draw block, so only drawing
        // updates each frame, not the whole screen.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width

            // Soft band of light gliding left to right
            val bandCenter = -0.3f * width + sweep.value * 1.6f * width
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    startX = bandCenter - width * 0.35f,
                    endX = bandCenter + width * 0.35f
                )
            )

            // Rising, swaying specks that fade in and out
            val clock = drift.value
            particles.forEach { p ->
                val t = (clock * p.speed + p.phase) % 1f
                val y = size.height * (1.05f - 1.1f * t)
                val sway = sin(2.0 * PI * (clock * p.swayCycles + p.swayPhase)).toFloat()
                val x = width * p.x + sway * p.swayDp.dp.toPx()
                val fade = sin(PI * t).toFloat()
                val radius = p.radiusDp.dp.toPx()

                drawCircle(
                    color = PollenColor.copy(alpha = 0.18f * fade),
                    radius = radius * 3f,
                    center = Offset(x, y)
                )
                drawCircle(
                    color = PollenColor.copy(alpha = 0.85f * fade),
                    radius = radius,
                    center = Offset(x, y)
                )
            }
        }

        // Centre content
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.kilivana_logo),
                contentDescription = "Kilivana logo",
                modifier = Modifier.height(120.dp),
                contentScale = ContentScale.Fit
            )
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
