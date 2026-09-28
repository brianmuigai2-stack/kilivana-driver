package com.example.kilivana_driver.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kilivana_driver.data.model.DeliveryOutcome
import com.example.kilivana_driver.data.model.DeliveryRecord
import com.example.kilivana_driver.ui.components.KilivanaStatusBarScrim
import com.example.kilivana_driver.ui.theme.KilivanaBackground
import com.example.kilivana_driver.ui.theme.KilivanaBorder
import com.example.kilivana_driver.ui.theme.KilivanaError
import com.example.kilivana_driver.ui.theme.KilivanaErrorTint
import com.example.kilivana_driver.ui.theme.KilivanaGreen
import com.example.kilivana_driver.ui.theme.KilivanaGreenCard
import com.example.kilivana_driver.ui.theme.KilivanaGreenTint
import com.example.kilivana_driver.ui.theme.KilivanaTextMuted
import com.example.kilivana_driver.ui.theme.KilivanaTextPrimary
import com.example.kilivana_driver.ui.theme.KilivanaTheme
import com.example.kilivana_driver.ui.theme.KilivanaWhite
import java.util.Locale

private enum class HistoryFilter(val label: String) {
    ALL("All"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled")
}

@Composable
fun DeliveryHistoryScreen(
    records: List<DeliveryRecord>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var filter by rememberSaveable { mutableStateOf(HistoryFilter.ALL) }

    val visible = when (filter) {
        HistoryFilter.ALL -> records
        HistoryFilter.COMPLETED -> records.filter { it.outcome == DeliveryOutcome.COMPLETED }
        HistoryFilter.CANCELLED -> records.filter { it.outcome == DeliveryOutcome.CANCELLED }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KilivanaBackground)
    ) {
        KilivanaStatusBarScrim()

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
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
                text = "Delivery History",
                color = KilivanaTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Filter chips
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HistoryFilter.values().forEach { option ->
                HistoryFilterChip(
                    text = option.label,
                    selected = option == filter,
                    onClick = { filter = option }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (visible.isEmpty()) {
                item { EmptyState(filter = filter) }
            } else {
                items(visible, key = { it.id }) { record ->
                    HistoryCard(record = record)
                }
            }
        }
    }
}

@Composable
private fun HistoryFilterChip(
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
private fun HistoryCard(record: DeliveryRecord) {
    val completed = record.outcome == DeliveryOutcome.COMPLETED
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
                .background(if (completed) KilivanaGreenTint else KilivanaErrorTint),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (completed) Icons.Outlined.LocalShipping else Icons.Filled.Close,
                contentDescription = null,
                tint = if (completed) KilivanaGreen else KilivanaError,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = record.id,
                color = KilivanaTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = record.cargo,
                color = KilivanaTextMuted,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = KilivanaTextMuted,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = record.dateLabel,
                    color = KilivanaTextMuted,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            OutcomeBadge(completed = completed)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (completed) formatKsh(record.earningsKsh) else "—",
                color = if (completed) KilivanaGreenCard else KilivanaTextMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun OutcomeBadge(completed: Boolean) {
    val textColor = if (completed) KilivanaGreen else KilivanaError
    val background = if (completed) KilivanaGreenTint else KilivanaErrorTint

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (completed) Icons.Filled.Check else Icons.Filled.Close,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (completed) "Completed" else "Cancelled",
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EmptyState(filter: HistoryFilter) {
    val (title, message) = when (filter) {
        HistoryFilter.ALL ->
            "No deliveries yet" to "Your delivery history will appear here."
        HistoryFilter.COMPLETED ->
            "No completed deliveries" to "Finished deliveries will show up here."
        HistoryFilter.CANCELLED ->
            "No cancelled deliveries" to "Nothing cancelled. Nice and clean."
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
                imageVector = Icons.Outlined.History,
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

private fun formatKsh(amount: Int): String = "KSh %,d".format(Locale.US, amount)

// TODO: replace with real delivery history from the API
internal fun sampleDeliveryHistory(): List<DeliveryRecord> = listOf(
    DeliveryRecord("AG-4587", "Vegetables (Mixed)", "12 Sep 2025", 2500, DeliveryOutcome.COMPLETED),
    DeliveryRecord("AG-4561", "Fresh Milk", "11 Sep 2025", 1500, DeliveryOutcome.COMPLETED),
    DeliveryRecord("AG-4543", "Maize", "9 Sep 2025", 4800, DeliveryOutcome.COMPLETED),
    DeliveryRecord("AG-4520", "Tomatoes", "7 Sep 2025", 1900, DeliveryOutcome.COMPLETED),
    DeliveryRecord("AG-4512", "Cabbages", "6 Sep 2025", 0, DeliveryOutcome.CANCELLED),
    DeliveryRecord("AG-4501", "Onions", "5 Sep 2025", 2200, DeliveryOutcome.COMPLETED)
)

@Preview(showSystemUi = true)
@Composable
private fun DeliveryHistoryPreview() {
    KilivanaTheme {
        DeliveryHistoryScreen(records = sampleDeliveryHistory(), onBack = {})
    }
}

@Preview(showSystemUi = true)
@Composable
private fun DeliveryHistoryEmptyPreview() {
    KilivanaTheme {
        DeliveryHistoryScreen(records = emptyList(), onBack = {})
    }
}
