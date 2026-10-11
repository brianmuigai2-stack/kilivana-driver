package com.example.kilivana_driver.ui.screens.jobs

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kilivana_driver.data.model.Job
import com.example.kilivana_driver.data.model.TripStage
import com.example.kilivana_driver.data.routing.RouteRepository
import com.example.kilivana_driver.ui.components.KilivanaButton
import com.example.kilivana_driver.ui.screens.map.CarMarker
import com.example.kilivana_driver.ui.screens.map.HeadingTracker
import com.example.kilivana_driver.ui.screens.map.rememberUserLocation
import com.example.kilivana_driver.ui.theme.KilivanaAmber
import com.example.kilivana_driver.ui.theme.KilivanaAmberTint
import com.example.kilivana_driver.ui.theme.KilivanaGreen
import com.example.kilivana_driver.ui.theme.KilivanaGreenCard
import com.example.kilivana_driver.ui.theme.KilivanaNotificationRed
import com.example.kilivana_driver.ui.theme.KilivanaTextMuted
import com.example.kilivana_driver.ui.theme.KilivanaTextPrimary
import com.example.kilivana_driver.ui.theme.KilivanaTheme
import com.example.kilivana_driver.ui.theme.KilivanaWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

private enum class UiTripStage { HEADING_TO_PICKUP, IN_TRANSIT, DELIVERED }

/*
 * OpenStreetMap requires a meaningful User-Agent. Keep this identical to
 * the one used in MapScreen.kt so both screens comply with OSM's tile
 * usage policy the same way.
 */
private const val OSM_USER_AGENT =
    "KilivanaDriver/1.0 (Kilivana agricultural logistics app)"

private const val DRIVER_BLUE = 0xFF3B82F6.toInt()
private const val FARMER_GREEN = 0xFF2E7D32.toInt()
private const val BUYER_RED = 0xFFE53935.toInt()

