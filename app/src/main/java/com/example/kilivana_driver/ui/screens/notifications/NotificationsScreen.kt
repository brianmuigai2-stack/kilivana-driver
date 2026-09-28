package com.example.kilivana_driver.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kilivana_driver.data.model.AppNotification
import com.example.kilivana_driver.data.model.NotificationType
import com.example.kilivana_driver.ui.components.KilivanaStatusBarScrim
import com.example.kilivana_driver.ui.theme.KilivanaAmber
import com.example.kilivana_driver.ui.theme.KilivanaAmberTint
import com.example.kilivana_driver.ui.theme.KilivanaBackground
import com.example.kilivana_driver.ui.theme.KilivanaBlue
import com.example.kilivana_driver.ui.theme.KilivanaBlueTint
import com.example.kilivana_driver.ui.theme.KilivanaBorder
import com.example.kilivana_driver.ui.theme.KilivanaGreen
import com.example.kilivana_driver.ui.theme.KilivanaGreenCard
import com.example.kilivana_driver.ui.theme.KilivanaGreenTint
import com.example.kilivana_driver.ui.theme.KilivanaTextMuted
import com.example.kilivana_driver.ui.theme.KilivanaTextPrimary
import com.example.kilivana_driver.ui.theme.KilivanaTheme
import com.example.kilivana_driver.ui.theme.KilivanaWhite

private enum class NotificationFilter { ALL, UNREAD }

@Composable
fun NotificationsScreen(
    uiState: NotificationsUiState,
    onBack: () -> Unit,
    onNotificationClick: (String) -> Unit,
    onMarkAllRead: () -> Unit,
    modifier: Modifier = Modifier
) {
    var filter by rememberSaveable { mutableStateOf(NotificationFilter.ALL) }

    val visible = when (filter) {
        NotificationFilter.ALL -> uiState.items
        NotificationFilter.UNREAD -> uiState.items.filter { !it.isRead }
    }
    val today = visible.filter { !it.isEarlier }
    val earlier = visible.filter { it.isEarlier }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KilivanaBackground)
            .navigationBarsPadding()
    ) {
        KilivanaStatusBarScrim()

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Back",
                    tint = KilivanaTextPrimary,
                    modifier = Modifier.size(30.dp)
                )
            }
            Text(
                text = "Notifications",
                modifier = Modifier.weight(1f),
                color = KilivanaTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            if (uiState.unreadCount > 0) {
                TextButton(
                    onClick = onMarkAllRead,
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DoneAll,
                        contentDescription = null,
                        tint = KilivanaGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Mark all read",
                        color = KilivanaGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Filter chips
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterPill(
                text = "All",
                selected = filter == NotificationFilter.ALL,
                onClick = { filter = NotificationFilter.ALL }
            )
            FilterPill(
                text = if (uiState.unreadCount > 0) "Unread (${uiState.unreadCount})" else "Unread",
                selected = filter == NotificationFilter.UNREAD,
                onClick = { filter = NotificationFilter.UNREAD }
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (visible.isEmpty()) {
                item { EmptyState(filter = filter) }
            } else {
                if (today.isNotEmpty()) {
                    item(key = "header_today") { SectionHeader("Today") }
                    items(today, key = { it.id }) { notification ->
                        NotificationCard(
                            notification = notification,
                            onClick = { onNotificationClick(notification.id) }
                        )
                    }
                }
                if (earlier.isNotEmpty()) {
                    item(key = "header_earlier") { SectionHeader("Earlier") }
                    items(earlier, key = { it.id }) { notification ->
                        NotificationCard(
                            notification = notification,
                            onClick = { onNotificationClick(notification.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) KilivanaGreenCard else KilivanaWhite)
            .then(
                if (selected) Modifier else Modifier.border(1.dp, KilivanaBorder, shape)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            color = if (selected) KilivanaWhite else KilivanaTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        modifier = Modifier.padding(top = 4.dp),
        color = KilivanaTextMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp
    )
}

private class TypeStyle(
    val icon: ImageVector,
    val tint: Color,
    val background: Color
)

private fun styleFor(type: NotificationType): TypeStyle = when (type) {
    NotificationType.NEW_JOB ->
        TypeStyle(Icons.Outlined.Work, KilivanaGreen, KilivanaGreenTint)
    NotificationType.JOB_UPDATE ->
        TypeStyle(Icons.Outlined.LocalShipping, KilivanaBlue, KilivanaBlueTint)
    NotificationType.PAYMENT ->
        TypeStyle(Icons.Outlined.AccountBalanceWallet, KilivanaAmber, KilivanaAmberTint)
    NotificationType.SYSTEM ->
        TypeStyle(Icons.Outlined.Info, KilivanaTextMuted, KilivanaBorder.copy(alpha = 0.4f))
}

@Composable
private fun NotificationCard(
    notification: AppNotification,
    onClick: () -> Unit
) {
    val style = styleFor(notification.type)
    val unread = !notification.isRead
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, shape)
            .clip(shape)
            .background(KilivanaWhite)
            .then(
                if (unread) Modifier.border(1.dp, KilivanaGreen.copy(alpha = 0.35f), shape)
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(style.background),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = style.icon,
                contentDescription = null,
                tint = style.tint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = notification.title,
                    modifier = Modifier.weight(1f),
                    color = KilivanaTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = if (unread) FontWeight.Bold else FontWeight.SemiBold
                )
                if (unread) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(KilivanaGreen)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = notification.message,
                color = KilivanaTextMuted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = notification.timeLabel,
                color = KilivanaTextMuted.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun EmptyState(filter: NotificationFilter) {
    val icon = if (filter == NotificationFilter.UNREAD) Icons.Outlined.DoneAll else Icons.Outlined.NotificationsNone
    val (title, message) = when (filter) {
        NotificationFilter.ALL ->
            "No notifications yet" to "New jobs, payments and updates will show up here."
        NotificationFilter.UNREAD ->
            "You're all caught up" to "No unread notifications."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(KilivanaGreenTint),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = KilivanaGreen,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            color = KilivanaTextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = message,
            color = KilivanaTextMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun NotificationsScreenPreview() {
    KilivanaTheme {
        NotificationsScreen(
            uiState = NotificationsUiState(items = sampleNotifications()),
            onBack = {},
            onNotificationClick = {},
            onMarkAllRead = {}
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun NotificationsEmptyPreview() {
    KilivanaTheme {
        NotificationsScreen(
            uiState = NotificationsUiState(),
            onBack = {},
            onNotificationClick = {},
            onMarkAllRead = {}
        )
    }
}
