package com.example.kilivana_driver.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kilivana_driver.data.model.DriverProfile
import com.example.kilivana_driver.ui.theme.KilivanaBackground
import com.example.kilivana_driver.ui.theme.KilivanaTextMuted
import com.example.kilivana_driver.ui.theme.KilivanaTextPrimary

/**
 * Read-only view of the driver's vehicle and licence record.
 *
 * These fields are created and edited by a Kilivana admin, not by the driver,
 * so this screen has no edit affordance at all — only an add/upload action for
 * images, which the backend explicitly lets a driver do for their own profile.
 * Any field the admin has not filled in shows as "Not provided" rather than
 * inviting the driver to type one in.
 */
@Composable
fun VehicleDetailsScreen(
    profile: DriverProfile?,
    isLoading: Boolean,
    needsProfile: Boolean,
    onBack: () -> Unit,
    onManageImagesClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KilivanaBackground)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
    ) {
        ProfileSubHeader(title = "Vehicle Details", onBack = onBack)

        Column(modifier = Modifier.padding(16.dp)) {
            if (isLoading && profile == null) {
                CircularProgressIndicator(color = KilivanaTextPrimary)
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (needsProfile) {
                NoVehicleOnFileNotice()
                Spacer(modifier = Modifier.height(16.dp))
            }

            InfoCard {
                InfoRow("Licence number", profile?.licenseNumber)
                InfoDivider()
                InfoRow("Vehicle number", profile?.vehicleNumber)
                InfoDivider()
                InfoRow("Vehicle type", profile?.vehicleType?.replaceFirstChar { it.uppercase() })
                InfoDivider()
                InfoRow("Vehicle details", profile?.vehicleDetails)
                InfoDivider()
                InfoRow(
                    "Availability",
                    profile?.availabilityStatus?.replaceFirstChar { it.uppercase() }
                )
                InfoDivider()
                InfoRow("Profile ID", profile?.id?.takeIf { it > 0 }?.toString())
            }

            Spacer(modifier = Modifier.height(16.dp))

            InfoCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onManageImagesClick)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Licence & vehicle photos",
                            color = KilivanaTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Upload or remove your documents",
                            color = KilivanaTextMuted,
                            fontSize = 13.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = KilivanaTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Your licence and vehicle details are set up by your Kilivana admin. " +
                    "You can add photos of your licence and vehicle here; if something below " +
                    "is missing, contact your admin to have it added.",
                color = KilivanaTextMuted,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}
