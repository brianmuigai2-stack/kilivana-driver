package com.example.kilivana_driver.ui.screens.inspector

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Agriculture
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.kilivana_driver.data.model.AuthUser
import com.example.kilivana_driver.data.model.DriverImage
import com.example.kilivana_driver.data.model.InspectionDto
import com.example.kilivana_driver.data.model.InspectionEvidenceImage
import com.example.kilivana_driver.data.model.InspectionFinding
import com.example.kilivana_driver.data.model.InspectionStatus
import com.example.kilivana_driver.data.network.InspectorRepository
import com.example.kilivana_driver.data.network.ProfileRepository
import com.example.kilivana_driver.data.network.SessionStore
import com.example.kilivana_driver.ui.components.KilivanaButton
import com.example.kilivana_driver.ui.components.KilivanaHeader
import com.example.kilivana_driver.ui.components.KilivanaTextField
import com.example.kilivana_driver.ui.theme.KilivanaAmber
import com.example.kilivana_driver.ui.theme.KilivanaAmberTint
import com.example.kilivana_driver.ui.theme.KilivanaBackground
import com.example.kilivana_driver.ui.theme.KilivanaBlue
import com.example.kilivana_driver.ui.theme.KilivanaBlueTint
import com.example.kilivana_driver.ui.theme.KilivanaBorder
import com.example.kilivana_driver.ui.theme.KilivanaError
import com.example.kilivana_driver.ui.theme.KilivanaErrorTint
import com.example.kilivana_driver.ui.theme.KilivanaGreen
import com.example.kilivana_driver.ui.theme.KilivanaGreenCard
import com.example.kilivana_driver.ui.theme.KilivanaGreenDark
import com.example.kilivana_driver.ui.theme.KilivanaGreenTint
import com.example.kilivana_driver.ui.theme.KilivanaTextMuted
import com.example.kilivana_driver.ui.theme.KilivanaTextPrimary
import com.example.kilivana_driver.ui.theme.KilivanaWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val InspectorCanvas = Color(0xFFF5F7F5)
private val InspectorQuestionSurface = Color(0xFFFFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectorShell(
    onLogout: () -> Unit
) {
    val user by SessionStore.currentUser.collectAsState()
    val repository = remember { InspectorRepository() }
    val profileRepository = remember { ProfileRepository() }
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

    // Profile photo state
    var profileImages by remember { mutableStateOf<List<DriverImage>>(emptyList()) }
    var isUploadingProfilePhoto by remember { mutableStateOf(false) }
    var profilePhotoError by remember { mutableStateOf<String?>(null) }
    var profilePhotoSuccess by remember { mutableStateOf<String?>(null) }
    var viewingProfilePhoto by remember { mutableStateOf(false) }
    var showDeletePhotoDialog by remember { mutableStateOf(false) }

    val primaryProfileImageUrl = profileImages.firstOrNull { it.isPrimary == true }?.url
        ?: profileImages.firstOrNull()?.url

    LaunchedEffect(user?.id, SessionStore.userId, selectedTab) {
        val userId = user?.id ?: SessionStore.userId
        if (userId != null && userId > 0) {
            profileRepository.getInspectorImages(userId).fold(
                onSuccess = { images ->
                    profileImages = images
                },
                onFailure = { _ -> }
            )
        }
    }

    val profilePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val userId = user?.id ?: SessionStore.userId ?: return@rememberLauncherForActivityResult
            scope.launch {
                isUploadingProfilePhoto = true
                profilePhotoError = null
                profilePhotoSuccess = null
                val tempFile = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val temp = File.createTempFile("inspector_avatar_", ".jpg", context.cacheDir)
                            temp.outputStream().use { out -> stream.copyTo(out) }
                            temp
                        }
                    }.getOrNull()
                }
                if (tempFile == null) {
                    profilePhotoError = "Could not read selected image."
                    isUploadingProfilePhoto = false
                    return@launch
                }
                val previousPrimaryId = profileImages.firstOrNull { it.isPrimary == true }?.id

                val result = profileRepository.uploadInspectorImage(
                    userId = userId,
                    file = tempFile,
                    isPrimary = true
                )
                tempFile.delete()

                if (result.isSuccess) {
                    val newImages = result.getOrDefault(emptyList())
                    if (previousPrimaryId != null && previousPrimaryId > 0) {
                        profileRepository.deleteInspectorImage(userId, previousPrimaryId)
                    }
                    profileImages = newImages.filterNot { it.id == previousPrimaryId }
                    profilePhotoSuccess = "Profile photo updated successfully."
                } else {
                    profilePhotoError = result.exceptionOrNull()?.message ?: "Could not upload profile photo."
                }
                isUploadingProfilePhoto = false
            }
        }
    }

    fun deleteProfilePhoto() {
        val userId = user?.id ?: SessionStore.userId ?: return
        val primaryId = profileImages.firstOrNull { it.isPrimary == true }?.id ?: profileImages.firstOrNull()?.id ?: return
        scope.launch {
            isUploadingProfilePhoto = true
            profilePhotoError = null
            profilePhotoSuccess = null
            val result = profileRepository.deleteInspectorImage(userId, primaryId)
            if (result.isSuccess) {
                profileImages = profileImages.filterNot { it.id == primaryId }
                profilePhotoSuccess = "Profile photo removed."
            } else {
                profilePhotoError = result.exceptionOrNull()?.message ?: "Could not remove profile photo."
            }
            isUploadingProfilePhoto = false
            showDeletePhotoDialog = false
        }
    }

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
                message = "Inspection findings submitted successfully."
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

    val openCount = assignments.count {
        it.hasStatus(InspectionStatus.ASSIGNED) ||
            it.hasStatus(InspectionStatus.PENDING) ||
            it.hasStatus(InspectionStatus.IN_PROGRESS)
    }
    val completedCount = assignments.count { it.hasStatus(InspectionStatus.COMPLETED) }
    val historyItems = assignments.filter {
        it.hasStatus(InspectionStatus.COMPLETED) ||
            it.hasStatus(InspectionStatus.CANCELLED) ||
            !it.result.isNullOrBlank()
    }
    val sourceItems = if (selectedTab == InspectorTab.HISTORY) historyItems else assignments
    val filteredItems = sourceItems.filter { item ->
        val query = searchQuery.trim()
        val matchesQuery = query.isBlank() || listOfNotNull(
            item.id?.toString(), item.targetType, item.targetId?.toString(), item.status,
            item.result, item.notes, item.targetFarm?.farmName, item.targetFarm?.name, item.targetFarm?.ownerName
        ).any { it.contains(query, ignoreCase = true) }
        val matchesFilter = when (queueFilter) {
            "Assigned" -> item.hasStatus(InspectionStatus.ASSIGNED)
            "Pending" -> item.hasStatus(InspectionStatus.PENDING)
            "In progress" -> item.hasStatus(InspectionStatus.IN_PROGRESS)
            "Completed" -> item.hasStatus(InspectionStatus.COMPLETED)
            "Rejected" -> item.result.equals(InspectionFinding.REJECTED.name, ignoreCase = true)
            else -> true
        }
        matchesQuery && matchesFilter
    }

    if (selected == null) {
        Scaffold(
            containerColor = KilivanaBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                        InspectorDashboardHeader(
                            user = user,
                            profileImageUrl = primaryProfileImageUrl,
                            isUploadingPhoto = isUploadingProfilePhoto,
                            onAvatarClick = { selectedTab = InspectorTab.PROFILE },
                            onLogout = onLogout
                        )
                    }
                    item {
                        InspectorSummary(
                            open = openCount,
                            completed = completedCount,
                            total = assignments.size
                        )
                    }
                    if (error != null) {
                        item {
                            InlineNotice(error.orEmpty(), isError = true)
                        }
                    }
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            SectionHeader(text = "Quick Actions")
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                InspectorQuickActionCard(
                                    title = "Inspection Queue",
                                    subtitle = "$openCount open",
                                    icon = Icons.AutoMirrored.Outlined.Assignment,
                                    onClick = { selectedTab = InspectorTab.INSPECTIONS },
                                    modifier = Modifier.weight(1f)
                                )
                                InspectorQuickActionCard(
                                    title = "History",
                                    subtitle = "$completedCount completed",
                                    icon = Icons.Outlined.History,
                                    onClick = { selectedTab = InspectorTab.HISTORY },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                InspectorQuickActionCard(
                                    title = "Profile",
                                    subtitle = user?.region ?: "Account details",
                                    icon = Icons.Outlined.Person,
                                    onClick = { selectedTab = InspectorTab.PROFILE },
                                    modifier = Modifier.weight(1f)
                                )
                                InspectorQuickActionCard(
                                    title = "Refresh",
                                    subtitle = "Sync assignments",
                                    icon = Icons.Outlined.Refresh,
                                    onClick = { refreshAssignments() },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Recent Inspections",
                                color = KilivanaTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = { refreshAssignments() },
                                    enabled = !isLoading && !isSaving
                                ) {
                                    Text("Refresh", color = KilivanaGreen, fontWeight = FontWeight.SemiBold)
                                }
                                TextButton(onClick = { selectedTab = InspectorTab.INSPECTIONS }) {
                                    Text("View all", color = KilivanaGreen, fontWeight = FontWeight.SemiBold)
                                }
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = KilivanaGreen, strokeWidth = 2.dp)
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }

                InspectorTab.INSPECTIONS, InspectorTab.HISTORY -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(KilivanaBackground)
                        .padding(padding)
                ) {
                    KilivanaHeader(
                        title = if (selectedTab == InspectorTab.HISTORY) "Inspection History" else "Inspection Queue",
                        subtitle = if (selectedTab == InspectorTab.HISTORY) "Completed and past inspection records" else "Assignments requiring verification",
                        onActionClick = { refreshAssignments() }
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        item { Spacer(Modifier.height(4.dp)) }

                        item {
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    placeholder = { Text("Search target, farm, or result") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.Search,
                                            contentDescription = null,
                                            tint = KilivanaTextMuted
                                        )
                                    },
                                    trailingIcon = if (searchQuery.isNotEmpty()) {
                                        {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(
                                                    Icons.Outlined.Close,
                                                    contentDescription = "Clear search",
                                                    tint = KilivanaTextMuted
                                                )
                                            }
                                        }
                                    } else null,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = KilivanaWhite,
                                        unfocusedContainerColor = KilivanaWhite,
                                        focusedBorderColor = KilivanaGreen,
                                        unfocusedBorderColor = KilivanaBorder,
                                        focusedLabelColor = KilivanaGreen,
                                        cursorColor = KilivanaGreen
                                    )
                                )
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val filters = if (selectedTab == InspectorTab.HISTORY) listOf("All", "Completed", "Rejected")
                                else listOf("All", "Assigned", "Pending", "In progress", "Completed", "Rejected")
                                filters.forEach { filter ->
                                    val isSelected = queueFilter == filter
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(if (isSelected) KilivanaGreen else KilivanaGreenTint)
                                            .clickable { queueFilter = filter }
                                            .padding(horizontal = 16.dp, vertical = 9.dp)
                                    ) {
                                        Text(
                                            text = filter,
                                            color = if (isSelected) KilivanaWhite else KilivanaGreen,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        if (isLoading) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(color = KilivanaGreen, strokeWidth = 2.dp)
                                }
                            }
                        }

                        if (filteredItems.isEmpty() && !isLoading && error == null) {
                            item {
                                InspectorEmptyState(
                                    title = if (selectedTab == InspectorTab.HISTORY) "No history records found" else "No matching inspections",
                                    message = if (searchQuery.isNotBlank() || queueFilter != "All") "Try adjusting your search or filter options." else "Assigned inspections will appear here when ready.",
                                    onRefresh = ::refreshAssignments
                                )
                            }
                        }

                        items(filteredItems, key = { it.id ?: it.hashCode().toLong() }) { item ->
                            InspectionCard(item = item, onClick = { openInspection(item) })
                        }

                        if (error != null) {
                            item {
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    InlineNotice(error.orEmpty(), isError = true)
                                }
                            }
                        }

                        if (message != null) {
                            item {
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    InlineNotice(message.orEmpty())
                                }
                            }
                        }
                    }
                }

                InspectorTab.PROFILE -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(KilivanaBackground)
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    KilivanaHeader(
                        title = "Inspector Profile",
                        subtitle = "Account credentials & assigned region"
                    )

                    Spacer(Modifier.height(16.dp))

                    profilePhotoError?.let { err ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            InlineNotice(err, isError = true)
                        }
                    }
                    profilePhotoSuccess?.let { succ ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            InlineNotice(succ)
                        }
                    }

                    InspectorProfileHeaderCard(
                        user = user,
                        profileImageUrl = primaryProfileImageUrl,
                        isUploadingPhoto = isUploadingProfilePhoto,
                        onPickPhoto = {
                            profilePhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onViewPhoto = { viewingProfilePhoto = true },
                        onDeletePhoto = { showDeletePhotoDialog = true }
                    )

                    Spacer(Modifier.height(16.dp))
                    InspectorProfileDetailsCard(user = user)
                    Spacer(Modifier.height(24.dp))

                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        OutlinedButton(
                            onClick = onLogout,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, KilivanaError.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = KilivanaError
                            )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Sign out", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
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

    if (viewingProfilePhoto && !primaryProfileImageUrl.isNullOrBlank()) {
        Dialog(
            onDismissRequest = { viewingProfilePhoto = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable { viewingProfilePhoto = false },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(primaryProfileImageUrl)
                        .diskCachePolicy(CachePolicy.DISABLED)
                        .build(),
                    contentDescription = "Full-size profile photo",
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { viewingProfilePhoto = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(16.dp)
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close photo", tint = Color.White)
                }
            }
        }
    }

    if (showDeletePhotoDialog) {
        AlertDialog(
            onDismissRequest = { showDeletePhotoDialog = false },
            title = { Text("Remove Profile Photo") },
            text = { Text("Are you sure you want to remove your profile photo?") },
            confirmButton = {
                TextButton(
                    onClick = { deleteProfilePhoto() },
                    enabled = !isUploadingProfilePhoto
                ) {
                    Text("Remove", color = KilivanaError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePhotoDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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
    val isSubmitted = item.status.equals("COMPLETED", ignoreCase = true) || item.status.equals("CANCELLED", ignoreCase = true)
    val isReadOnly = isSubmitted || (!isInProgress && !canStart)
    val canSubmit = isInProgress && checklistComplete && selectedFinding != null
    val actionEnabled = !isSaving && !isLoadingDetail && !isReadOnly && (canStart || canSubmit)
    var selectedEvidence by remember(item.id) { mutableStateOf<List<Uri>>(emptyList()) }
    val evidencePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris -> selectedEvidence = uris }
    BackHandler(enabled = true, onBack = onBack)

    Scaffold(
        containerColor = KilivanaBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            KilivanaHeader(
                title = "Inspection #${item.id ?: "—"}",
                subtitle = item.targetType?.replace('_', ' ')?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Subject",
                onBack = onBack
            )
        },
        bottomBar = {
            Surface(
                color = KilivanaWhite,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    KilivanaButton(
                        text = when {
                            canStart -> "Start Inspection"
                            isInProgress -> "Submit Inspection"
                            isSubmitted -> "Inspection Completed (View Only)"
                            else -> "Inspection Unavailable"
                        },
                        onClick = { if (canStart) onStart() else onSubmit(selectedEvidence) },
                        enabled = actionEnabled,
                        isLoading = isSaving || isLoadingDetail
                    )
                    if (isSubmitted) {
                        Text(
                            "This inspection has been submitted and is locked.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            textAlign = TextAlign.Center,
                            color = KilivanaTextMuted,
                            fontSize = 12.sp
                        )
                    } else if (isInProgress && !checklistComplete) {
                        Text(
                            "Complete every checklist item to submit.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            textAlign = TextAlign.Center,
                            color = KilivanaTextMuted,
                            fontSize = 12.sp
                        )
                    } else if (isInProgress && selectedFinding == null) {
                        Text(
                            "Select an inspection result before submitting.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            textAlign = TextAlign.Center,
                            color = KilivanaAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
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
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (isReadOnly) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = KilivanaAmberTint,
                    border = BorderStroke(1.dp, KilivanaAmber.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = KilivanaAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Submitted Inspection — View Only",
                                color = KilivanaTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Form fields are frozen and cannot be changed.",
                                color = KilivanaTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // SUBJECT CARD
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = KilivanaGreenCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(KilivanaGreenDark, KilivanaGreenCard)))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "INSPECTION SUBJECT",
                            color = KilivanaWhite.copy(alpha = 0.72f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        StatusPill(status = item.status)
                    }

                    Text(
                        text = item.targetType?.replace('_', ' ')?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Assigned Subject",
                        color = KilivanaWhite,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "Reference #${item.targetId ?: "—"}",
                        color = KilivanaWhite.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    item.targetFarm?.let { farm ->
                        HorizontalDivider(color = KilivanaWhite.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 2.dp))

                        val farmName = farm.farmName?.takeIf(String::isNotBlank)
                            ?: farm.name?.takeIf(String::isNotBlank)
                        farmName?.let { name ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.Agriculture, contentDescription = null, tint = KilivanaWhite, modifier = Modifier.size(18.dp))
                                Text(name, color = KilivanaWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        farm.ownerName?.takeIf(String::isNotBlank)?.let { owner ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.Person, contentDescription = null, tint = KilivanaWhite.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
                                Text("Owner: $owner", color = KilivanaWhite.copy(alpha = 0.9f), fontSize = 13.sp)
                            }
                        }

                        listOfNotNull(farm.location, farm.subCounty, farm.county)
                            .filter(String::isNotBlank).distinct().joinToString(" · ")
                            .takeIf(String::isNotBlank)?.let { loc ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = KilivanaWhite.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
                                    Text(loc, color = KilivanaWhite.copy(alpha = 0.9f), fontSize = 13.sp)
                                }
                            }

                        farm.ownerPhone?.takeIf(String::isNotBlank)?.let { phone ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.Phone, contentDescription = null, tint = KilivanaWhite.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
                                Text(phone, color = KilivanaWhite.copy(alpha = 0.9f), fontSize = 13.sp)
                            }
                        }
                    }

                    item.inspectedAt?.takeIf { it.isNotBlank() }?.let { date ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = KilivanaWhite.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
                            Text("Inspection Date: $date", color = KilivanaWhite.copy(alpha = 0.88f), fontSize = 12.sp)
                        }
                    }
                }
            }

            // CHECKLIST CARD
            SectionCard(
                title = "Checklist Items",
                subtitle = "Verify each requirement and mark its status.",
                trailing = "${checklistQuestions.count { checklistAnswers[it] != null }} / ${checklistQuestions.size}"
            ) {
                val answeredCount = checklistQuestions.count { checklistAnswers[it] != null }
                val progressFraction = if (checklistQuestions.isEmpty()) 0f else answeredCount.toFloat() / checklistQuestions.size

                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = KilivanaGreen,
                    trackColor = KilivanaGreenTint
                )

                checklistQuestions.forEach { question ->
                    ChecklistQuestionRow(
                        question = question,
                        answer = checklistAnswers[question],
                        enabled = !isSaving && !isReadOnly,
                        onAnswer = { answer -> onAnswer(question, answer) }
                    )
                }

                if (!isReadOnly) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KilivanaTextField(
                            value = customQuestion,
                            onValueChange = onCustomQuestionChange,
                            modifier = Modifier.weight(1f),
                            placeholder = "Add custom check item…",
                            enabled = !isSaving && customQuestionCount < 8
                        )
                        Button(
                            onClick = onAddCustomQuestion,
                            enabled = !isSaving && customQuestion.isNotBlank() && customQuestionCount < 8,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = KilivanaGreen)
                        ) {
                            Text("Add", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // INSPECTION RESULT CARD
            SectionCard(
                title = "Inspection Result",
                subtitle = if (isReadOnly) "Submitted inspection outcome." else "Select the outcome supported by your observations."
            ) {
                InspectionFinding.entries.forEach { finding ->
                    val isSelected = selectedFinding == finding
                    val (tintColor, borderColor, icon) = when (finding) {
                        InspectionFinding.APPROVED -> Triple(KilivanaGreenTint, KilivanaGreen, Icons.Outlined.CheckCircle)
                        InspectionFinding.REJECTED -> Triple(KilivanaErrorTint, KilivanaError, Icons.Outlined.Warning)
                        InspectionFinding.CHANGES_REQUIRED -> Triple(KilivanaAmberTint, KilivanaAmber, Icons.Outlined.Info)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = !isSaving && !isReadOnly) { onFindingSelected(finding) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) tintColor else KilivanaWhite
                        ),
                        border = BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) borderColor else KilivanaBorder.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) borderColor else InspectorCanvas),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = KilivanaWhite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                finding.label,
                                color = KilivanaTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // OBSERVATIONS CARD
            SectionCard(
                title = "Observations & Notes",
                subtitle = if (isReadOnly) "Submitted inspection notes." else "Record any discrepancies, conditions, or recommendations."
            ) {
                OutlinedTextField(
                    value = findingNotes,
                    onValueChange = onNotesChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Write detailed observations here…") },
                    minLines = 4,
                    maxLines = 7,
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isSaving && !isReadOnly,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = KilivanaWhite,
                        unfocusedContainerColor = KilivanaWhite,
                        disabledContainerColor = KilivanaBackground,
                        disabledTextColor = KilivanaTextPrimary,
                        disabledBorderColor = KilivanaBorder,
                        focusedBorderColor = KilivanaGreen,
                        unfocusedBorderColor = KilivanaBorder,
                        focusedLabelColor = KilivanaGreen,
                        cursorColor = KilivanaGreen
                    )
                )
            }

            // EVIDENCE PHOTOS CARD
            SectionCard(
                title = "Evidence Photos",
                subtitle = if (isReadOnly) "Attached evidence photos for this inspection." else "Attach clear photo evidence to validate inspection results."
            ) {
                if (!isReadOnly) {
                    OutlinedButton(
                        onClick = {
                            evidencePicker.launch(
                                androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, KilivanaGreen),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = KilivanaGreen)
                    ) {
                        Icon(Icons.Outlined.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (selectedEvidence.isEmpty()) "Choose Evidence Photos" else "Change Selected Photos",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (selectedEvidence.isNotEmpty() && !isReadOnly) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        selectedEvidence.forEachIndexed { index, uri ->
                            Box(
                                modifier = Modifier
                                    .size(84.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, KilivanaBorder, RoundedCornerShape(12.dp))
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
                                        .background(KilivanaTextPrimary.copy(alpha = 0.8f))
                                        .clickable { selectedEvidence = selectedEvidence.filterNot { it == uri } },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Close,
                                        contentDescription = "Remove photo",
                                        tint = KilivanaWhite,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        "${selectedEvidence.size} photo${if (selectedEvidence.size == 1) "" else "s"} queued for upload.",
                        color = KilivanaAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (isLoadingDetail) {
                    InlineNotice("Loading uploaded evidence photos…", isProgress = true)
                } else if (evidenceImages.isNotEmpty()) {
                    Text(
                        "Uploaded Evidence · ${evidenceImages.size}",
                        color = KilivanaTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        evidenceImages.forEachIndexed { index, image ->
                            Box(
                                modifier = Modifier
                                    .size(84.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, KilivanaBorder, RoundedCornerShape(12.dp))
                            ) {
                                AsyncImage(
                                    model = image.url,
                                    contentDescription = "Uploaded evidence photo ${index + 1}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                if (!isReadOnly) {
                                    IconButton(
                                        onClick = { onDeleteEvidence(image) },
                                        enabled = !isSaving,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(2.dp)
                                            .size(28.dp)
                                            .background(KilivanaTextPrimary.copy(alpha = 0.8f), CircleShape)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Close,
                                            contentDescription = "Delete photo",
                                            tint = KilivanaWhite,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (item.evidenceUrls.isNullOrBlank()) {
                    Text("No uploaded evidence photos yet.", color = KilivanaTextMuted, fontSize = 12.sp)
                }
            }

            if (isLoadingDetail) InlineNotice("Loading latest inspection details…", isProgress = true)
            error?.let { InlineNotice(it, isError = true) }
            message?.let { InlineNotice(it) }
            Spacer(Modifier.height(16.dp))
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
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = KilivanaWhite),
        border = BorderStroke(1.dp, KilivanaBorder.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, color = KilivanaTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = KilivanaTextMuted, fontSize = 12.sp)
                }
                trailing?.let {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(KilivanaGreenTint)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(it, color = KilivanaGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
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
            .clip(RoundedCornerShape(12.dp))
            .background(if (isError) KilivanaErrorTint else KilivanaGreenTint)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (isProgress) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = KilivanaGreen)
        } else if (isError) {
            Icon(Icons.Outlined.Warning, contentDescription = null, tint = KilivanaError, modifier = Modifier.size(18.dp))
        } else {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = KilivanaGreen, modifier = Modifier.size(18.dp))
        }
        Text(
            text,
            color = if (isError) KilivanaError else KilivanaGreen,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ChecklistQuestionRow(
    question: String,
    answer: String?,
    enabled: Boolean,
    onAnswer: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = InspectorQuestionSurface),
        border = BorderStroke(1.dp, KilivanaBorder.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(question, color = KilivanaTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("PASS" to "Pass", "ISSUE" to "Issue", "N/A" to "N/A").forEach { (optionKey, optionLabel) ->
                    val isSelected = answer == optionKey
                    val optionColor = when (optionKey) {
                        "PASS" -> KilivanaGreen
                        "ISSUE" -> KilivanaError
                        else -> KilivanaTextMuted
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) optionColor else KilivanaBackground)
                            .border(
                                1.dp,
                                if (isSelected) optionColor else KilivanaBorder.copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable(enabled = enabled) { onAnswer(optionKey) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = optionLabel,
                            color = if (isSelected) KilivanaWhite else KilivanaTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
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
private fun InspectorDashboardHeader(
    user: AuthUser?,
    profileImageUrl: String?,
    isUploadingPhoto: Boolean,
    onAvatarClick: () -> Unit,
    onLogout: () -> Unit
) {
    val displayName = user?.name?.takeIf { it.isNotBlank() } ?: "Inspector"
    val firstName = displayName.trim().substringBefore(' ')
    val initials = displayName.trim().split(Regex("\\s+")).take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
    val context = LocalContext.current
    var imageError by remember(profileImageUrl) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(KilivanaGreenCard)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 64.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(KilivanaWhite.copy(alpha = 0.18f))
                    .border(2.dp, KilivanaWhite.copy(alpha = 0.65f), CircleShape)
                    .clickable(onClick = onAvatarClick),
                contentAlignment = Alignment.Center
            ) {
                if (isUploadingPhoto) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = KilivanaWhite, strokeWidth = 2.dp)
                } else if (!profileImageUrl.isNullOrBlank() && !imageError) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(profileImageUrl)
                            .diskCachePolicy(CachePolicy.DISABLED)
                            .build(),
                        contentDescription = "Inspector profile photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = ColorPainter(Color.Transparent),
                        onError = { imageError = true }
                    )
                } else {
                    Text(initials.ifBlank { "I" }, color = KilivanaWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text("INSPECTOR PORTAL 📋", color = KilivanaWhite.copy(alpha = 0.82f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Hello, $firstName",
                    color = KilivanaWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                onClick = onLogout,
                shape = RoundedCornerShape(20.dp),
                color = KilivanaWhite.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, KilivanaWhite.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = "Sign out",
                        tint = KilivanaWhite,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Sign out",
                        color = KilivanaWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        user?.referenceCode?.takeIf { it.isNotBlank() }?.let { referenceCode ->
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(KilivanaWhite.copy(alpha = 0.18f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "INSPECTOR ID · $referenceCode",
                    color = KilivanaWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
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
        "COMPLETED" -> KilivanaGreen
        "IN_PROGRESS" -> KilivanaBlue
        "ASSIGNED", "PENDING" -> KilivanaAmber
        "CANCELLED" -> KilivanaTextMuted
        else -> KilivanaGreen
    }
    Text(
        text = normalized,
        color = tone,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(tone.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

@Composable
private fun InspectorEmptyState(
    title: String = "You're all caught up!",
    message: String = "New inspections assigned to you will appear here.",
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = KilivanaWhite),
        border = BorderStroke(1.dp, KilivanaBorder.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(KilivanaGreenTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Outlined.Assignment, contentDescription = null, tint = KilivanaGreen, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(title, color = KilivanaTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                message,
                color = KilivanaTextMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onRefresh) { Text("Check again", color = KilivanaGreen, fontWeight = FontWeight.Bold) }
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
        ?: "Pending verification"

    val isCompleted = item.hasStatus(InspectionStatus.COMPLETED)
    val isInProgress = item.hasStatus(InspectionStatus.IN_PROGRESS)
    val isRejected = item.result.equals(InspectionFinding.REJECTED.name, ignoreCase = true)

    val (badgeBg, badgeIcon) = when {
        isCompleted && !isRejected -> KilivanaGreen to Icons.Filled.Check
        isRejected -> KilivanaError to Icons.Outlined.Warning
        isInProgress -> KilivanaBlue to Icons.Outlined.HourglassTop
        else -> KilivanaAmber to Icons.AutoMirrored.Outlined.Assignment
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = KilivanaWhite),
        border = BorderStroke(1.dp, KilivanaBorder.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(badgeBg.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(badgeIcon, contentDescription = null, tint = badgeBg, modifier = Modifier.size(22.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.targetType?.replace('_', ' ')?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Inspection",
                        color = KilivanaTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(KilivanaBackground)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("#${item.targetId ?: item.id ?: "—"}", color = KilivanaTextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                item.targetFarm?.farmName?.takeIf { it.isNotBlank() }?.let { farm ->
                    Text("Farm: $farm", color = KilivanaTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }

                Text(
                    text = finding,
                    color = if (isRejected) KilivanaError else KilivanaTextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = KilivanaTextMuted)
        }
    }
}

@Composable
private fun InspectorSummary(
    open: Int,
    completed: Int,
    total: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .pullUp(44.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = KilivanaWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            border = BorderStroke(1.dp, KilivanaBorder.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "INSPECTION OVERVIEW",
                        color = KilivanaGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "$total Total Assigned",
                        color = KilivanaTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InspectorMetricCard("Open Queue", open, KilivanaAmber, KilivanaAmberTint, Modifier.weight(1f))
                    InspectorMetricCard("Completed", completed, KilivanaGreen, KilivanaGreenTint, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun InspectorMetricCard(
    label: String,
    count: Int,
    accent: Color,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(tint)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(count.toString(), color = accent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            label,
            color = KilivanaTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        color = KilivanaTextPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun InspectorQuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = KilivanaWhite),
        border = BorderStroke(1.dp, KilivanaBorder.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(KilivanaGreenTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = KilivanaGreen, modifier = Modifier.size(20.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = KilivanaTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = KilivanaTextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private fun InspectionDto.hasStatus(expected: InspectionStatus): Boolean =
    status.equals(expected.name, ignoreCase = true)

@Composable
private fun InspectorProfileHeaderCard(
    user: AuthUser?,
    profileImageUrl: String?,
    isUploadingPhoto: Boolean,
    onPickPhoto: () -> Unit,
    onViewPhoto: () -> Unit,
    onDeletePhoto: () -> Unit
) {
    val displayName = user?.name?.takeIf { it.isNotBlank() } ?: "Inspector"
    val initials = displayName.trim().split(Regex("\\s+")).take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
    val context = LocalContext.current
    var imageError by remember(profileImageUrl) { mutableStateOf(false) }
    val hasPhoto = !profileImageUrl.isNullOrBlank() && !imageError

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = KilivanaWhite),
        border = BorderStroke(1.dp, KilivanaBorder.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(KilivanaGreenCard)
                        .clickable { if (hasPhoto) onViewPhoto() else onPickPhoto() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploadingPhoto) {
                        CircularProgressIndicator(modifier = Modifier.size(26.dp), color = KilivanaWhite, strokeWidth = 2.dp)
                    } else if (hasPhoto) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(profileImageUrl)
                                .diskCachePolicy(CachePolicy.DISABLED)
                                .build(),
                            contentDescription = "Inspector profile photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = ColorPainter(Color.Transparent),
                            onError = { imageError = true }
                        )
                    } else {
                        Text(initials.ifBlank { "I" }, color = KilivanaWhite, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(KilivanaGreen)
                            .border(1.5.dp, KilivanaWhite, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoCamera,
                            contentDescription = "Upload photo",
                            tint = KilivanaWhite,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(displayName, color = KilivanaTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(user?.email ?: "Inspector Account", color = KilivanaTextMuted, fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(KilivanaGreenTint)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("ACTIVE INSPECTOR", color = KilivanaGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(color = KilivanaBorder.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onPickPhoto,
                    enabled = !isUploadingPhoto,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, KilivanaGreen),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = KilivanaGreen)
                ) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (hasPhoto) "Change Photo" else "Upload Photo", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                if (hasPhoto) {
                    OutlinedButton(
                        onClick = onDeletePhoto,
                        enabled = !isUploadingPhoto,
                        modifier = Modifier.height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, KilivanaError.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = KilivanaError)
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete photo", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun InspectorProfileDetailsCard(user: AuthUser?) {
    val profileDetails = listOf(
        Triple("Full Name", user?.name?.takeIf { it.isNotBlank() } ?: "Inspector", Icons.Outlined.Person),
        Triple("Inspector ID", user?.referenceCode?.takeIf { it.isNotBlank() } ?: user?.id?.toString().orEmpty(), Icons.Outlined.Badge),
        Triple("Email Address", user?.email?.takeIf { it.isNotBlank() } ?: "Not provided", Icons.Outlined.Email),
        Triple("Phone Number", user?.phone?.takeIf { it.isNotBlank() } ?: "Not provided", Icons.Outlined.Phone),
        Triple("Assigned Region", user?.region?.takeIf { it.isNotBlank() } ?: "Not provided", Icons.Outlined.LocationOn)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = KilivanaWhite),
        border = BorderStroke(1.dp, KilivanaBorder.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp)
        ) {
            profileDetails.forEachIndexed { index, (label, value, icon) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(KilivanaGreenTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = KilivanaGreen, modifier = Modifier.size(18.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(label.uppercase(), color = KilivanaTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(value, color = KilivanaTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (index < profileDetails.lastIndex) {
                    HorizontalDivider(color = KilivanaBorder.copy(alpha = 0.4f))
                }
            }
        }
    }
}

private enum class InspectorTab(val label: String, val icon: ImageVector) {
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
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(KilivanaBorder.copy(alpha = 0.6f))
        )
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
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = KilivanaGreen,
                        selectedTextColor = KilivanaGreen,
                        unselectedIconColor = KilivanaTextMuted,
                        unselectedTextColor = KilivanaTextMuted,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    }
}
