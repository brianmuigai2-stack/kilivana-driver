package com.example.kilivana_driver.ui.screens.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.kilivana_driver.data.model.DriverImage
import com.example.kilivana_driver.ui.components.KilivanaButton
import com.example.kilivana_driver.ui.theme.KilivanaBackground
import com.example.kilivana_driver.ui.theme.KilivanaGreen
import com.example.kilivana_driver.ui.theme.KilivanaGreenTint
import com.example.kilivana_driver.ui.theme.KilivanaNotificationRed
import com.example.kilivana_driver.ui.theme.KilivanaTextMuted
import com.example.kilivana_driver.ui.theme.KilivanaTextPrimary
import com.example.kilivana_driver.ui.theme.KilivanaWhite

/**
 * The driver's licence and vehicle photos.
 *
 * Unlike the rest of the profile, images ARE the driver's to manage: the
 * backend allows a driver to upload and delete their own images even though
 * the vehicle and licence text is admin-only.
 */
@Composable
fun ProfileImagesScreen(
    images: List<DriverImage>,
    isLoading: Boolean,
    isUploading: Boolean,
    isDeleting: Boolean,
    onPickImage: () -> Unit,
    onDeleteImage: (DriverImage) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KilivanaBackground)
            .navigationBarsPadding()
    ) {
        ProfileSubHeader(title = "Licence & Vehicle Photos", onBack = onBack)

        Column(modifier = Modifier.padding(16.dp)) {
            KilivanaButton(
                text = "Upload photo",
                onClick = onPickImage,
                isLoading = isUploading
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading && images.isEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = KilivanaGreen)
                }
            } else if (images.isEmpty()) {
                NoPhotosYet()
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(images, key = { it.id }) { image ->
                        ImageTile(
                            image = image,
                            isDeleting = isDeleting,
                            onDelete = { onDeleteImage(image) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageTile(
    image: DriverImage,
    isDeleting: Boolean,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(KilivanaGreenTint)
    ) {
        AsyncImage(
            model = image.url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (image.isPrimary) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(KilivanaGreen)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Primary",
                    color = KilivanaWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(KilivanaWhite.copy(alpha = 0.9f))
                .clickable(enabled = !isDeleting, onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            if (isDeleting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = KilivanaGreen
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete photo",
                    tint = KilivanaNotificationRed,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun NoPhotosYet() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.AddAPhoto,
            contentDescription = null,
            tint = KilivanaTextMuted,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No photos yet",
            color = KilivanaTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Upload a clear photo of your licence and vehicle so the admin can verify them.",
            color = KilivanaTextMuted,
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
internal fun NoVehicleOnFileNotice() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(KilivanaGreenTint)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.AddAPhoto,
            contentDescription = null,
            tint = KilivanaGreen,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.size(10.dp))
        Text(
            text = "Your admin hasn't added your licence and vehicle details yet, so they " +
                "can't be shown here. You can still upload photos of them.",
            color = KilivanaGreen,
            fontSize = 14.sp
        )
    }
}
