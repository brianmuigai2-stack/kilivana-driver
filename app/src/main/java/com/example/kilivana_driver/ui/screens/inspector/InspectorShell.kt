package com.example.kilivana_driver.ui.screens.inspector

import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Close
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.kilivana_driver.data.model.AuthUser
import com.example.kilivana_driver.data.model.InspectionDto
import com.example.kilivana_driver.data.model.InspectionEvidenceImage
import com.example.kilivana_driver.data.model.InspectionFinding
import com.example.kilivana_driver.data.model.InspectionStatus
import com.example.kilivana_driver.data.network.InspectorRepository
import com.example.kilivana_driver.data.network.SessionStore
import com.example.kilivana_driver.ui.theme.KilivanaAmber
import com.example.kilivana_driver.ui.theme.KilivanaBlue
import com.example.kilivana_driver.ui.theme.KilivanaBorder
import com.example.kilivana_driver.ui.theme.KilivanaGreen
import com.example.kilivana_driver.ui.theme.KilivanaGreenCard
import com.example.kilivana_driver.ui.theme.KilivanaGreenDark
import com.example.kilivana_driver.ui.theme.KilivanaGreenTint
import com.example.kilivana_driver.ui.theme.KilivanaBackground
import com.example.kilivana_driver.ui.theme.KilivanaTextMuted
import com.example.kilivana_driver.ui.theme.KilivanaTextPrimary
import com.example.kilivana_driver.ui.theme.KilivanaWhite
import kotlinx.coroutines.launch

