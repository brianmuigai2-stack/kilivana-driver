package com.example.kilivana_driver.ui.screens.jobs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kilivana_driver.data.model.Job
import com.example.kilivana_driver.data.model.JobStatus
import com.example.kilivana_driver.ui.theme.KilivanaAmber
import com.example.kilivana_driver.ui.theme.KilivanaAmberTint
import com.example.kilivana_driver.ui.theme.KilivanaBackground
import com.example.kilivana_driver.ui.theme.KilivanaBlue
import com.example.kilivana_driver.ui.theme.KilivanaBlueTint
import com.example.kilivana_driver.ui.theme.KilivanaBorder
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
fun JobsScreen(
    uiState: JobsUiState,
    onStatusSelected: (JobStatus) -> Unit,
    onFilterClick: () -> Unit,
    onJobClick: (Job) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleJobs = uiState.visibleJobs

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KilivanaBackground)
            .statusBarsPadding()
    ) {
        JobsHeader(onFilterClick = onFilterClick)

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            JobStatus.values().forEach { status ->
                StatusTab(
                    text = "${status.label} (${uiState.countFor(status)})",
                    selected = status == uiState.selectedStatus,
                    onClick = { onStatusSelected(status) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (visibleJobs.isEmpty()) {
                item { EmptyState(status = uiState.selectedStatus) }
            } else {
                items(visibleJobs, key = { it.id }) { job ->
                    JobCard(job = job, onClick = { onJobClick(job) })
                }
            }
        }
    }
}

@Composable
private fun JobsHeader(onFilterClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Jobs",
            modifier = Modifier.weight(1f),
            color = KilivanaTextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .size(44.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(KilivanaWhite)
                .clickable(onClick = onFilterClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Tune,
                contentDescription = "Filter jobs",
                tint = KilivanaGreen,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun StatusTab(
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
            .padding(horizontal = 16.dp, vertical = 10.dp)
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
private fun JobCard(
    job: Job,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, shape)
            .clip(shape)
            .background(KilivanaWhite)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = job.id,
                modifier = Modifier.weight(1f),
                color = KilivanaTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            StatusBadge(job = job)
        }

        Spacer(modifier = Modifier.height(14.dp))

        RouteStop(
            dotColor = KilivanaGreen,
            location = job.pickupLocation,
            time = job.pickupTime
        )
        Spacer(modifier = Modifier.height(10.dp))
        RouteStop(
            dotColor = KilivanaNotificationRed,
            location = job.dropoffLocation,
            time = job.dropoffTime
        )

        Spacer(modifier = Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(KilivanaBorder.copy(alpha = 0.6f))
        )
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = job.cargo,
                    color = KilivanaTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${job.quantity} • ${job.distanceKm} km • ${job.estimatedTime}",
                    color = KilivanaTextMuted,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = formatKsh(job.payoutKsh),
                color = KilivanaGreenCard,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = KilivanaGreenCard,
                contentColor = KilivanaWhite
            )
        ) {
            Text(
                text = "View Details",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun RouteStop(
    dotColor: Color,
    location: String,
    time: String
) {
    Row {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = location,
                color = KilivanaTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = time,
                color = KilivanaTextMuted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
internal fun StatusBadge(job: Job) {
    val badge: Triple<String, Color, Color>? = when (job.status) {
        JobStatus.AVAILABLE ->
            if (job.isNew) Triple("NEW", KilivanaAmber, KilivanaAmberTint) else null
        JobStatus.ACCEPTED -> Triple("ACCEPTED", KilivanaBlue, KilivanaBlueTint)
        JobStatus.COMPLETED -> Triple("COMPLETED", KilivanaGreen, KilivanaGreenTint)
    }
    if (badge == null) return

    val (label, textColor, background) = badge
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun EmptyState(status: JobStatus) {
    val (title, message) = when (status) {
        JobStatus.AVAILABLE ->
            "No jobs available right now" to "New jobs from dispatch will appear here."
        JobStatus.ACCEPTED ->
            "No accepted jobs yet" to "Jobs you accept will show up here."
        JobStatus.COMPLETED ->
            "No completed jobs yet" to "Your finished deliveries will appear here."
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
                imageVector = Icons.Outlined.Description,
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

internal fun formatKsh(amount: Int): String = "KSh %,d".format(Locale.US, amount)

@Preview(showSystemUi = true)
@Composable
private fun JobsScreenPreview() {
    KilivanaTheme {
        JobsScreen(
            uiState = JobsUiState(jobs = sampleJobs()),
            onStatusSelected = {},
            onFilterClick = {},
            onJobClick = {}
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun JobsScreenEmptyPreview() {
    KilivanaTheme {
        JobsScreen(
            uiState = JobsUiState(selectedStatus = JobStatus.ACCEPTED, jobs = sampleJobs()),
            onStatusSelected = {},
            onFilterClick = {},
            onJobClick = {}
        )
    }
}
