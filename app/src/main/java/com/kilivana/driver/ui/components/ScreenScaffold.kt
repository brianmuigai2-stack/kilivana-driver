package com.kilivana.driver.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Global screen container that enforces the app's Android system-bar rules.
 *
 * - The whole window is used (edge-to-edge), so a background that should reach
 *   the very top or bottom of the screen (status bar / navigation bar area)
 *   simply sets [containerColor].
 * - When a [topBar] is supplied it is allowed to draw full-bleed to the top
 *   edge; the [topBar] itself is responsible for padding its own text/icons
 *   below the status bar (see [WindowInsets.statusBars]).
 * - The main [content] fills everything between the [topBar] and [bottomBar].
 * - When a [bottomBar] is supplied it is placed completely above the Android
 *   navigation area (3-button or gesture), so app buttons never overlap the
 *   system Back/Home/Recent affordances.
 *
 * No hardcoded margins are used: every inset is read from WindowInsets.
 */
@Composable
fun ScreenScaffold(
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Transparent,
    topBar: @Composable (() -> Unit)? = null,
    bottomBar: @Composable (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    val navBar = WindowInsets.navigationBars
    val navBarBottom = navBar.asPaddingValues().calculateBottomPadding()
    val contentPadding = PaddingValues(
        bottom = if (bottomBar != null) navBarBottom else 0.dp
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(containerColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            topBar?.let { bar ->
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    bar()
                }
            }

            // Main content fills the space that is not consumed by the top /
            // bottom bars. Bottom padding is only added when a bottom bar is
            // present so the content never scrolls under the navigation area.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(contentPadding)
            ) {
                content(contentPadding)
            }

            // The bottom bar is kept entirely above the navigation area.
            bottomBar?.let { bar ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(navBar)
                ) {
                    bar()
                }
            }
        }
    }
}
