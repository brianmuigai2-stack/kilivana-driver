package com.example.kilivana_driver.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun KilivanaTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    // Runs every recomposition, but Compose skips notifying readers when a
    // value hasn't actually changed, so this is cheap once the mode settles.
    applyKilivanaPalette(darkTheme)

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = KilivanaGreen,
            onPrimary = KilivanaWhite,
            background = KilivanaBackground,
            surface = KilivanaWhite
        )
    } else {
        lightColorScheme(
            primary = KilivanaGreen,
            onPrimary = KilivanaWhite,
            background = KilivanaBackground,
            surface = KilivanaWhite
        )
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
