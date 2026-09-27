package com.example.kilivana_driver.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.kilivana_driver.ui.theme.KilivanaGreenCard

/** A green strip exactly as tall as the status bar, painted behind it. */
@Composable
fun KilivanaStatusBarScrim() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsTopHeight(WindowInsets.statusBars)
            .background(KilivanaGreenCard)
    )
}
