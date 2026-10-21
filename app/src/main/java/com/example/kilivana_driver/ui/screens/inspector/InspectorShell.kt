package com.example.kilivana_driver.ui.screens.inspector

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kilivana_driver.data.model.AuthUser
import com.example.kilivana_driver.data.model.InspectionDto
import com.example.kilivana_driver.data.model.InspectionFinding
import com.example.kilivana_driver.data.model.InspectionStatus
import com.example.kilivana_driver.data.network.InspectorRepository
import com.example.kilivana_driver.data.network.SessionStore
import com.example.kilivana_driver.ui.theme.KilivanaAmber
import com.example.kilivana_driver.ui.theme.KilivanaAmberTint
import com.example.kilivana_driver.ui.theme.KilivanaGreen
import com.example.kilivana_driver.ui.theme.KilivanaGreenCard
import com.example.kilivana_driver.ui.theme.KilivanaGreenTint
import com.example.kilivana_driver.ui.theme.KilivanaTextMuted
import com.example.kilivana_driver.ui.theme.KilivanaTextPrimary
import com.example.kilivana_driver.ui.theme.KilivanaWhite
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectorShell(
    onLogout: () -> Unit
) {
    val user by SessionStore.currentUser.collectAsState()
    val repository = remember { InspectorRepository() }
    val scope = rememberCoroutineScope()

    var assignments by remember { mutableStateOf<List<InspectionDto>>(emptyList()) }
    var selected by remember { mutableStateOf<InspectionDto?>(null) }
    var findingNotes by remember { mutableStateOf("") }
    var selectedFinding by remember { mutableStateOf(InspectionFinding.APPROVED) }
    var isLoading by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var selectedTab by rememberSaveable { mutableStateOf(InspectorTab.DASHBOARD) }

    fun refreshAssignments() {
        scope.launch {
            isLoading = true
            error = null
            try {
                assignments = repository.getAssignments()
                selected?.id?.let { selectedId ->
                    selected = assignments.firstOrNull { it.id == selectedId }
                }
            } catch (e: Exception) {
                error = e.message ?: "Could not load your assignments."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(user?.id, user?.role) {
        if (user?.role.equals("INSPECTOR", ignoreCase = true)) {
            refreshAssignments()
        }
    }

    fun saveStatus(item: InspectionDto, status: InspectionStatus) {
        scope.launch {
            isSaving = true
            error = null
            message = null
            try {
                val updated = repository.updateStatus(item.id ?: 0L, status)
                assignments = assignments.map { if (it.id == updated.id) updated else it }
                selected = updated
                message = "Inspection status updated."
            } catch (e: Exception) {
                error = e.message ?: "Could not update the inspection status."
            } finally {
                isSaving = false
            }
        }
    }

    fun saveFinding(item: InspectionDto) {
        scope.launch {
            isSaving = true
            error = null
            message = null
            try {
                val updated = repository.submitFinding(
                    item.id ?: 0L,
                    selectedFinding,
                    findingNotes
                )
                assignments = assignments.map { if (it.id == updated.id) updated else it }
                selected = updated
                findingNotes = updated.notes.orEmpty()
                message = "Inspection findings submitted."
            } catch (e: Exception) {
                error = e.message ?: "Could not submit the findings."
            } finally {
                isSaving = false
            }
        }
    }

    val openInspection: (InspectionDto) -> Unit = { item ->
        selected = item
        findingNotes = item.notes.orEmpty()
        selectedFinding = InspectionFinding.entries
            .firstOrNull { it.name == item.result }
            ?: InspectionFinding.APPROVED
        error = null
        message = null
    }

    val pendingCount = assignments.count {
        it.status == "ASSIGNED" || it.status == "PENDING"
    }
    val inProgressCount = assignments.count { it.status == "IN_PROGRESS" }
    val completedCount = assignments.count { it.status == "COMPLETED" }

    Scaffold(
        containerColor = KilivanaWhite,
        bottomBar = {
            InspectorBottomBar(
                selected = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { padding ->
        when (selectedTab) {
            InspectorTab.DASHBOARD -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(KilivanaWhite)
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    InspectorHeader(
                        user = user,
                        onNotificationsClick = { /* TODO: inspector notifications */ },
                        onLogout = onLogout
                    )
                }
                item {
                    InspectorSummary(
                        assignments = assignments.size,
                        pending = pendingCount,
                        inProgress = inProgressCount,
                        completed = completedCount
                    )
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Recent inspections",
                            color = KilivanaGreen,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { selectedTab = InspectorTab.INSPECTIONS }) {
                            Text("View all", color = KilivanaGreen)
                        }
                    }
                }
                if (assignments.isEmpty() && !isLoading) {
                    item {
                        Text(
                            "No inspections are currently assigned to your account.",
                            color = KilivanaGreen,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    items(
                        assignments.take(3),
                        key = { it.id ?: it.hashCode().toLong() }
                    ) { item ->
                        InspectionCard(item = item, onClick = { openInspection(item) })
                    }
                }
                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = KilivanaGreen)
                        }
                    }
                }
                if (error != null) {
                    item {
                        Text(
                            text = error.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }

            InspectorTab.INSPECTIONS -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(KilivanaWhite)
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    InspectorHeader(
                        user = user,
                        onNotificationsClick = { /* TODO: inspector notifications */ },
                        onLogout = onLogout
                    )
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "My inspections",
                            color = KilivanaGreen,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = { refreshAssignments() },
                            enabled = !isLoading && !isSaving
                        ) {
                            Text("Refresh", color = KilivanaGreen)
                        }
                    }
                }
                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = KilivanaGreen)
                        }
                    }
                }
                if (assignments.isEmpty() && !isLoading && error == null) {
                    item {
                        Text(
                            "No inspections are currently assigned to your account.",
                            color = KilivanaGreen,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
                items(assignments, key = { it.id ?: it.hashCode().toLong() }) { item ->
                    InspectionCard(item = item, onClick = { openInspection(item) })
                }
                if (error != null) {
                    item {
                        Text(
                            text = error.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
                if (message != null) {
                    item {
                        Text(
                            text = message.orEmpty(),
                            color = KilivanaGreen,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }

            InspectorTab.PROFILE -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(KilivanaWhite)
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                InspectorHeader(
                    user = user,
                    onNotificationsClick = { /* TODO: inspector notifications */ },
                    onLogout = onLogout
                )
                Text(
                    text = "Profile",
                    color = KilivanaGreen,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
                )
                InspectorProfileCard(user = user)
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    selected?.let { item ->
        AlertDialog(
            onDismissRequest = { if (!isSaving) selected = null },
            title = {
                Text("Inspection #${item.id}")
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(440.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Target: ${item.targetType ?: "Unknown"} #${item.targetId ?: "—"}")
                    Text("Current status: ${item.status ?: "Unknown"}")
                    Text("Current finding: ${item.result ?: "Not submitted"}")

                    Text("Update status", color = KilivanaTextPrimary, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InspectionStatus.entries.forEach { status ->
                            OutlinedButton(
                                onClick = { saveStatus(item, status) },
                                enabled = !isSaving && item.status != status.name,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (item.status == status.name) KilivanaWhite
                                    else KilivanaGreen
                                )
                            ) {
                                Text(status.label)
                            }
                        }
                    }

                    Text("Inspection finding", color = KilivanaTextPrimary, fontWeight = FontWeight.SemiBold)
                    InspectionFinding.entries.forEach { finding ->
                        OutlinedButton(
                            onClick = { selectedFinding = finding },
                            enabled = !isSaving,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (selectedFinding == finding) KilivanaWhite
                                else KilivanaGreen
                            )
                        ) {
                            Text(
                                if (selectedFinding == finding) "✓ ${finding.label}"
                                else finding.label
                            )
                        }
                    }

                    OutlinedTextField(
                        value = findingNotes,
                        onValueChange = { findingNotes = it.take(4000) },
                        label = { Text("Findings and notes", color = KilivanaTextMuted) },
                        placeholder = { Text("Describe the inspection outcome", color = KilivanaTextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                        enabled = !isSaving,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = KilivanaGreen,
                            unfocusedBorderColor = KilivanaGreen.copy(alpha = 0.4f),
                            focusedLabelColor = KilivanaGreen,
                            unfocusedLabelColor = KilivanaTextMuted
                        )
                    )

                    if (error != null) {
                        Text(
                            error.orEmpty(),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (message != null) {
                        Text(
                            message.orEmpty(),
                            color = KilivanaGreen
                        )
                    }
                    if (isSaving) {
                        CircularProgressIndicator()
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { saveFinding(item) },
                    enabled = !isSaving && findingNotes.trim().isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KilivanaGreen,
                        contentColor = KilivanaWhite
                    )
                ) {
                    Text(if (isSaving) "Saving…" else "Submit findings")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { selected = null },
                    enabled = !isSaving
                ) {
                    Text("Close", color = KilivanaGreen)
                }
            }
        )
    }
}

@Composable
private fun CountCard(
    label: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Card(
        modifier = modifier
            .shadow(4.dp, shape)
            .clip(shape)
            .border(1.dp, KilivanaGreenTint, shape),
        colors = CardDefaults.cardColors(
            containerColor = KilivanaWhite,
            contentColor = KilivanaGreen
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                label,
                color = KilivanaGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                count.toString(),
                color = KilivanaGreen,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun InspectorHeader(
    user: AuthUser?,
    onNotificationsClick: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(KilivanaGreenCard)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 36.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Inspector portal",
                    color = KilivanaWhite.copy(alpha = 0.78f),
                    fontSize = 13.sp
                )
                Text(
                    text = user?.name?.takeIf { it.isNotBlank() } ?: "Inspector",
                    color = KilivanaWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onNotificationsClick) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = KilivanaWhite
                )
            }
            TextButton(onClick = onLogout) {
                Text("Log out", color = KilivanaWhite)
            }
        }
        user?.referenceCode?.takeIf { it.isNotBlank() }?.let { referenceCode ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Inspector ID: $referenceCode",
                color = KilivanaWhite,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(KilivanaWhite.copy(alpha = 0.16f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

private fun Modifier.pullUp(amount: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val overlap = amount.roundToPx().coerceAtMost(placeable.height)
    layout(placeable.width, placeable.height - overlap) {
        placeable.place(0, -overlap)
    }
}

@Composable
private fun InspectionCard(
    item: InspectionDto,
    onClick: () -> Unit
) {
    val status = InspectionStatus.from(item.status)?.label
        ?: item.status?.replace('_', ' ')?.lowercase()?.replaceFirstChar { it.uppercase() }
        ?: "Unknown"
    val finding = InspectionFinding.entries
        .firstOrNull { it.name == item.result }
        ?.label
        ?: item.result?.replace('_', ' ')?.lowercase()?.replaceFirstChar { it.uppercase() }
        ?: "Finding not submitted"
    val shape = RoundedCornerShape(14.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(2.dp, shape)
            .clip(shape)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = KilivanaWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, KilivanaGreenTint)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Inspection #${item.id ?: "—"}",
                    color = KilivanaTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = status,
                    color = KilivanaGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(KilivanaGreenTint)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
            Text(
                text = "${item.targetType ?: "Target"} #${item.targetId ?: "—"}",
                color = KilivanaGreen,
                fontSize = 13.sp
            )
            Text(
                text = finding,
                color = KilivanaGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            item.inspectedAt?.takeIf { it.isNotBlank() }?.let { inspectedAt ->
                Text(
                    text = "Inspected: $inspectedAt",
                    color = KilivanaGreen,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun InspectorSummary(
    assignments: Int,
    pending: Int,
    inProgress: Int,
    completed: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .pullUp(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CountCard("Assigned", assignments, Modifier.weight(1f))
                CountCard("Pending", pending, Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CountCard("In progress", inProgress, Modifier.weight(1f))
                CountCard("Completed", completed, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun InspectorProfileCard(user: AuthUser?) {
    val profileDetails = listOf(
        "Name" to (user?.name?.takeIf { it.isNotBlank() } ?: "Inspector"),
        "Inspector ID" to (user?.referenceCode?.takeIf { it.isNotBlank() } ?: user?.id?.toString().orEmpty()),
        "Email" to (user?.email?.takeIf { it.isNotBlank() } ?: "Not provided"),
        "Phone" to (user?.phone?.takeIf { it.isNotBlank() } ?: "Not provided"),
        "Region" to (user?.region?.takeIf { it.isNotBlank() } ?: "Not provided")
    )
    val shape = RoundedCornerShape(14.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(1.dp, KilivanaGreenTint, shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = KilivanaWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            profileDetails.forEach { (label, value) ->
                Column {
                    Text(label, color = KilivanaGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(value, color = KilivanaGreen, fontSize = 15.sp)
                }
            }
        }
    }
}

private enum class InspectorTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DASHBOARD("Dashboard", Icons.Outlined.Home),
    INSPECTIONS("My inspections", Icons.AutoMirrored.Outlined.Assignment),
    PROFILE("Profile", Icons.Outlined.Person)
}

@Composable
private fun InspectorBottomBar(
    selected: InspectorTab,
    onTabSelected: (InspectorTab) -> Unit
) {
    NavigationBar(
        containerColor = KilivanaWhite,
        tonalElevation = 0.dp
    ) {
        InspectorTab.entries.forEach { tab ->
            val isSelected = selected == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = {
                    Text(
                        text = tab.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = KilivanaGreen,
                    selectedTextColor = KilivanaGreen,
                    unselectedIconColor = KilivanaTextMuted,
                    unselectedTextColor = KilivanaTextMuted,
                    indicatorColor = KilivanaGreenTint
                )
            )
        }
    }
}
