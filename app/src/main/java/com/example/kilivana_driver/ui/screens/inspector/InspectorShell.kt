package com.example.kilivana_driver.ui.screens.inspector

import androidx.compose.foundation.verticalScroll

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kilivana_driver.data.model.AuthUser
import com.example.kilivana_driver.data.model.InspectionDto
import com.example.kilivana_driver.data.model.InspectionFinding
import com.example.kilivana_driver.data.model.InspectionStatus
import com.example.kilivana_driver.data.network.InspectorRepository
import com.example.kilivana_driver.data.network.SessionStore
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

    val pendingCount = assignments.count {
        it.status == "ASSIGNED" || it.status == "PENDING"
    }
    val inProgressCount = assignments.count { it.status == "IN_PROGRESS" }
    val completedCount = assignments.count { it.status == "COMPLETED" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inspector dashboard") },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Sign out")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Hello, ${user?.name?.takeIf { it.isNotBlank() } ?: "Inspector"}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Review your assigned inspections and record findings.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CountCard("Assigned", assignments.size, Modifier.weight(1f))
                    CountCard("Pending", pendingCount, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CountCard("In progress", inProgressCount, Modifier.weight(1f))
                    CountCard("Completed", completedCount, Modifier.weight(1f))
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "My inspections",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = { refreshAssignments() },
                        enabled = !isLoading && !isSaving
                    ) {
                        Text("Refresh")
                    }
                }
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (assignments.isEmpty() && !isLoading && error == null) {
                item {
                    Text("No inspections are currently assigned to your account.")
                }
            }

            items(assignments, key = { it.id ?: it.hashCode().toLong() }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selected = item
                            findingNotes = item.notes.orEmpty()
                            selectedFinding = InspectionFinding.entries
                                .firstOrNull { it.name == item.result }
                                ?: InspectionFinding.APPROVED
                            error = null
                            message = null
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "${item.targetType ?: "Inspection"} #${item.targetId ?: "—"}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("Inspection ID: ${item.id ?: "—"}")
                        Text("Status: ${item.status ?: "Unknown"}")
                        Text("Finding: ${item.result ?: "Not submitted"}")
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Open inspection",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (error != null) {
                item {
                    Text(
                        text = error.orEmpty(),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (message != null) {
                item {
                    Text(
                        text = message.orEmpty(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
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

                    Text("Update status", fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InspectionStatus.entries.forEach { status ->
                            OutlinedButton(
                                onClick = { saveStatus(item, status) },
                                enabled = !isSaving && item.status != status.name
                            ) {
                                Text(status.label)
                            }
                        }
                    }

                    Text("Inspection finding", fontWeight = FontWeight.SemiBold)
                    InspectionFinding.entries.forEach { finding ->
                        OutlinedButton(
                            onClick = { selectedFinding = finding },
                            enabled = !isSaving
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
                        label = { Text("Findings and notes") },
                        placeholder = { Text("Describe the inspection outcome") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                        enabled = !isSaving
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
                            color = MaterialTheme.colorScheme.primary
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
                    enabled = !isSaving && findingNotes.trim().isNotEmpty()
                ) {
                    Text(if (isSaving) "Saving…" else "Submit findings")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { selected = null },
                    enabled = !isSaving
                ) {
                    Text("Close")
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
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
