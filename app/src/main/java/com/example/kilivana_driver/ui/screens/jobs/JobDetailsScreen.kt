package com.example.kilivana_driver.ui.screens.jobs

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kilivana_driver.data.model.Job
import com.example.kilivana_driver.data.model.JobStatus
import com.example.kilivana_driver.ui.components.KilivanaButton
import com.example.kilivana_driver.ui.theme.KilivanaBackground
import com.example.kilivana_driver.ui.theme.KilivanaBorder
import com.example.kilivana_driver.ui.theme.KilivanaGreen
import com.example.kilivana_driver.ui.theme.KilivanaGreenCard
import com.example.kilivana_driver.ui.theme.KilivanaGreenTint
import com.example.kilivana_driver.ui.theme.KilivanaNotificationRed
import com.example.kilivana_driver.ui.theme.KilivanaTextMuted
import com.example.kilivana_driver.ui.theme.KilivanaTextPrimary
import com.example.kilivana_driver.ui.theme.KilivanaTheme
import com.example.kilivana_driver.ui.theme.KilivanaWhite

@Composable
fun JobDetailsScreen(
    job: Job,
    onBack: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onStartTrip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDeclineDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KilivanaBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top bar
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
                text = "Job Details",
                modifier = Modifier.weight(1f),
                color = KilivanaTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(KilivanaWhite)
                    .clickable {
                        val text = "Kilivana job ${job.id}: ${job.pickupLocation} → " +
                            "${job.dropoffLocation} • ${job.cargo} (${job.quantity})"
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(send, "Share job"))
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "Share job",
                    tint = KilivanaTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Route card
            DetailsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = job.id,
                        modifier = Modifier.weight(1f),
                        color = KilivanaTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    StatusBadge(job = job)
                }
                Spacer(modifier = Modifier.height(16.dp))
                RoutePoint(
                    label = "PICKUP POINT",
                    color = KilivanaGreen,
                    location = job.pickupLocation,
                    detail = "${job.pickupTime} · ${job.pickupPlace}"
                )
                Spacer(modifier = Modifier.height(16.dp))
                RoutePoint(
                    label = "DROP-OFF POINT",
                    color = KilivanaNotificationRed,
                    location = job.dropoffLocation,
                    detail = "${job.dropoffTime} · ${job.dropoffPlace}"
                )
            }

            // Cargo card
            DetailsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // TODO: replace with the cargo photo from the API (Coil)
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(KilivanaGreenTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Inventory2,
                            contentDescription = null,
                            tint = KilivanaGreen,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = job.cargo,
                            color = KilivanaTextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Weight: ${job.quantity}",
                            color = KilivanaTextMuted,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${formatKsh(job.payoutKsh)} payout",
                            color = KilivanaGreenCard,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Delivery details card
            DetailsCard {
                Text(
                    text = "Delivery Details",
                    color = KilivanaTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))
                DetailRow("Distance", "${job.distanceKm} km")
                Spacer(modifier = Modifier.height(12.dp))
                DetailRow("Estimated Time", job.estimatedTime)
                Spacer(modifier = Modifier.height(12.dp))
                DetailRow("Order ID", "#${job.id}")
                Spacer(modifier = Modifier.height(12.dp))
                DetailRow("Customer", job.customer)
            }

            Spacer(modifier = Modifier.height(4.dp))
        }

        // Bottom actions
        if (job.status != JobStatus.COMPLETED) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (job.status) {
                    JobStatus.AVAILABLE -> {
                        KilivanaButton(text = "Accept Job", onClick = onAccept)
                        OutlinedButton(
                            onClick = { showDeclineDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, KilivanaBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = KilivanaWhite,
                                contentColor = KilivanaTextMuted
                            )
                        ) {
                            Text(
                                text = "Decline",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    JobStatus.ACCEPTED -> {
                        KilivanaButton(text = "Start Trip", onClick = onStartTrip)
                    }
                    JobStatus.COMPLETED -> Unit
                }
            }
        }
    }

    if (showDeclineDialog) {
        AlertDialog(
            onDismissRequest = { showDeclineDialog = false },
            title = { Text("Decline this job?") },
            text = { Text("Dispatch will offer ${job.id} to another driver.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeclineDialog = false
                        onDecline()
                    }
                ) {
                    Text(
                        text = "Decline",
                        color = KilivanaNotificationRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeclineDialog = false }) {
                    Text(text = "Cancel", color = KilivanaTextMuted)
                }
            }
        )
    }
}

@Composable
private fun DetailsCard(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, shape)
            .clip(shape)
            .background(KilivanaWhite)
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
private fun RoutePoint(
    label: String,
    color: Color,
    location: String,
    detail: String
) {
    Row {
        Box(
            modifier = Modifier
                .padding(top = 3.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = location,
                color = KilivanaTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = detail,
                color = KilivanaTextMuted,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = KilivanaTextMuted,
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = KilivanaTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun JobDetailsAvailablePreview() {
    KilivanaTheme {
        JobDetailsScreen(
            job = sampleJobs().first(),
            onBack = {},
            onAccept = {},
            onDecline = {},
            onStartTrip = {}
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun JobDetailsAcceptedPreview() {
    KilivanaTheme {
        JobDetailsScreen(
            job = sampleJobs().first().copy(status = JobStatus.ACCEPTED, isNew = false),
            onBack = {},
            onAccept = {},
            onDecline = {},
            onStartTrip = {}
        )
    }
}
