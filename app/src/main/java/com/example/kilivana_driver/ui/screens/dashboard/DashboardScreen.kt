package com.example.kilivana_driver.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.kilivana_driver.ui.theme.KilivanaBackground
import com.example.kilivana_driver.ui.theme.KilivanaBlue
import com.example.kilivana_driver.ui.theme.KilivanaBlueTint
import com.example.kilivana_driver.ui.theme.KilivanaGreen
import com.example.kilivana_driver.ui.theme.KilivanaGreenCard
import com.example.kilivana_driver.ui.theme.KilivanaGreenTint
import com.example.kilivana_driver.ui.theme.KilivanaNotificationRed
import com.example.kilivana_driver.ui.theme.KilivanaTextMuted
import com.example.kilivana_driver.ui.theme.KilivanaTextPrimary
import com.example.kilivana_driver.ui.theme.KilivanaTheme
import com.example.kilivana_driver.ui.theme.KilivanaWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onNotificationsClick: () -> Unit,
    onMyJobsClick: () -> Unit,
    onMapRouteClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateLabel = remember {
        SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KilivanaBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // Full-bleed green header (paints behind the status bar, like Profile)
        DashboardHeader(
            uiState = uiState,
            dateLabel = dateLabel,
            onNotificationsClick = onNotificationsClick
        )

        // The schedule card floats over the bottom edge of the header
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .pullUp(44.dp)
        ) {
            ScheduleCard(
                deliveries = uiState.deliveriesAssigned,
                estimatedTransit = uiState.estimatedTransit
            )
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            SectionTitle(text = "Quick Actions", topSpace = 20.dp)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard("My Jobs", Icons.Outlined.Work, onMyJobsClick, Modifier.weight(1f))
                QuickActionCard("Map Route", Icons.Outlined.Map, onMapRouteClick, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard("History", Icons.Outlined.History, onHistoryClick, Modifier.weight(1f))
                QuickActionCard("Profile", Icons.Outlined.Person, onProfileClick, Modifier.weight(1f))
            }

            SectionTitle(text = "Recent Activity", topSpace = 24.dp)

            if (uiState.recentActivity.isEmpty()) {
                Text(
                    text = "No recent activity yet.",
                    color = KilivanaTextMuted,
                    fontSize = 14.sp
                )
            } else {
                uiState.recentActivity.forEachIndexed { index, item ->
                    if (index > 0) Spacer(modifier = Modifier.height(12.dp))
                    ActivityRow(item = item)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/** Pulls a composable up over the one above it, without leaving a gap below. */
private fun Modifier.pullUp(amount: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val overlap = amount.roundToPx().coerceAtMost(placeable.height)
    layout(placeable.width, placeable.height - overlap) {
        placeable.place(0, -overlap)
    }
}

private fun greetingEmoji(greeting: String): String = when {
    greeting.contains("morning", ignoreCase = true) -> "☀️"
    greeting.contains("afternoon", ignoreCase = true) -> "🌤️"
    else -> "🌙"
}

@Composable
private fun DashboardHeader(
    uiState: DashboardUiState,
    dateLabel: String,
    onNotificationsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(KilivanaGreenCard)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 64.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar: the driver's profile photo when one has been uploaded,
            // otherwise their initials. Same fallback as the profile header.
            val context = LocalContext.current
            val photoUrl = uiState.photoUrl
            val imageError = remember { mutableStateOf(false) }
            // A fresh URL means a fresh image to load, so clear any stale
            // error from the previous photo.
            LaunchedEffect(photoUrl) { imageError.value = false }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(KilivanaWhite.copy(alpha = 0.18f))
                    .border(2.dp, KilivanaWhite.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (photoUrl.isNotBlank() && !imageError.value) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(photoUrl)
                            .diskCachePolicy(CachePolicy.DISABLED)
                            .build(),
                        contentDescription = "Driver photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = ColorPainter(Color.Transparent),
                        onError = { imageError.value = true }
                    )
                }
                if (photoUrl.isBlank() || imageError.value) {
                    Text(
                        text = uiState.driverName.take(1).uppercase(),
                        color = KilivanaWhite,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${uiState.greeting} ${greetingEmoji(uiState.greeting)}",
                    color = KilivanaWhite.copy(alpha = 0.85f),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = uiState.driverName,
                    color = KilivanaWhite,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(KilivanaWhite.copy(alpha = 0.18f))
                    .clickable(onClick = onNotificationsClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = KilivanaWhite,
                    modifier = Modifier.size(22.dp)
                )
                if (uiState.hasUnreadNotifications) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 9.dp, end = 10.dp)
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(KilivanaNotificationRed)
                            .border(1.5.dp, KilivanaGreenCard, CircleShape)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeaderPill(text = "Driver ID: ${uiState.driverId}")
            HeaderPill(text = dateLabel)
        }
    }
}

@Composable
private fun HeaderPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(KilivanaWhite.copy(alpha = 0.16f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = KilivanaWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ScheduleCard(
    deliveries: Int,
    estimatedTransit: String
) {
    val headline = when (deliveries) {
        0 -> "No deliveries assigned"
        1 -> "1 Delivery assigned"
        else -> "$deliveries Deliveries assigned"
    }

    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, shape)
            .clip(shape)
            .background(KilivanaWhite)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "TODAY'S SCHEDULE",
                color = KilivanaGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = headline,
                color = KilivanaTextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            if (deliveries > 0 && estimatedTransit.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Estimated transit: $estimatedTransit",
                    color = KilivanaTextMuted,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(KilivanaGreenTint),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.LocalShipping,
                contentDescription = null,
                tint = KilivanaGreen,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun SectionTitle(
    text: String,
    topSpace: Dp
) {
    Spacer(modifier = Modifier.height(topSpace))
    Text(
        text = text,
        color = KilivanaTextPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun QuickActionCard(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .shadow(1.dp, shape)
            .clip(shape)
            .background(KilivanaWhite)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(KilivanaGreenTint),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = KilivanaGreen,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = label,
            color = KilivanaTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ActivityRow(item: ActivityItem) {
    val icon: ImageVector
    val tint = when (item.type) {
        ActivityType.PICKED_UP -> KilivanaGreen
        ActivityType.DELIVERED -> KilivanaBlue
    }
    val background = when (item.type) {
        ActivityType.PICKED_UP -> KilivanaGreenTint
        ActivityType.DELIVERED -> KilivanaBlueTint
    }
    icon = when (item.type) {
        ActivityType.PICKED_UP -> Icons.Outlined.LocalShipping
        ActivityType.DELIVERED -> Icons.Filled.Check
    }

    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, shape)
            .clip(shape)
            .background(KilivanaWhite)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(background),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = item.title,
                color = KilivanaTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.subtitle,
                color = KilivanaTextMuted,
                fontSize = 13.sp
            )
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun DashboardScreenPreview() {
    KilivanaTheme {
        DashboardScreen(
            uiState = sampleDashboardState(),
            onNotificationsClick = {},
            onMyJobsClick = {},
            onMapRouteClick = {},
            onHistoryClick = {},
            onProfileClick = {}
        )
    }
}
