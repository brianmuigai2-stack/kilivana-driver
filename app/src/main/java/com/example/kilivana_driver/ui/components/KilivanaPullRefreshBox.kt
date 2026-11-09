package com.example.kilivana_driver.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Reusable pull-to-refresh container wrapping content with Material 3 [PullToRefreshBox].
 * Implements swipe-down to refresh across app screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KilivanaPullRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
    ) {
        content()
    }
}