@Composable
fun JobRouteScreen(
    job: Job,
    onBack: () -> Unit,
    onDeliveryComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tripViewModel: TripViewModel = viewModel()

    // Initialize the ViewModel with the job
    LaunchedEffect(job.id) {
        tripViewModel.initialize(job)
    }

    // Collect state from ViewModel
    val tripStage by tripViewModel.tripStage.collectAsStateWithLifecycle()
    val isBusy by tripViewModel.isBusy.collectAsStateWithLifecycle()
    val lastError by tripViewModel.lastError.collectAsStateWithLifecycle()
    val routePoints by tripViewModel.routePoints.collectAsStateWithLifecycle()
    val usingEstimatedRoute by tripViewModel.usingEstimatedRoute.collectAsStateWithLifecycle()
    val distanceLabel by tripViewModel.distanceLabel.collectAsStateWithLifecycle()
    val queueSize by tripViewModel.queueSize.collectAsStateWithLifecycle()

    // Convert backend TripStage to UI stage
    val uiStage = remember { mutableStateOf(UiTripStage.HEADING_TO_PICKUP) }
    var hasFramedRoute by remember { mutableStateOf(false) }
    LaunchedEffect(tripStage) {
        uiStage.value = when (tripStage) {
            com.example.kilivana_driver.data.model.TripStage.HEADING_TO_PICKUP -> UiTripStage.HEADING_TO_PICKUP
            com.example.kilivana_driver.data.model.TripStage.IN_TRANSIT -> UiTripStage.IN_TRANSIT
            com.example.kilivana_driver.data.model.TripStage.DELIVERED -> UiTripStage.DELIVERED
            com.example.kilivana_driver.data.model.TripStage.CANCELLED -> UiTripStage.DELIVERED
            else -> UiTripStage.HEADING_TO_PICKUP
        }
        // Reset framing when stage changes so it re-frames for the new route
        hasFramedRoute = false
    }

    // GPS location stream
    val location by rememberUserLocation(enabled = true)

    // Fixed points
    val pickupPoint = remember { GeoPoint(job.pickupLat, job.pickupLon) }
    val dropoffPoint = remember { GeoPoint(job.dropoffLat, job.dropoffLon) }
    val targetPoint = when (uiStage.value) {
        UiTripStage.HEADING_TO_PICKUP -> pickupPoint
        UiTripStage.IN_TRANSIT -> dropoffPoint
        UiTripStage.DELIVERED -> dropoffPoint
    }

    // Map setup
    val mapView = remember {
        Configuration.getInstance().apply {
            load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            userAgentValue = OSM_USER_AGENT
        }

        val tileSource = XYTileSource(
            "MAPNIK",
            0,
            19,
            256,
            ".png",
            arrayOf("https://tile.openstreetmap.org/"),
            OSM_USER_AGENT
        )

        MapView(context).apply {
            setTileSource(tileSource)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(13.0)
            controller.setCenter(pickupPoint)
        }
    }

    // Markers
    val pickupMarker = remember {
        Marker(mapView).apply {
            position = pickupPoint
            icon = labeledMarkerDrawable(context, "Farmer", FARMER_GREEN)
            setAnchor(0.5f, pinAnchorFraction(context))
            title = job.pickupPlace
        }
    }
    val dropoffMarker = remember {
        Marker(mapView).apply {
            position = dropoffPoint
            icon = labeledMarkerDrawable(context, "Buyer", BUYER_RED)
            setAnchor(0.5f, pinAnchorFraction(context))
            title = job.dropoffPlace
        }
    }

    // Car marker for smooth animation
    val carMarker = remember { CarMarker(mapView, context) }

    // Route lines
    val pickupLine = remember { Polyline().apply { outlinePaint.strokeWidth = 9f } }
    val dropoffLine = remember { Polyline().apply { outlinePaint.strokeWidth = 9f } }

    // Heading tracker for vehicle rotation
    val headingTracker = remember { HeadingTracker() }

    // Preview route (farmer -> buyer) fetched once
    var previewRouteToBuyer by remember { mutableStateOf<List<GeoPoint>?>(null) }
    LaunchedEffect(job.id) {
        val result = withContext(Dispatchers.IO) {
            RouteRepository.fetchRoute(pickupPoint, dropoffPoint)
        }
        previewRouteToBuyer = result.points
        if (!result.isRealRoute) tripViewModel._usingEstimatedRoute.value = true
    }

    // Forward GPS updates to ViewModel
    LaunchedEffect(location) {
        location?.let { tripViewModel.onLocationUpdate(it) }
    }

    // Update car marker position and heading
    LaunchedEffect(location, tripViewModel) {
        val fix = location ?: return@LaunchedEffect
        val vehiclePos = GeoPoint(fix.latitude, fix.longitude)
        val heading = headingTracker.update(fix, targetPoint)
        tripViewModel.setCurrentHeading(heading)
        carMarker.update(vehiclePos, heading)
    }

    // Draw overlays
    val displayedDropoffRoute = if (uiStage.value == UiTripStage.IN_TRANSIT) routePoints else previewRouteToBuyer

    LaunchedEffect(routePoints, displayedDropoffRoute, uiStage.value, location) {
        mapView.overlays.clear()

        val activeAlpha = 255
        val dimAlpha = 90

        // Pickup route (green)
        if (uiStage.value == UiTripStage.HEADING_TO_PICKUP) {
            routePoints?.let { points ->
                pickupLine.setPoints(points)
                pickupLine.outlinePaint.color = KilivanaGreen.toArgb()
                pickupLine.outlinePaint.alpha = activeAlpha
                mapView.overlays.add(pickupLine)
            }
        } else if (uiStage.value == UiTripStage.IN_TRANSIT) {
            routePoints?.let { points ->
                pickupLine.setPoints(points)
                pickupLine.outlinePaint.color = KilivanaGreen.toArgb()
                pickupLine.outlinePaint.alpha = dimAlpha
                mapView.overlays.add(pickupLine)
            }
        }

        // Dropoff route (red)
        displayedDropoffRoute?.let { points ->
            dropoffLine.setPoints(points)
            dropoffLine.outlinePaint.color = KilivanaNotificationRed.toArgb()
            dropoffLine.outlinePaint.alpha = if (uiStage.value == UiTripStage.IN_TRANSIT) activeAlpha else dimAlpha
            mapView.overlays.add(dropoffLine)
        }

        mapView.overlays.add(pickupMarker)
        mapView.overlays.add(dropoffMarker)

        // Frame the route for the current stage only
        val shouldFrame = when (uiStage.value) {
            UiTripStage.HEADING_TO_PICKUP -> location != null && routePoints != null
            UiTripStage.IN_TRANSIT -> location != null && routePoints != null
            else -> false
        }
        if (!hasFramedRoute && shouldFrame) {
            val fix = location!!
            val targetPoint = when (uiStage.value) {
                UiTripStage.HEADING_TO_PICKUP -> pickupPoint
                UiTripStage.IN_TRANSIT -> dropoffPoint
                else -> pickupPoint
            }
            val box = BoundingBox.fromGeoPoints(
                listOf(GeoPoint(fix.latitude, fix.longitude), targetPoint)
            )
            mapView.zoomToBoundingBox(box, true, 140)
            hasFramedRoute = true
        }

        mapView.invalidate()
    }

    var showOtpDialog by remember { mutableStateOf(false) }
    var otpError by remember { mutableStateOf<String?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        // Top bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(2.dp, CircleShape)
                        .clip(CircleShape)
                        .background(KilivanaWhite)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = KilivanaTextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .shadow(2.dp, RoundedCornerShape(50))
                        .clip(RoundedCornerShape(50))
                        .background(KilivanaWhite)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "En Route",
                        modifier = Modifier.weight(1f),
                        color = KilivanaTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = distanceLabel, color = KilivanaTextMuted, fontSize = 13.sp)
                }
            }

            if (usingEstimatedRoute) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .align(Alignment.Start)
                        .shadow(2.dp, RoundedCornerShape(50))
                        .clip(RoundedCornerShape(50))
                        .background(KilivanaAmberTint)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = KilivanaAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Showing an estimated route (live roads unavailable)",
                        color = KilivanaAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Offline queue indicator
            if (queueSize > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .align(Alignment.Start)
                        .shadow(2.dp, RoundedCornerShape(50))
                        .clip(RoundedCornerShape(50))
                        .background(KilivanaAmberTint)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = KilivanaAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Queued $queueSize location update(s) — will send when online",
                        color = KilivanaAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (lastError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .align(Alignment.Start)
                        .shadow(2.dp, RoundedCornerShape(50))
                        .clip(RoundedCornerShape(50))
                        .background(KilivanaNotificationRed.copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = KilivanaNotificationRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = lastError!!,
                        color = KilivanaNotificationRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Bottom status card
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(KilivanaWhite)
                .navigationBarsPadding()
                .padding(20.dp)
        ) {
            Text(
                text = job.id,
                color = KilivanaTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                TripStep(
                    label = "Picked Up",
                    done = uiStage.value != UiTripStage.HEADING_TO_PICKUP,
                    current = false,
                    modifier = Modifier.weight(1f)
                )
                TripStep(
                    label = "In Transit",
                    done = uiStage.value == UiTripStage.DELIVERED,
                    current = uiStage.value == UiTripStage.IN_TRANSIT,
                    modifier = Modifier.weight(1f)
                )
                TripStep(
                    label = "Delivered",
                    done = false,
                    current = uiStage.value == UiTripStage.DELIVERED,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            when (uiStage.value) {
                UiTripStage.HEADING_TO_PICKUP -> KilivanaButton(
                    text = "Mark as Picked Up",
                    onClick = {
                        tripViewModel.markPickedUp()
                    },
                    enabled = !isBusy
                )
                UiTripStage.IN_TRANSIT -> KilivanaButton(
                    text = "Confirm Delivery (OTP)",
                    onClick = {
                        otpError = null
                        showOtpDialog = true
                    },
                    enabled = !isBusy
                )
                UiTripStage.DELIVERED -> Unit
            }
        }
    }

    if (showOtpDialog) {
        OtpDialog(
            jobId = job.id,
            error = otpError,
            onConfirm = { entered ->
                if (entered == job.deliveryOtp) {
                    showOtpDialog = false
                    otpError = null
                    tripViewModel.completeDelivery()
                    onDeliveryComplete()
                } else {
                    otpError = "That code doesn't match. Ask the buyer to double-check it."
                }
            },
            onDismiss = {
                showOtpDialog = false
                otpError = null
            }
        )
    }
}

@Composable
private fun OtpDialog(
    jobId: String,
    error: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm delivery") },
        text = {
            Column {
                Text(
                    text = "Ask the buyer for their delivery code and enter it to confirm $jobId was delivered.",
                    color = KilivanaTextMuted,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) code = it },
                    label = { Text("Delivery code") },
                    singleLine = true,
                    isError = error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = error, color = KilivanaNotificationRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(code) }) {
                Text("Confirm", color = KilivanaGreen, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = KilivanaTextMuted) }
        }
    )
}

