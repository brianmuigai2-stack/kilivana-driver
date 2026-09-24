package com.example.kilivana_driver.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val KilivanaColorScheme = lightColorScheme(
    primary = KilivanaGreen,
    onPrimary = KilivanaWhite
)

@Composable
fun KilivanaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KilivanaColorScheme,
        content = content
    )
}
