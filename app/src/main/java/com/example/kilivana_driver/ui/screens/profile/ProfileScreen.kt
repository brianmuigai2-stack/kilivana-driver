package com.example.kilivana_driver.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.kilivana_driver.data.model.Driver
import com.example.kilivana_driver.ui.theme.KilivanaAmber
import com.example.kilivana_driver.ui.theme.KilivanaAmberTint
import com.example.kilivana_driver.ui.theme.KilivanaBackground
import com.example.kilivana_driver.ui.theme.KilivanaBorder
import com.example.kilivana_driver.ui.theme.KilivanaErrorTint
import com.example.kilivana_driver.ui.theme.KilivanaGreen
import com.example.kilivana_driver.ui.theme.KilivanaGreenCard
import com.example.kilivana_driver.ui.theme.KilivanaGreenTint
import com.example.kilivana_driver.ui.theme.KilivanaNotificationRed
import com.example.kilivana_driver.ui.theme.KilivanaTextMuted
import com.example.kilivana_driver.ui.theme.KilivanaTextPrimary
import com.example.kilivana_driver.ui.theme.KilivanaTheme
import com.example.kilivana_driver.ui.theme.KilivanaWhite
import java.util.Locale

@Composable
fun ProfileScreen(
    driver: Driver,
    uiState: ProfileUiState = ProfileUiState(),
    onPickImage: () -> Unit = {},
    onDismissUploadMessage: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    onSettingsClick: () -> Unit = {},
    onPersonalInfoClick: () -> Unit = {},
    onVehicleDetailsClick: () -> Unit = {},
    onBankDetailsClick: () -> Unit = {},
    onChangePasswordClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
    onLogout: () -> Unit,
    onDeletePhoto: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeletePhotoDialog by remember { mutableStateOf(false) }
    var viewingProfileImage by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KilivanaBackground)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
    ) {
        // No statusBarsPadding here: the green header paints behind the
        // status bar, and only the icons/text inside it are pushed down.
        ProfileHeader(
            driver = driver,
            primaryImageUrl = uiState.primaryImage?.url,
            uploading = uiState.isUploading,
            onPickImage = onPickImage,
            onViewImage = { viewingProfileImage = true },
            onBack = onBack,
            onSettingsClick = onSettingsClick
        )

        Column(modifier = Modifier.padding(16.dp)) {
            SettingsCard {
                SettingsRow(
                    icon = Icons.Outlined.PhotoCamera,
                    title = "Profile Photo",
                    subtitle = when {
                        uiState.isUploading -> "Uploading…"
                        uiState.primaryImage != null -> "Tap to change your photo"
                        else -> "Add a photo so dispatch can identify you"
                    },
                    trailing = {
                        when {
                            uiState.isUploading -> CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = KilivanaGreen
                            )

                            else -> Icon(
                                imageVector = Icons.Outlined.ChevronRight,
                                contentDescription = null,
                                tint = KilivanaTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    onClick = { if (!uiState.isUploading) onPickImage() }
                )
            }

            val message = uiState.errorMessage ?: uiState.successMessage
            if (message != null) {
                Spacer(modifier = Modifier.height(8.dp))
                UploadBanner(
                    message = message,
                    isError = uiState.errorMessage != null,
                    onDismiss = onDismissUploadMessage
                )
            }

            if (uiState.needsProfile) {
                Spacer(modifier = Modifier.height(12.dp))
                MissingProfileNotice()
            }

            Spacer(modifier = Modifier.height(20.dp))

            SettingsCard {
                SettingsRow(
                    icon = Icons.Outlined.Person,
                    title = "Personal Information",
                    onClick = onPersonalInfoClick
                )
                RowDivider()
                SettingsRow(
                    icon = Icons.Outlined.LocalShipping,
                    title = "Vehicle Details",
                    subtitle = when {
                        uiState.profile == null -> "Not set up yet"
                        uiState.profile?.licenseNumber.isNullOrBlank() &&
                            uiState.profile?.vehicleNumber.isNullOrBlank() -> "No details saved"
                        else -> listOfNotNull(
                            uiState.profile?.vehicleNumber?.takeIf { it.isNotBlank() },
                            uiState.profile?.vehicleType?.takeIf { it.isNotBlank() }
                        ).joinToString(" • ")
                    },
                    onClick = onVehicleDetailsClick
                )
                RowDivider()
                SettingsRow(
                    icon = Icons.Outlined.AccountBalance,
                    title = "Bank Details",
                    subtitle = driver.paymentMethod,
                    onClick = onBankDetailsClick
                )
                RowDivider()
                SettingsRow(
                    icon = Icons.Outlined.Lock,
                    title = "Change Password",
                    onClick = onChangePasswordClick
                )
                RowDivider()
                SettingsRow(
                    icon = Icons.Outlined.Notifications,
                    title = "Notifications",
                    onClick = onNotificationsClick
                )
                RowDivider()
                SettingsRow(
                    icon = Icons.Outlined.HelpOutline,
                    title = "Help & Support",
                    onClick = onHelpClick
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
                Text(
                    text = "Log Out",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { showDeletePhotoDialog = true },
                enabled = uiState.primaryImage != null && !uiState.isDeletingImage,
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
                if (uiState.isDeletingImage) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = KilivanaNotificationRed
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Delete photo",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    val profileImageUrl = uiState.primaryImage?.url
    if (viewingProfileImage && !profileImageUrl.isNullOrBlank()) {
        ImageViewerDialog(
            url = profileImageUrl,
            onDismiss = { viewingProfileImage = false }
        )
    }

    if (showDeletePhotoDialog) {
        AlertDialog(
            onDismissRequest = { showDeletePhotoDialog = false },
            title = { Text("Delete profile photo?") },
            text = { Text("Your current photo will be removed. You can upload a new one anytime.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeletePhotoDialog = false
                        onDeletePhoto()
                    }
                ) {
                    Text(
                        text = "Delete",
                        color = KilivanaNotificationRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePhotoDialog = false }) {
                    Text(text = "Cancel", color = KilivanaTextMuted)
                }
            }
        )
    }
}

@Composable
private fun ProfileHeader(
    driver: Driver,
    primaryImageUrl: String?,
    uploading: Boolean,
    onPickImage: () -> Unit,
    onViewImage: () -> Unit,
    onBack: (() -> Unit)?,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(KilivanaGreenCard)
            .statusBarsPadding()
            .padding(bottom = 28.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 8.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
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
                        tint = KilivanaWhite,
                        modifier = Modifier.size(30.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(44.dp))
            }
            Text(
                text = "Profile",
                modifier = Modifier.weight(1f),
                color = KilivanaWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onSettingsClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = KilivanaWhite,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tap avatar = view photo (if one exists) or pick one.
            // Camera badge always lets the driver change the photo.
            Box(modifier = Modifier.size(80.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(KilivanaWhite.copy(alpha = 0.2f))
                        .clickable(
                            enabled = !uploading,
                            onClick = if (primaryImageUrl.isNullOrBlank()) onPickImage else onViewImage
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (primaryImageUrl.isNullOrBlank()) {
                        Text(
                            text = driver.name.split(" ").mapNotNull { it.firstOrNull() }.take(2)
                                .joinToString("").uppercase(),
                            color = KilivanaWhite,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        var imageError by remember(primaryImageUrl) { mutableStateOf(false) }
                        if (imageError) {
                            Text(
                                text = driver.name.split(" ").mapNotNull { it.firstOrNull() }.take(2)
                                    .joinToString("").uppercase(),
                                color = KilivanaWhite,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            val context = LocalContext.current
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(primaryImageUrl)
                                    .diskCachePolicy(CachePolicy.DISABLED)
                                    .build(),
                                contentDescription = "Profile photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                error = ColorPainter(androidx.compose.ui.graphics.Color.Transparent),
                                onError = { imageError = true }
                            )
                        }
                    }

                    if (uploading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = KilivanaWhite
                            )
                        }
                    }
                }

                if (!uploading) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(KilivanaGreen)
                            .clickable(onClick = onPickImage),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoCamera,
                            contentDescription = "Change photo",
                            tint = KilivanaWhite,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = driver.name,
                color = KilivanaWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Driver ID: ${driver.driverId}",
                color = KilivanaWhite.copy(alpha = 0.85f),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(KilivanaWhite.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color(0xFFFFC107),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "%.1f".format(Locale.US, driver.rating),
                    color = KilivanaWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = " (${driver.deliveriesCompleted} deliveries completed)",
                    color = KilivanaWhite.copy(alpha = 0.85f),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
internal fun ImageViewerDialog(url: String, onDismiss: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black)
        ) {
            val context = LocalContext.current
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(url)
                    .diskCachePolicy(CachePolicy.DISABLED)
                    .build(),
                contentDescription = "Full screen image",
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            offsetX = if (scale == 1f) 0f else offsetX + pan.x
                            offsetY = if (scale == 1f) 0f else offsetY + pan.y
                        }
                    }
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    ),
                contentScale = ContentScale.Fit
            )
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f))
                    .clickable(onClick = onDismiss)
                    .align(Alignment.TopEnd),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close",
                    tint = KilivanaWhite,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
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

private typealias ColumnScope = androidx.compose.foundation.layout.ColumnScope

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(KilivanaGreenTint),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = KilivanaGreen,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = KilivanaTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = KilivanaTextMuted,
                    fontSize = 13.sp
                )
            }
        }
        trailing?.invoke() ?: Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = KilivanaTextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun MissingProfileNotice() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(KilivanaAmberTint)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = KilivanaAmber,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Your licence and vehicle details are set up by your Kilivana admin. " +
                "You can add photos of them here, but the details themselves are filled in " +
                "for you — ask your admin if something is missing.",
            color = KilivanaAmber,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun UploadBanner(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isError) KilivanaErrorTint else KilivanaGreenTint)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isError) Icons.Outlined.ErrorOutline else Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = if (isError) KilivanaNotificationRed else KilivanaGreen,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = message,
            color = if (isError) KilivanaNotificationRed else KilivanaGreen,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Dismiss",
            color = if (isError) KilivanaNotificationRed else KilivanaGreen,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onDismiss)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
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

@Preview(showSystemUi = true)
@Composable
private fun ProfileScreenPreview() {
    KilivanaTheme {
        ProfileScreen(
            driver = Driver(
                name = "James Mwangi",
                driverId = "DRI-0042",
                rating = 4.8,
                deliveriesCompleted = 12,
                vehiclePlate = "KDB 432A",
                vehicleType = "Truck",
                paymentMethod = "M-Pesa registered"
            ),
            onLogout = {}
        )
    }
}