@Composable
private fun TripStep(
    label: String,
    done: Boolean,
    current: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(
                    when {
                        done -> KilivanaGreen
                        current -> KilivanaGreenCard
                        else -> KilivanaTextMuted.copy(alpha = 0.25f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = KilivanaWhite,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = if (done || current) KilivanaTextPrimary else KilivanaTextMuted,
            fontSize = 12.sp,
            fontWeight = if (current) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun Color.toArgb(): Int {
    return android.graphics.Color.argb(
        (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()
    )
}

/**
 * A small color-coded label bubble ("You" / "Farmer" / "Buyer") sitting
 * above a pin dot, drawn as a single bitmap so it can be used as a marker
 * icon. The pin's exact point (not the bubble) marks the real location.
 */
private fun labeledMarkerDrawable(context: Context, label: String, markerColor: Int): Drawable {
    val density = context.resources.displayMetrics.density
    val pinRadius = 9f * density
    val pinStroke = 3f * density
    val gap = 4f * density
    val bubblePaddingH = 10f * density
    val bubblePaddingV = 5f * density
    val textSize = 12f * density

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        this.textSize = textSize
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    val textWidth = textPaint.measureText(label)
    val bubbleWidth = textWidth + bubblePaddingH * 2
    val bubbleHeight = textSize + bubblePaddingV * 2

    val totalWidth = maxOf(bubbleWidth, pinRadius * 2 + pinStroke * 2).toInt() + 4
    val totalHeight = (bubbleHeight + gap + pinRadius * 2 + pinStroke).toInt() + 2

    val bitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val centerX = totalWidth / 2f

    val bubbleRect = RectF(
        centerX - bubbleWidth / 2f,
        0f,
        centerX + bubbleWidth / 2f,
        bubbleHeight
    )
    val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = markerColor }
    canvas.drawRoundRect(bubbleRect, bubbleHeight / 2f, bubbleHeight / 2f, bubblePaint)
    canvas.drawText(label, centerX, bubbleHeight / 2f - (textPaint.ascent() + textPaint.descent()) / 2f, textPaint)

    val pinCenterY = bubbleHeight + gap + pinRadius
    val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = markerColor }
    canvas.drawCircle(centerX, pinCenterY, pinRadius, pinPaint)
    val pinStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = pinStroke
    }
    canvas.drawCircle(centerX, pinCenterY, pinRadius, pinStrokePaint)

    return BitmapDrawable(context.resources, bitmap)
}

/** Vertical anchor (0..1) that lands exactly on the pin's center within [labeledMarkerDrawable]. */
private fun pinAnchorFraction(context: Context): Float {
    val density = context.resources.displayMetrics.density
    val pinRadius = 9f * density
    val pinStroke = 3f * density
    val gap = 4f * density
    val bubblePaddingV = 5f * density
    val textSize = 12f * density
    val bubbleHeight = textSize + bubblePaddingV * 2
    val totalHeight = bubbleHeight + gap + pinRadius * 2 + pinStroke + 2
    val pinCenterY = bubbleHeight + gap + pinRadius
    return pinCenterY / totalHeight
}

@Preview(showSystemUi = true)
@Composable
private fun JobRouteScreenPreview() {
    KilivanaTheme {
        JobRouteScreen(job = sampleJobs().first(), onBack = {}, onDeliveryComplete = {})
    }
}