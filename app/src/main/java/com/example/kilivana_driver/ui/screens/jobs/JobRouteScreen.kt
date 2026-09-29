package com.example.kilivana_driver.ui.screens.jobs

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
import com.example.kilivana_driver.data.model.Job
import com.example.kilivana_driver.data.routing.RouteRepository
import com.example.kilivana_driver.ui.components.KilivanaButton
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
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

private enum class TripStage { HEADING_TO_PICKUP, IN_TRANSIT, DELIVERED }

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

// How far the driver has to move, and how long we wait between calls,
// before we ask OSRM for a fresh route. Keeps the line hugging the real
// road as the driver moves, without hammering the free routing server.
private const val REROUTE_DISTANCE_METERS = 300.0
private const val REROUTE_MIN_INTERVAL_MS = 15_000L

@Composable
fun JobRouteScreen(
    job: Job,
    onBack: () -> Unit,
    onDeliveryComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var stage by remember { mutableStateOf(TripStage.HEADING_TO_PICKUP) }
    var showOtpDialog by remember { mutableStateOf(false) }
    var otpError by remember { mutableStateOf<String?>(null) }

    val pickupPoint = remember { GeoPoint(job.pickupLat, job.pickupLon) }
    val dropoffPoint = remember { GeoPoint(job.dropoffLat, job.dropoffLon) }

    val location by rememberUserLocation(enabled = true)

    // Live route: driver's real position -> farmer (recalculated on the move)
    var routeToPickup by remember { mutableStateOf<List<GeoPoint>?>(null) }
    var lastPickupOrigin by remember { mutableStateOf<GeoPoint?>(null) }
    var lastPickupFetchTime by remember { mutableStateOf(0L) }

    // One-time overview: farmer -> buyer (shown dimmed for context before pickup)
    var previewRouteToBuyer by remember { mutableStateOf<List<GeoPoint>?>(null) }

    // Live route: driver's real position -> buyer (recalculated on the move,
    // only kicks in once the cargo has actually been picked up)
    var liveRouteToBuyer by remember { mutableStateOf<List<GeoPoint>?>(null) }
    var lastBuyerOrigin by remember { mutableStateOf<GeoPoint?>(null) }
    var lastBuyerFetchTime by remember { mutableStateOf(0L) }

    var usingEstimatedRoute by remember { mutableStateOf(false) }
    var hasFramedRoute by remember { mutableStateOf(false) }

    val mapView = remember {
        Configuration.getInstance().apply {
            load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            userAgentValue = OSM_USER_AGENT
        }

        // Same custom tile source as MapScreen.kt, so requests carry our
        // User-Agent instead of the default one OSM's servers reject.
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
    val userMarker = remember {
        Marker(mapView).apply {
            icon = labeledMarkerDrawable(context, "You", DRIVER_BLUE)
            setAnchor(0.5f, pinAnchorFraction(context))
        }
    }
    val pickupLine = remember { Polyline().apply { outlinePaint.strokeWidth = 9f } }
    val dropoffLine = remember { Polyline().apply { outlinePaint.strokeWidth = 9f } }

    // Preview overview route (farmer -> buyer), fetched once
    LaunchedEffect(job.id) {
        val result = withContext(Dispatchers.IO) {
            RouteRepository.fetchRoute(pickupPoint, dropoffPoint)
        }
        previewRouteToBuyer = result.points
        if (!result.isRealRoute) usingEstimatedRoute = true
    }

    // Live route to the farmer: refetched whenever the driver has moved far
    // enough, while they're still heading to pickup
    LaunchedEffect(location, stage, job.id) {
        val fix = location ?: return@LaunchedEffect
        if (stage != TripStage.HEADING_TO_PICKUP) return@LaunchedEffect

        val current = GeoPoint(fix.latitude, fix.longitude)
        val origin = lastPickupOrigin
        val now = System.currentTimeMillis()
        val movedFar = origin == null || current.distanceToAsDouble(origin) > REROUTE_DISTANCE_METERS
        val dueForRefresh = origin == null || now - lastPickupFetchTime > REROUTE_MIN_INTERVAL_MS

        if (movedFar && dueForRefresh) {
            lastPickupOrigin = current
            lastPickupFetchTime = now
            val result = withContext(Dispatchers.IO) { RouteRepository.fetchRoute(current, pickupPoint) }
            routeToPickup = result.points
            if (!result.isRealRoute) usingEstimatedRoute = true
        }
    }

    // Live route to the buyer: refetched whenever the driver has moved far
    // enough, while the cargo is in transit
    LaunchedEffect(location, stage, job.id) {
        val fix = location ?: return@LaunchedEffect
        if (stage != TripStage.IN_TRANSIT) return@LaunchedEffect

        val current = GeoPoint(fix.latitude, fix.longitude)
        val origin = lastBuyerOrigin
        val now = System.currentTimeMillis()
        val movedFar = origin == null || current.distanceToAsDouble(origin) > REROUTE_DISTANCE_METERS
        val dueForRefresh = origin == null || now - lastBuyerFetchTime > REROUTE_MIN_INTERVAL_MS

        if (movedFar && dueForRefresh) {
            lastBuyerOrigin = current
            lastBuyerFetchTime = now
            val result = withContext(Dispatchers.IO) { RouteRepository.fetchRoute(current, dropoffPoint) }
            liveRouteToBuyer = result.points
            if (!result.isRealRoute) usingEstimatedRoute = true
        }
    }

    val displayedDropoffRoute = liveRouteToBuyer ?: previewRouteToBuyer

    // Draw / update overlays whenever anything relevant changes
    LaunchedEffect(location, routeToPickup, displayedDropoffRoute, stage) {
        mapView.overlays.clear()

        val activeAlpha = 255
        val dimAlpha = 90

        routeToPickup?.let { points ->
            pickupLine.setPoints(points)
            pickupLine.outlinePaint.color = KilivanaGreen.toArgb()
            pickupLine.outlinePaint.alpha = if (stage == TripStage.HEADING_TO_PICKUP) activeAlpha else dimAlpha
            mapView.overlays.add(pickupLine)
        }
        displayedDropoffRoute?.let { points ->
            dropoffLine.setPoints(points)
            dropoffLine.outlinePaint.color = KilivanaNotificationRed.toArgb()
            dropoffLine.outlinePaint.alpha = if (stage == TripStage.IN_TRANSIT) activeAlpha else dimAlpha
            mapView.overlays.add(dropoffLine)
        }

        mapView.overlays.add(pickupMarker)
        mapView.overlays.add(dropoffMarker)

        location?.let { fix ->
            userMarker.position = GeoPoint(fix.latitude, fix.longitude)
            mapView.overlays.add(userMarker)
        }

        // Frame the whole route once we have everything, so the driver sees
        // themselves, the farmer and the buyer all at once
        if (!hasFramedRoute && location != null && routeToPickup != null) {
            val fix = location!!
            val box = BoundingBox.fromGeoPoints(
                listOf(GeoPoint(fix.latitude, fix.longitude), pickupPoint, dropoffPoint)
            )
            mapView.zoomToBoundingBox(box, true, 140)
            hasFramedRoute = true
        }

        mapView.invalidate()
    }

    val distanceLabel = when (stage) {
        TripStage.HEADING_TO_PICKUP -> routeToPickup?.let {
            "%.1f km to farmer".format(RouteRepository.routeLengthKm(it))
        } ?: "Finding route to farmer…"
        TripStage.IN_TRANSIT -> displayedDropoffRoute?.let {
            "%.1f km to buyer".format(RouteRepository.routeLengthKm(it))
        } ?: "Finding route to buyer…"
        TripStage.DELIVERED -> "Delivered"
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        // Top bar, floating over the map (this screen keeps dark status bar icons)
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
                    done = stage != TripStage.HEADING_TO_PICKUP,
                    current = false,
                    modifier = Modifier.weight(1f)
                )
                TripStep(
                    label = "In Transit",
                    done = stage == TripStage.DELIVERED,
                    current = stage == TripStage.IN_TRANSIT,
                    modifier = Modifier.weight(1f)
                )
                TripStep(
                    label = "Delivered",
                    done = false,
                    current = stage == TripStage.DELIVERED,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            when (stage) {
                TripStage.HEADING_TO_PICKUP -> KilivanaButton(
                    text = "Mark as Picked Up",
                    onClick = { stage = TripStage.IN_TRANSIT }
                )
                TripStage.IN_TRANSIT -> KilivanaButton(
                    text = "Confirm Delivery (OTP)",
                    onClick = {
                        otpError = null
                        showOtpDialog = true
                    }
                )
                TripStage.DELIVERED -> Unit
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
                    stage = TripStage.DELIVERED
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