private val InspectorCanvas = Color(0xFFF1F4F1)
private val InspectorQuestionSurface = Color(0xFFF6F8F6)
private val InspectorCardBorder = Color(0xFFDCE4DD)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectorShell(
    onLogout: () -> Unit
) {
    val user by SessionStore.currentUser.collectAsState()
    val repository = remember { InspectorRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var assignments by remember { mutableStateOf<List<InspectionDto>>(emptyList()) }
    var selected by remember { mutableStateOf<InspectionDto?>(null) }
    var evidenceImages by remember { mutableStateOf<List<InspectionEvidenceImage>>(emptyList()) }
    var findingNotes by remember { mutableStateOf("") }
    var selectedFinding by remember { mutableStateOf<InspectionFinding?>(null) }
    var checklistAnswers by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var customChecklistQuestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var newChecklistQuestion by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingDetail by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var selectedTab by rememberSaveable { mutableStateOf(InspectorTab.DASHBOARD) }
    var queueFilter by rememberSaveable { mutableStateOf("All") }
    var searchQuery by rememberSaveable { mutableStateOf("") }

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

    LaunchedEffect(selectedTab) {
        queueFilter = "All"
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

    fun removeEvidence(item: InspectionDto, image: InspectionEvidenceImage) {
        scope.launch {
            isSaving = true
            error = null
            message = null
            try {
                repository.deleteEvidence(item.id ?: 0L, image.id)
                evidenceImages = evidenceImages.filterNot { it.id == image.id }
                message = "Evidence photo removed."
            } catch (e: Exception) {
                error = e.message ?: "Could not remove this evidence photo."
            } finally {
                isSaving = false
            }
        }
    }

    fun saveFinding(item: InspectionDto, evidenceUris: List<Uri>) {
        scope.launch {
            isSaving = true
            error = null
            message = null
            try {
                val finding = selectedFinding ?: return@launch
                evidenceUris.forEach { uri ->
                    val uploaded = repository.uploadEvidence(
                        item.id ?: 0L,
                        context.contentResolver,
                        uri,
                        context.cacheDir
                    )
                    evidenceImages = (evidenceImages + uploaded).distinctBy { it.id }
                }
                val updated = repository.submitFinding(
                    item.id ?: 0L,
                    finding,
                    encodeInspectionNotes(
                        questions = checklistQuestionsFor(item.targetType) + customChecklistQuestions,
                        answers = checklistAnswers,
                        observations = findingNotes
                    )
                )
                assignments = assignments.map { if (it.id == updated.id) updated else it }
                selected = updated
                val parsedNotes = decodeInspectionNotes(updated.notes.orEmpty(), updated.targetType)
                findingNotes = parsedNotes.observations
                checklistAnswers = parsedNotes.answers
                customChecklistQuestions = parsedNotes.customQuestions
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
        evidenceImages = emptyList()
        val parsedNotes = decodeInspectionNotes(item.notes.orEmpty(), item.targetType)
        findingNotes = parsedNotes.observations
        checklistAnswers = parsedNotes.answers
        customChecklistQuestions = parsedNotes.customQuestions
        newChecklistQuestion = ""
        selectedFinding = InspectionFinding.entries.firstOrNull { it.name == item.result }
        error = null
        message = null
        item.id?.let { inspectionId ->
            scope.launch {
                isLoadingDetail = true
                try {
                    val details = repository.getInspection(inspectionId)
                    selected = details
                    val latestNotes = decodeInspectionNotes(details.notes.orEmpty(), details.targetType)
                    findingNotes = latestNotes.observations
                    checklistAnswers = latestNotes.answers
                    customChecklistQuestions = latestNotes.customQuestions
                    selectedFinding = InspectionFinding.entries.firstOrNull { it.name == details.result }
                    try {
                        evidenceImages = repository.getEvidence(inspectionId)
                    } catch (e: Exception) {
                        error = e.message ?: "Could not load existing evidence photos."
                    }
                } catch (e: Exception) {
                    error = e.message ?: "Could not load inspection details."
                } finally {
                    isLoadingDetail = false
                }
            }
        }
    }

    val assignedCount = assignments.count { it.status == "ASSIGNED" }
    val pendingCount = assignments.count { it.status == "PENDING" }
    val completedCount = assignments.count { it.status == "COMPLETED" }
    val rejectedCount = assignments.count { it.result == InspectionFinding.REJECTED.name }
    val historyItems = assignments.filter {
        it.status == "COMPLETED" || it.status == "CANCELLED" || !it.result.isNullOrBlank()
    }
    val sourceItems = if (selectedTab == InspectorTab.HISTORY) historyItems else assignments
    val filteredItems = sourceItems.filter { item ->
        val query = searchQuery.trim()
        val matchesQuery = query.isBlank() || listOfNotNull(
            item.id?.toString(), item.targetType, item.targetId?.toString(), item.status,
            item.result, item.notes
        ).any { it.contains(query, ignoreCase = true) }
        val matchesFilter = when (queueFilter) {
            "Assigned" -> item.status == "ASSIGNED"
            "Pending" -> item.status == "PENDING"
            "In progress" -> item.status == "IN_PROGRESS"
            "Completed" -> item.status == "COMPLETED"
            "Rejected" -> item.result == InspectionFinding.REJECTED.name
            else -> true
        }
        matchesQuery && matchesFilter
    }

    if (selected == null) Scaffold(
        containerColor = KilivanaBackground,
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
                    .background(KilivanaBackground)
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    InspectorHeader(user = user, onLogout = onLogout)
                }
                item {
                    InspectorSummary(
                        assigned = assignedCount,
                        pending = pendingCount,
                        completed = completedCount,
                        rejected = rejectedCount
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
                            color = KilivanaTextPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { selectedTab = InspectorTab.INSPECTIONS }) {
                            Text("View all", color = KilivanaGreen)
                        }
                    }
                }
                if (assignments.isEmpty() && !isLoading && error == null) {
                    item {
                        InspectorEmptyState(onRefresh = ::refreshAssignments)
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

            InspectorTab.INSPECTIONS, InspectorTab.HISTORY -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(KilivanaBackground)
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    InspectorHeader(user = user, onLogout = onLogout)
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
                            if (selectedTab == InspectorTab.HISTORY) "Inspection history" else "Inspection queue",
                            color = KilivanaTextPrimary,
                            fontSize = 19.sp,
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
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                        singleLine = true,
                        placeholder = { Text("Search target, result or notes") },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = KilivanaGreen,
                            unfocusedBorderColor = KilivanaBorder,
                            focusedLabelColor = KilivanaGreen
                        )
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val filters = if (selectedTab == InspectorTab.HISTORY) listOf("All", "Completed", "Rejected")
                        else listOf("All", "Assigned", "Pending", "In progress", "Completed", "Rejected")
                        filters.forEach { filter ->
                            FilterChip(
                                selected = queueFilter == filter,
                                onClick = { queueFilter = filter },
                                label = { Text(filter) },
                                shape = RoundedCornerShape(50),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = KilivanaGreen,
                                    selectedLabelColor = KilivanaWhite
                                )
                            )
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
                if (filteredItems.isEmpty() && !isLoading && error == null) {
                    item {
                        InspectorEmptyState(
                            title = if (selectedTab == InspectorTab.HISTORY) "No history to show" else "No inspections found",
                            message = if (searchQuery.isNotBlank() || queueFilter != "All") "Try another search or filter." else "Assigned inspections will appear here.",
                            onRefresh = ::refreshAssignments
                        )
                    }
                }
                items(filteredItems, key = { it.id ?: it.hashCode().toLong() }) { item ->
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
                    .background(KilivanaBackground)
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                InspectorHeader(user = user, onLogout = onLogout)
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
    } else {
        selected?.let { item ->
            val checklistQuestions = checklistQuestionsFor(item.targetType) + customChecklistQuestions
            val checklistComplete = checklistQuestions.all { checklistAnswers[it] != null }
            InspectorPerformInspectionScreen(
                item = item,
                evidenceImages = evidenceImages,
                checklistQuestions = checklistQuestions,
                checklistAnswers = checklistAnswers,
                findingNotes = findingNotes,
                selectedFinding = selectedFinding,
                customQuestion = newChecklistQuestion,
                customQuestionCount = customChecklistQuestions.size,
                checklistComplete = checklistComplete,
                isSaving = isSaving,
                isLoadingDetail = isLoadingDetail,
                error = error,
                message = message,
                onBack = { if (!isSaving) selected = null },
                onStart = { saveStatus(item, InspectionStatus.IN_PROGRESS) },
                onAnswer = { question, answer -> checklistAnswers = checklistAnswers + (question to answer) },
                onCustomQuestionChange = { newChecklistQuestion = it.take(120) },
                onAddCustomQuestion = {
                    val question = newChecklistQuestion.trim()
                    if (question.isNotEmpty() && question !in checklistQuestions && customChecklistQuestions.size < 8) {
                        customChecklistQuestions = customChecklistQuestions + question
                        newChecklistQuestion = ""
                    }
                },
                onFindingSelected = { selectedFinding = it },
                onNotesChange = { findingNotes = it.take(2000) },
                onDeleteEvidence = { image -> removeEvidence(item, image) },
                onSubmit = { evidenceUris -> saveFinding(item, evidenceUris) }
            )
        }
    }
}

@Composable
private fun InspectorPerformInspectionScreen(
    item: InspectionDto,
    evidenceImages: List<InspectionEvidenceImage>,
    checklistQuestions: List<String>,
    checklistAnswers: Map<String, String>,
    findingNotes: String,
    selectedFinding: InspectionFinding?,
    customQuestion: String,
    customQuestionCount: Int,
    checklistComplete: Boolean,
    isSaving: Boolean,
    isLoadingDetail: Boolean,
    error: String?,
    message: String?,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onAnswer: (String, String) -> Unit,
    onCustomQuestionChange: (String) -> Unit,
    onAddCustomQuestion: () -> Unit,
    onFindingSelected: (InspectionFinding) -> Unit,
    onNotesChange: (String) -> Unit,
    onDeleteEvidence: (InspectionEvidenceImage) -> Unit,
    onSubmit: (List<Uri>) -> Unit
) {
    val isInProgress = item.status == InspectionStatus.IN_PROGRESS.name
    val canStart = item.status == InspectionStatus.ASSIGNED.name || item.status == InspectionStatus.PENDING.name
    val canSubmit = isInProgress && checklistComplete && selectedFinding != null
    val actionEnabled = !isSaving && !isLoadingDetail && (canStart || canSubmit)
    var selectedEvidence by remember(item.id) { mutableStateOf<List<Uri>>(emptyList()) }
    val evidencePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris -> selectedEvidence = uris }
    BackHandler(enabled = true, onBack = onBack)

    Scaffold(
        containerColor = InspectorCanvas,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KilivanaWhite)
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, enabled = !isSaving) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = KilivanaTextPrimary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Inspection", color = KilivanaTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Assignment #${item.id ?: "—"}", color = KilivanaTextMuted, fontSize = 12.sp)
                }
                StatusPill(item.status)
            }
        },
        bottomBar = {
            Surface(color = KilivanaWhite, shadowElevation = 10.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = { if (canStart) onStart() else onSubmit(selectedEvidence) },
                        enabled = actionEnabled,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KilivanaGreen,
                            contentColor = KilivanaWhite
                        )
                    ) {
                        if (isSaving || isLoadingDetail) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = KilivanaWhite)
                        } else {
                            Text(
                                when {
                                    canStart -> "Start inspection"
                                    isInProgress -> "Submit inspection"
                                    item.status == InspectionStatus.COMPLETED.name -> "Inspection completed"
                                    else -> "Inspection unavailable"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (isInProgress && !checklistComplete) {
                        Text(
                            "Complete every checklist item to submit.",
                            modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
                            textAlign = TextAlign.Center,
                            color = KilivanaTextMuted,
                            fontSize = 11.sp
                        )
                    } else if (isInProgress && selectedFinding == null) {
                        Text(
                            "Choose an inspection result before submitting.",
                            modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
                            textAlign = TextAlign.Center,
                            color = KilivanaTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = KilivanaGreenCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                .background(Brush.linearGradient(listOf(KilivanaGreenDark, KilivanaGreenCard)))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Text("INSPECTION SUBJECT", color = KilivanaWhite.copy(alpha = 0.72f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                    Text(
                        text = item.targetType?.replace('_', ' ')?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Assigned subject",
                        color = KilivanaWhite,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Reference #${item.targetId ?: "—"}", color = KilivanaWhite.copy(alpha = 0.82f), fontSize = 13.sp)
                    item.targetFarm?.let { farm ->
                        val farmName = farm.farmName?.takeIf(String::isNotBlank)
                            ?: farm.name?.takeIf(String::isNotBlank)
                        farmName?.let { Text(it, color = KilivanaWhite, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
                        farm.ownerName?.takeIf(String::isNotBlank)?.let {
                            Text("Owner · $it", color = KilivanaWhite.copy(alpha = 0.88f), fontSize = 13.sp)
                        }
                        listOfNotNull(farm.location, farm.subCounty, farm.county)
                            .filter(String::isNotBlank).distinct().joinToString(" · ")
                            .takeIf(String::isNotBlank)?.let {
                                Text(it, color = KilivanaWhite.copy(alpha = 0.84f), fontSize = 12.sp)
                            }
                        farm.ownerPhone?.takeIf(String::isNotBlank)?.let {
                            Text(it, color = KilivanaWhite.copy(alpha = 0.84f), fontSize = 12.sp)
                        }
                    }
                    item.inspectedAt?.takeIf { it.isNotBlank() }?.let {
                        Text("Inspection date · $it", color = KilivanaWhite.copy(alpha = 0.82f), fontSize = 12.sp)
                    }
                }
            }

            SectionCard(
                title = "Checklist",
                subtitle = "Review each item and record its outcome.",
                trailing = "${checklistQuestions.count { checklistAnswers[it] != null }} / ${checklistQuestions.size}"
            ) {
                checklistQuestions.forEach { question ->
                    ChecklistQuestionRow(
                        question = question,
                        answer = checklistAnswers[question],
                        enabled = !isSaving,
                        onAnswer = { answer -> onAnswer(question, answer) }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customQuestion,
                        onValueChange = onCustomQuestionChange,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Add a custom check") },
                        shape = RoundedCornerShape(14.dp),
                        enabled = !isSaving && customQuestionCount < 8
                    )
                    TextButton(
                        onClick = onAddCustomQuestion,
                        enabled = !isSaving && customQuestion.isNotBlank() && customQuestionCount < 8
                    ) { Text("Add") }
                }
                val answeredCount = checklistQuestions.count { checklistAnswers[it] != null }
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { if (checklistQuestions.isEmpty()) 0f else answeredCount.toFloat() / checklistQuestions.size },
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                    color = KilivanaGreen,
                    trackColor = KilivanaGreenTint
                )
                Text(
                    if (checklistComplete) "Checklist complete" else "$answeredCount of ${checklistQuestions.size} checks completed",
                    color = if (checklistComplete) KilivanaGreen else KilivanaTextMuted,
                    fontSize = 11.sp,
                    fontWeight = if (checklistComplete) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            SectionCard(title = "Inspection result", subtitle = "Select the outcome supported by your findings.") {
                InspectionFinding.entries.forEach { finding ->
                    val selected = selectedFinding == finding
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(15.dp))
                            .clickable(enabled = !isSaving) { onFindingSelected(finding) },
                        shape = RoundedCornerShape(15.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) KilivanaGreenTint else KilivanaWhite
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selected) KilivanaGreen else KilivanaBorder.copy(alpha = 0.6f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) KilivanaGreen else InspectorCanvas),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) Text("✓", color = KilivanaWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(finding.label, color = KilivanaTextPrimary, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                        }
                    }
                }
            }

            SectionCard(title = "Observations", subtitle = "Add notes that explain your decision.") {
                OutlinedTextField(
                    value = findingNotes,
                    onValueChange = onNotesChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Record conditions, discrepancies, or follow-up actions…") },
                    minLines = 4,
                    maxLines = 7,
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isSaving,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KilivanaGreen,
                        unfocusedBorderColor = KilivanaBorder,
                        focusedLabelColor = KilivanaGreen
                    )
                )
            }

            SectionCard(title = "Evidence photos", subtitle = "Add clear photos that support your inspection result.") {
                OutlinedButton(
                    onClick = {
                        evidencePicker.launch(
                            androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Outlined.AddAPhoto, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(if (selectedEvidence.isEmpty()) "Choose photos" else "Change selected photos")
                }
                Text(
                    "Selected photos upload securely when you submit the inspection.",
                    color = KilivanaTextMuted,
                    fontSize = 11.sp
                )
                if (selectedEvidence.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        selectedEvidence.forEachIndexed { index, uri ->
                            Box(
                                modifier = Modifier
                                    .size(84.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.dp, InspectorCardBorder, RoundedCornerShape(14.dp))
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Selected evidence photo ${index + 1}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(KilivanaTextPrimary.copy(alpha = 0.76f))
                                        .clickable { selectedEvidence = selectedEvidence.filterNot { it == uri } },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Remove photo", tint = KilivanaWhite, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                    Text(
                        "${selectedEvidence.size} photo${if (selectedEvidence.size == 1) "" else "s"} ready to upload.",
                        color = KilivanaAmber,
                        fontSize = 11.sp
                    )
                }
                if (isLoadingDetail) {
                    InlineNotice("Loading uploaded evidence…", isProgress = true)
                } else if (evidenceImages.isNotEmpty()) {
                    Text(
                        "Uploaded evidence · ${evidenceImages.size}",
                        color = KilivanaTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        evidenceImages.forEachIndexed { index, image ->
                            Box(
                                modifier = Modifier
                                    .size(84.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, InspectorCardBorder, RoundedCornerShape(10.dp))
                            ) {
                                AsyncImage(
                                    model = image.url,
                                    contentDescription = "Uploaded evidence photo ${index + 1}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { onDeleteEvidence(image) },
                                    enabled = !isSaving,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp)
                                        .size(30.dp)
                                        .background(KilivanaTextPrimary.copy(alpha = 0.76f), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Outlined.Close,
                                        contentDescription = "Delete uploaded evidence photo ${index + 1}",
                                        tint = KilivanaWhite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                } else if (item.evidenceUrls.isNullOrBlank()) {
                    Text("No evidence photos uploaded yet.", color = KilivanaTextMuted, fontSize = 12.sp)
                }
                item.evidenceUrls?.takeIf { it.isNotBlank() }?.let {
                    Text("Previously uploaded evidence", color = KilivanaTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(it, color = KilivanaGreen, fontSize = 12.sp)
                }
            }

            if (isLoadingDetail) InlineNotice("Loading the latest inspection details…", isProgress = true)
            error?.let { InlineNotice(it, isError = true) }
            message?.let { InlineNotice(it) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    trailing: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = KilivanaWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, InspectorCardBorder)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(title, color = KilivanaTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = KilivanaTextMuted, fontSize = 12.sp)
                }
                trailing?.let {
                    Text(it, color = KilivanaGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            content()
        }
    }
}

@Composable
private fun InlineNotice(
    text: String,
    isError: Boolean = false,
    isProgress: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.08f) else KilivanaGreenTint)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (isProgress) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = KilivanaGreen)
        Text(text, color = if (isError) MaterialTheme.colorScheme.error else KilivanaGreen, fontSize = 12.sp)
    }
}

@Composable
private fun ChecklistQuestionRow(
    question: String,
    answer: String?,
    enabled: Boolean,
    onAnswer: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(InspectorQuestionSurface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(question, color = KilivanaTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("PASS", "ISSUE", "N/A").forEach { option ->
                val label = when (option) {
                    "PASS" -> "Pass"
                    "ISSUE" -> "Issue"
                    else -> "N/A"
                }
                FilterChip(
                    selected = answer == option,
                    onClick = { onAnswer(option) },
                    enabled = enabled,
                    label = { Text(label, fontSize = 11.sp) },
                    shape = RoundedCornerShape(50),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = when (option) {
                            "PASS" -> KilivanaGreen
                            "ISSUE" -> MaterialTheme.colorScheme.error
                            else -> KilivanaTextMuted
                        },
                        selectedLabelColor = KilivanaWhite
                    )
                )
            }
        }
    }
}

private fun checklistQuestionsFor(targetType: String?): List<String> = when (targetType.orEmpty().uppercase()) {
    "PRODUCT" -> listOf(
        "Product matches the assigned item",
        "Quality and condition meet requirements",
        "Quantity and packaging are correct",
        "Storage and handling are acceptable"
    )
    "FARMER", "SUPPLIER" -> listOf(
        "Identity and registration details are verified",
        "Required documentation is available",
        "Production or supply site meets requirements",
        "Traceability records are complete"
    )
    "TRANSACTION", "DELIVERY", "ORDER" -> listOf(
        "Goods match the transaction",
        "Quantity and condition are verified",
        "Packaging and supporting documents are checked",
        "Handling and delivery conditions are acceptable"
    )
    else -> listOf(
        "Inspection subject matches the assignment",
        "Quality and condition meet requirements",
        "Quantity and supporting documents are verified",
        "Handling and storage are acceptable"
    )
}

private data class ParsedInspectionNotes(
    val observations: String,
    val answers: Map<String, String>,
    val customQuestions: List<String>
)

private fun encodeInspectionNotes(
    questions: List<String>,
    answers: Map<String, String>,
    observations: String
): String = buildString {
    appendLine("Kilivana inspection checklist")
    questions.forEach { question ->
        val answer = answers[question] ?: "N/A"
        appendLine("- [$answer] $question")
    }
    appendLine()
    appendLine("Observations")
    append(observations.trim().ifBlank { "No additional observations." })
}

private fun decodeInspectionNotes(raw: String, targetType: String?): ParsedInspectionNotes {
    val marker = "Kilivana inspection checklist\n"
    if (!raw.startsWith(marker)) return ParsedInspectionNotes(raw, emptyMap(), emptyList())

    val observationsMarker = "\n\nObservations\n"
    val observationsIndex = raw.indexOf(observationsMarker)
    val checklistBlock = if (observationsIndex >= 0) raw.substring(marker.length, observationsIndex) else raw.substring(marker.length)
    val observations = if (observationsIndex >= 0) raw.substring(observationsIndex + observationsMarker.length) else ""
    val parsedLines = checklistBlock.lineSequence().mapNotNull { line ->
        val match = Regex("^- \\[([A-Z/]+)] (.+)$").matchEntire(line) ?: return@mapNotNull null
        match.groupValues[2] to match.groupValues[1]
    }.toMap()
    val defaultQuestions = checklistQuestionsFor(targetType)
    val customQuestions = parsedLines.keys.filterNot { it in defaultQuestions }
    return ParsedInspectionNotes(observations, parsedLines, customQuestions)
}

@Composable
private fun InspectorMetric(
    label: String,
    count: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(count.toString(), color = accent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            label,
            color = KilivanaTextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun InspectorHeader(
    user: AuthUser?,
    onLogout: () -> Unit
) {
    val displayName = user?.name?.takeIf { it.isNotBlank() } ?: "Inspector"
    val firstName = displayName.trim().substringBefore(' ')
    val initials = displayName.trim().split(Regex("\\s+")).take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(Brush.verticalGradient(listOf(KilivanaGreenDark, KilivanaGreenCard)))
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 56.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(KilivanaWhite.copy(alpha = 0.18f))
                    .border(2.dp, KilivanaWhite.copy(alpha = 0.65f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(initials.ifBlank { "I" }, color = KilivanaWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("INSPECTOR", color = KilivanaWhite.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("Good day,", color = KilivanaWhite.copy(alpha = 0.84f), fontSize = 13.sp)
                Text(
                    firstName,
                    color = KilivanaWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(
                onClick = onLogout,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Sign out", color = KilivanaWhite.copy(alpha = 0.9f), fontSize = 12.sp)
            }
        }
        user?.referenceCode?.takeIf { it.isNotBlank() }?.let { referenceCode ->
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(KilivanaWhite.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "INSPECTOR ID · $referenceCode",
                    color = KilivanaWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
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
private fun StatusPill(status: String?) {
    val normalized = status?.let { InspectionStatus.from(it)?.label }
        ?: status?.replace('_', ' ')?.lowercase()?.replaceFirstChar { it.uppercase() }
        ?: "Unknown"
    val tone = when (status?.uppercase()) {
        "COMPLETED" -> KilivanaGreenCard
        "IN_PROGRESS" -> KilivanaBlue
        "ASSIGNED", "PENDING" -> KilivanaAmber
        "CANCELLED" -> KilivanaTextMuted
        else -> KilivanaGreen
    }
    Text(
        text = normalized,
        color = tone,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(tone.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
private fun InspectorEmptyState(
    title: String = "You’re all caught up",
    message: String = "New inspections assigned to you will appear here.",
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = KilivanaWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, KilivanaBorder.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(KilivanaGreenTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Outlined.Assignment, contentDescription = null, tint = KilivanaGreen)
            }
            Spacer(Modifier.height(14.dp))
            Text(title, color = KilivanaTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(
                message,
                color = KilivanaTextMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(14.dp))
            TextButton(onClick = onRefresh) { Text("Check again", color = KilivanaGreen, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun InspectionCard(
    item: InspectionDto,
    onClick: () -> Unit
) {
    val finding = InspectionFinding.entries
        .firstOrNull { it.name == item.result }
        ?.label
        ?: item.result?.replace('_', ' ')?.lowercase()?.replaceFirstChar { it.uppercase() }
        ?: "Finding not submitted"
    val shape = RoundedCornerShape(20.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .clip(shape)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = KilivanaWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, KilivanaBorder.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.targetType?.replace('_', ' ')?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Inspection"}  #${item.targetId ?: item.id ?: "—"}",
                    color = KilivanaTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                StatusPill(status = item.status)
            }
            Text(
                text = finding,
                color = KilivanaTextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            item.inspectedAt?.takeIf { it.isNotBlank() }?.let { inspectedAt ->
                Text(text = "Updated $inspectedAt", color = KilivanaTextMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun InspectorSummary(
    assigned: Int,
    pending: Int,
    completed: Int,
    rejected: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .pullUp(32.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = KilivanaWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, KilivanaBorder.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "INSPECTION OVERVIEW",
                    color = KilivanaGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InspectorMetric("Assigned", assigned, KilivanaGreen, Modifier.weight(1f))
                    InspectorMetric("Pending", pending, KilivanaAmber, Modifier.weight(1f))
                    InspectorMetric("Completed", completed, KilivanaBlue, Modifier.weight(1f))
                    InspectorMetric("Rejected", rejected, MaterialTheme.colorScheme.error, Modifier.weight(1f))
                }
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
    val shape = RoundedCornerShape(22.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .border(1.dp, KilivanaBorder.copy(alpha = 0.55f), shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = KilivanaWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            profileDetails.forEachIndexed { index, (label, value) ->
                Column {
                    Column(Modifier.padding(vertical = 13.dp)) {
                        Text(label.uppercase(), color = KilivanaTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(value, color = KilivanaTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                    if (index < profileDetails.lastIndex) {
                        androidx.compose.material3.HorizontalDivider(color = KilivanaBorder.copy(alpha = 0.45f))
                    }
                }
            }
        }
    }
}

private enum class InspectorTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DASHBOARD("Dashboard", Icons.Outlined.Home),
    INSPECTIONS("Queue", Icons.AutoMirrored.Outlined.Assignment),
    HISTORY("History", Icons.Outlined.History),
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
