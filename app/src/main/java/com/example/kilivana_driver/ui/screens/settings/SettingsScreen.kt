package com.example.kilivana_driver.ui.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.kilivana_driver.BuildConfig
import com.example.kilivana_driver.ui.theme.KilivanaBackground
import com.example.kilivana_driver.ui.theme.KilivanaBorder
import com.example.kilivana_driver.ui.theme.KilivanaGreen
import com.example.kilivana_driver.ui.theme.KilivanaGreenTint
import com.example.kilivana_driver.ui.theme.KilivanaNotificationRed
import com.example.kilivana_driver.ui.theme.KilivanaTextMuted
import com.example.kilivana_driver.ui.theme.KilivanaTextPrimary
import com.example.kilivana_driver.ui.theme.KilivanaTheme
import com.example.kilivana_driver.ui.theme.KilivanaWhite

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onPushNotificationsChange: (Boolean) -> Unit,
    onNotificationSoundChange: (Boolean) -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onChangePasswordClick: () -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onAboutClick: () -> Unit,
    onSendTestNotification: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Android 13+ requires runtime permission before notifications can be
    // shown. We only turn the toggle on once that permission is granted.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> onPushNotificationsChange(granted) }

    fun requestPushToggle(enable: Boolean) {
        val needsRuntimePermission = enable &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needsRuntimePermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onPushNotificationsChange(enable)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KilivanaBackground)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        TopBar(title = "Settings", onBack = onBack)

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            SectionLabel("Preferences")
            SettingsCard {
                SwitchRow(
                    icon = Icons.Outlined.NotificationsActive,
                    title = "Push Notifications",
                    subtitle = "New jobs and dispatch updates",
                    checked = uiState.pushNotificationsEnabled,
                    onCheckedChange = ::requestPushToggle
                )
                RowDivider()
                SwitchRow(
                    icon = Icons.Outlined.VolumeUp,
                    title = "Notification Sound",
                    checked = uiState.notificationSoundEnabled,
                    onCheckedChange = onNotificationSoundChange,
                    enabled = uiState.pushNotificationsEnabled
                )
                RowDivider()
                SwitchRow(
                    icon = Icons.Outlined.DarkMode,
                    title = "Dark Mode",
                    checked = uiState.darkModeEnabled,
                    onCheckedChange = onDarkModeChange
                )
                RowDivider()
                ActionRow(
                    icon = Icons.Outlined.Send,
                    title = "Send test notification",
                    actionLabel = "Send",
                    enabled = uiState.pushNotificationsEnabled,
                    onClick = onSendTestNotification
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Sound changes apply to notifications sent after you change this. Your device's own settings can always override sound and vibration for this app.",
                color = KilivanaTextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            SectionLabel("App", topSpace = 24.dp)
            SettingsCard {
                NavRow(
                    icon = Icons.Outlined.Language,
                    title = "Language",
                    value = uiState.language.displayName,
                    onClick = { showLanguageDialog = true }
                )
                RowDivider()
                NavRow(
                    icon = Icons.Outlined.Description,
                    title = "Terms of Service",
                    onClick = onTermsClick
                )
                RowDivider()
                NavRow(
                    icon = Icons.Outlined.PrivacyTip,
                    title = "Privacy Policy",
                    onClick = onPrivacyClick
                )
                RowDivider()
                NavRow(
                    icon = Icons.Outlined.Info,
                    title = "About Kilivana",
                    value = "v${BuildConfig.VERSION_NAME}",
                    onClick = onAboutClick
                )
            }

            SectionLabel("Account", topSpace = 24.dp)
            SettingsCard {
                NavRow(
                    icon = Icons.Outlined.Lock,
                    title = "Change Password",
                    onClick = onChangePasswordClick
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, KilivanaNotificationRed),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = KilivanaWhite,
                    contentColor = KilivanaNotificationRed
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Log Out", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Kilivana Driver • v${BuildConfig.VERSION_NAME}",
                modifier = Modifier.fillMaxWidth(),
                color = KilivanaTextMuted.copy(alpha = 0.7f),
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showLanguageDialog) {
        LanguageDialog(
            selected = uiState.language,
            onSelect = {
                onLanguageSelected(it)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log out?") },
            text = { Text("You'll need your phone number and password to sign in again.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text(text = "Log Out", color = KilivanaNotificationRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(text = "Cancel", color = KilivanaTextMuted)
                }
            }
        )
    }
}

@Composable
private fun TopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
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
            text = title,
            color = KilivanaTextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SectionLabel(text: String, topSpace: androidx.compose.ui.unit.Dp = 0.dp) {
    if (topSpace > 0.dp) Spacer(modifier = Modifier.height(topSpace))
    Text(
        text = text.uppercase(),
        color = KilivanaTextMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun SettingsCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, shape)
            .clip(shape)
            .background(KilivanaWhite)
    ) {
        content()
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RowIcon(icon)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (enabled) KilivanaTextPrimary else KilivanaTextMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, color = KilivanaTextMuted, fontSize = 12.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = KilivanaWhite,
                checkedTrackColor = KilivanaGreen
            )
        )
    }
}

@Composable
private fun NavRow(
    icon: ImageVector,
    title: String,
    value: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RowIcon(icon)
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = KilivanaTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (value != null) {
            Text(text = value, color = KilivanaTextMuted, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = KilivanaTextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    actionLabel: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RowIcon(icon)
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = if (enabled) KilivanaTextPrimary else KilivanaTextMuted,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(if (enabled) KilivanaGreenTint else KilivanaBorder.copy(alpha = 0.4f))
                .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = actionLabel,
                color = if (enabled) KilivanaGreen else KilivanaTextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RowIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(KilivanaGreenTint),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = KilivanaGreen, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun RowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 66.dp)
            .height(1.dp)
            .background(KilivanaBorder.copy(alpha = 0.6f))
    )
}

@Composable
private fun LanguageDialog(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose language") },
        text = {
            Column {
                AppLanguage.values().forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(language) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = language == selected,
                            onClick = { onSelect(language) },
                            colors = RadioButtonDefaults.colors(selectedColor = KilivanaGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = language.displayName, color = KilivanaTextPrimary, fontSize = 15.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Done", color = KilivanaGreen, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Preview(showSystemUi = true)
@Composable
private fun SettingsScreenPreview() {
    KilivanaTheme {
        SettingsScreen(
            uiState = SettingsUiState(),
            onBack = {},
            onPushNotificationsChange = {},
            onNotificationSoundChange = {},
            onDarkModeChange = {},
            onLanguageSelected = {},
            onChangePasswordClick = {},
            onTermsClick = {},
            onPrivacyClick = {},
            onAboutClick = {},
            onSendTestNotification = {},
            onLogout = {}
        )
    }
}
