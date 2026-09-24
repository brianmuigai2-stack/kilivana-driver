package com.kilivana.driver.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kilivana_driver.ui.theme.Green700
import com.example.kilivana_driver.ui.theme.Green900
import com.example.kilivana_driver.ui.theme.Green500
import com.example.kilivana_driver.ui.theme.KilivanadriverTheme
import com.kilivana.driver.ui.components.DriverBottomAppBar
import com.kilivana.driver.ui.components.DriverBottomNavDestination
import com.kilivana.driver.ui.components.ScreenScaffold

/**
 * Driver dashboard.
 *
 * Shows three job categories: Today's jobs, pending jobs and active delivery.
 * The data layer is intentionally not wired up yet, so the screen renders the
 * UI skeleton with empty states rather than fake data.
 */
data class DriverJob(
    val id: String,
    val title: String,
    val subtitle: String,
    val status: JobStatus
)

enum class JobStatus { TODAY, PENDING, ACTIVE }

@Composable
fun DriverDashboard(
    jobs: List<DriverJob> = emptyList(),
    currentDestination: DriverBottomNavDestination = DriverBottomNavDestination.Dashboard,
    onNavSelected: (DriverBottomNavDestination) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val todayJobs = jobs.filter { it.status == JobStatus.TODAY }
    val pendingJobs = jobs.filter { it.status == JobStatus.PENDING }
    val activeJobs = jobs.filter { it.status == JobStatus.ACTIVE }

    ScreenScaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.White,
        topBar = {
            // Full-bleed header background drawn to the very top of the screen,
            // including behind the status bar. The header text/icons are inset
            // below the status bar so they never overlap the phone's time,
            // battery, signal or notification icons.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Green900)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Text(
                    text = "Driver Dashboard",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Your delivery overview",
                    color = Color(0xFFCDE8D8),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        },
        bottomBar = {
            DriverBottomAppBar(
                currentDestination = currentDestination,
                onDestinationSelected = onNavSelected
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                JobSection(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    title = "Today's jobs",
                    jobs = todayJobs,
                    accent = Green700
                )
            }
            item {
                JobSection(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    title = "Pending jobs",
                    jobs = pendingJobs,
                    accent = Color(0xFFE0A458)
                )
            }
            item {
                JobSection(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    title = "Active delivery",
                    jobs = activeJobs,
                    accent = Color(0xFF4A90D9)
                )
            }
        }
    }
}

@Composable
private fun JobSection(
    modifier: Modifier = Modifier,
    title: String,
    jobs: List<DriverJob>,
    accent: Color
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(
                modifier = Modifier
                    .size(8.dp)
                    .background(accent, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = Green900,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${jobs.size}",
                color = accent,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (jobs.isEmpty()) {
            EmptyStateCard(text = "No jobs scheduled")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                jobs.forEach { job ->
                    JobCard(job = job, accent = accent)
                }
            }
        }
    }
}

@Composable
private fun JobCard(job: DriverJob, accent: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F9F7)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(
                modifier = Modifier
                    .size(10.dp)
                    .background(accent, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = job.title,
                    color = Green900,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = job.subtitle,
                    color = Color(0xFF6B6B6B),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F9F7)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            color = Color(0xFF9A9A9A),
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DriverDashboardPreviewEmpty() {
    KilivanadriverTheme {
        DriverDashboard(jobs = emptyList())
    }
}

@Preview(showBackground = true)
@Composable
fun DriverDashboardPreviewWithContent() {
    KilivanadriverTheme {
        DriverDashboard(
            jobs = listOf(
                DriverJob("1", "Delivery to Kiambu", "3 stops · 42 km", JobStatus.TODAY),
                DriverJob("2", "Pickup from Thika", "Awaiting confirmation", JobStatus.PENDING),
                DriverJob("3", "Delivery to Nairobi West", "In transit", JobStatus.ACTIVE)
            )
        )
    }
}