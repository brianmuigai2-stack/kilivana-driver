package com.example.kilivana_driver.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Green800,
    secondary = Green600,
    tertiary = Green400,
    background = Green900,
    surface = Green900,
    onPrimary = OnGreen,
    onSecondary = OnGreen,
    onTertiary = OnGreen,
    onBackground = Green50,
    onSurface = Green50
)

private val LightColorScheme = lightColorScheme(
    primary = Green700,
    secondary = Green600,
    tertiary = Green400,
    background = Green50,
    surface = Green50,
    onPrimary = OnGreen,
    onSecondary = OnGreen,
    onTertiary = OnGreen,
    onBackground = OnGreenDark,
    onSurface = OnGreenDark
)

@Composable
fun KilivanadriverTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}