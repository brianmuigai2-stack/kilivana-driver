package com.example.kilivana_driver.ui.screens.jobs

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
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
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kilivana_driver.data.model.TrackingHistoryPoint
import com.example.kilivana_driver.data.model.Job
import com.example.kilivana_driver.data.routing.RouteRepository
import com.example.kilivana_driver.ui.components.KilivanaButton
import com.example.kilivana_driver.ui.screens.map.CarMarker
import com.example.kilivana_driver.ui.screens.map.GeoPoint
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
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import com.mapbox.maps.plugin.Plugin
import com.mapbox.maps.plugin.annotation.AnnotationConfig
import com.mapbox.maps.plugin.annotation.AnnotationPlugin
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class UiTripStage { HEADING_TO_PICKUP, IN_TRANSIT, DELIVERED }

private const val DRIVER_BLUE = 0xFF3B82F6.toInt()
private const val FARMER_GREEN = 0xFF2E7D32.toInt()
private const val BUYER_RED = 0xFFE53935.toInt()
private const val ROUTE_LINE = 0xFF1F6B2E.toInt()
private const val PREVIEW_LINE = 0xFF8A94A6.toInt()
private const val TRACKING_LINE = 0xFF3B82F6.toInt()
private const val ROUTE_CAMERA_PITCH = 45.0

/** Shows the active trip map, tracking history and pickup/delivery controls, forwarding completion to the caller. */
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
        tripViewModel.startTrip()
    }

    // Collect state from ViewModel
    val tripStage by tripViewModel.tripStage.collectAsStateWithLifecycle()
    val isBusy by tripViewModel.isBusy.collectAsStateWithLifecycle()
    val lastError by tripViewModel.lastError.collectAsStateWithLifecycle()
    val routePoints by tripViewModel.routePoints.collectAsStateWithLifecycle()
    val usingEstimatedRoute by tripViewModel.usingEstimatedRoute.collectAsStateWithLifecycle()
    val distanceLabel by tripViewModel.distanceLabel.collectAsStateWithLifecycle()
    val queueSize by tripViewModel.queueSize.collectAsStateWithLifecycle()
    val trackingPoints by tripViewModel.trackingPoints.collectAsStateWithLifecycle()
    val isSubmittingProof by tripViewModel.isSubmittingProof.collectAsStateWithLifecycle()
    val proofError by tripViewModel.proofError.collectAsStateWithLifecycle()
    val proofFiled by tripViewModel.proofFiled.collectAsStateWithLifecycle()

    // Convert backend TripStage to UI stage
    val uiStage = remember { mutableStateOf(UiTripStage.HEADING_TO_PICKUP) }
    var hasFramedRoute by remember { mutableStateOf(false) }
    var isCameraFollowing by remember { mutableStateOf(true) }
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
        isCameraFollowing = true
    }

    // GPS location stream
    val location by rememberUserLocation(enabled = true)

    // Fixed points
    val pickupPoint = remember(job.id) { GeoPoint(job.pickupLat, job.pickupLon) }
    val dropoffPoint = remember(job.id) { GeoPoint(job.dropoffLat, job.dropoffLon) }
    val targetPoint = when (uiStage.value) {
        UiTripStage.HEADING_TO_PICKUP -> pickupPoint
        UiTripStage.IN_TRANSIT -> dropoffPoint
        UiTripStage.DELIVERED -> dropoffPoint
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapView(context, MapInitOptions(context))
    }
    var styleLoaded by remember { mutableStateOf(false) }
    var routeMapAnnotations by remember { mutableStateOf<RouteMapAnnotations?>(null) }
    val carMarker = remember { CarMarker() }

    DisposableEffect(mapView) {
        mapView.getMapboxMap().loadStyleUri(Style.STANDARD) {
            styleLoaded = true
            val center = Point.fromLngLat(pickupPoint.longitude, pickupPoint.latitude)
            mapView.getMapboxMap().setCamera(
                CameraOptions.Builder()
                    .center(center)
                    .zoom(12.0)
                    .pitch(30.0)
                    .build()
            )
        }

        onDispose {
            mapView.onDestroy()
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }

        // Any touch on the map means the user wants to control the camera
        // manually — stop auto-following so zoom/pan isn't overridden by GPS.
        val touchListener = View.OnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                isCameraFollowing = false
            }
            false
        }
        mapView.setOnTouchListener(touchListener)

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.setOnTouchListener(null)
            mapView.onStop()
        }
    }

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

    LaunchedEffect(styleLoaded, mapView) {
        if (!styleLoaded || routeMapAnnotations != null) return@LaunchedEffect
        val annotationPlugin = mapView.getPlugin<AnnotationPlugin>(Plugin.MAPBOX_ANNOTATION_PLUGIN_ID)
            ?: return@LaunchedEffect
        routeMapAnnotations = RouteMapAnnotations(
            stopPointManager = annotationPlugin.createPointAnnotationManager(AnnotationConfig()),
            vehiclePointManager = annotationPlugin.createPointAnnotationManager(AnnotationConfig()),
            lineManager = annotationPlugin.createPolylineAnnotationManager(AnnotationConfig())
        ).also { carMarker.bind(it.vehiclePointManager) }
    }

    DisposableEffect(routeMapAnnotations) {
        val annotations = routeMapAnnotations ?: return@DisposableEffect onDispose {}
        onDispose {
            carMarker.cancel()
            annotations.destroy()
        }
    }

    LaunchedEffect(routeMapAnnotations, pickupPoint, dropoffPoint, trackingPoints) {
        routeMapAnnotations?.drawPoints(
            pickupPoint = pickupPoint,
            dropoffPoint = dropoffPoint,
            trackingPoints = trackingPoints
        )
    }

    LaunchedEffect(routeMapAnnotations, routePoints, previewRouteToBuyer, trackingPoints) {
        routeMapAnnotations?.drawLines(
            activeRoute = routePoints,
            previewRoute = previewRouteToBuyer,
            trackingPoints = trackingPoints
        )
    }

    // Update car marker position and heading
    LaunchedEffect(location, tripViewModel) {
        val fix = location ?: return@LaunchedEffect
        val vehiclePos = GeoPoint(fix.latitude, fix.longitude)
        val heading = headingTracker.update(fix, targetPoint)
        tripViewModel.setCurrentHeading(heading)
        carMarker.update(vehiclePos.toMapboxPoint(), heading)
    }

    LaunchedEffect(location, uiStage.value) {
        val fix = location ?: return@LaunchedEffect
        val target = when (uiStage.value) {
            UiTripStage.HEADING_TO_PICKUP -> pickupPoint
            UiTripStage.IN_TRANSIT -> dropoffPoint
            UiTripStage.DELIVERED -> dropoffPoint
        }

        if (isCameraFollowing) {
            mapView.getMapboxMap().setCamera(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(fix.longitude, fix.latitude))
                    .zoom(15.0)
                    .pitch(ROUTE_CAMERA_PITCH)
                    .build()
            )

            if (!hasFramedRoute) {
                mapView.getMapboxMap().setCamera(
                    CameraOptions.Builder()
                        .center(Point.fromLngLat((fix.longitude + target.longitude) / 2.0, (fix.latitude + target.latitude) / 2.0))
                        .zoom(12.5)
                        .pitch(ROUTE_CAMERA_PITCH)
                        .build()
                )
                hasFramedRoute = true
            }
        }
    }

    var showOtpDialog by remember { mutableStateOf(false) }
    var showProofDialog by remember { mutableStateOf(false) }
    var otpError by remember { mutableStateOf<String?>(null) }
    var deliveryOtp by remember { mutableStateOf("") }

    LaunchedEffect(proofFiled) {
        if (proofFiled) onDeliveryComplete()
    }

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

        // Recenter button (top-right), only shown when not already following
        if (!isCameraFollowing) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 12.dp, end = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(2.dp, CircleShape)
                        .clip(CircleShape)
                        .background(KilivanaWhite)
                        .clickable(onClick = {
                            isCameraFollowing = true
                            hasFramedRoute = true
                            location?.let { fix ->
                                mapView.getMapboxMap().setCamera(
                                    CameraOptions.Builder()
                                        .center(Point.fromLngLat(fix.longitude, fix.latitude))
                                        .zoom(15.0)
                                        .pitch(ROUTE_CAMERA_PITCH)
                                        .build()
                                )
                            }
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Recenter on my location",
                        tint = KilivanaTextPrimary,
                        modifier = Modifier.size(24.dp)
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
                if (entered.isNotBlank()) {
                    deliveryOtp = entered
                    showOtpDialog = false
                    otpError = null
                    showProofDialog = true
                } else {
                    otpError = "Enter the buyer's delivery code."
                }
            },
            onDismiss = {
                showOtpDialog = false
                otpError = null
            }
        )
    }

    if (showProofDialog) {
        DeliveryProofDialog(
            isSubmitting = isSubmittingProof,
            error = proofError,
            onSubmit = { recipientName, photoUrl, signatureUrl ->
                tripViewModel.fileDeliveryProof(
                    otp = deliveryOtp,
                    recipientName = recipientName,
                    photoUrl = photoUrl,
                    signatureUrl = signatureUrl
                )
            },
            onDismiss = {
                if (!isSubmittingProof) {
                    showProofDialog = false
                    deliveryOtp = ""
                }
            }
        )
    }
}

private class RouteMapAnnotations(
    val stopPointManager: PointAnnotationManager,
    val vehiclePointManager: PointAnnotationManager,
    private val lineManager: PolylineAnnotationManager
) {
    private val pickupIcon by lazy { createCircleBitmap(FARMER_GREEN) }
    private val dropoffIcon by lazy { createCircleBitmap(BUYER_RED) }
    private val trackingIcon by lazy { createCircleBitmap(DRIVER_BLUE, radius = 9f) }

    /** Replaces stop annotations with pickup, dropoff and the latest tracking ping when available. */
    fun drawPoints(
        pickupPoint: GeoPoint,
        dropoffPoint: GeoPoint,
        trackingPoints: List<TrackingHistoryPoint>
    ) {
        stopPointManager.deleteAll()
        stopPointManager.create(
            PointAnnotationOptions()
                .withPoint(pickupPoint.toMapboxPoint())
                .withIconImage(pickupIcon)
                .withIconAnchor(IconAnchor.BOTTOM)
                .withTextField("Pickup")
                .withTextAnchor(TextAnchor.TOP)
                .withTextOffset(listOf(0.0, 0.4))
                .withTextSize(12.0)
                .withTextColor(FARMER_GREEN)
                .withTextHaloColor(android.graphics.Color.WHITE)
                .withTextHaloWidth(1.5)
        )
        stopPointManager.create(
            PointAnnotationOptions()
                .withPoint(dropoffPoint.toMapboxPoint())
                .withIconImage(dropoffIcon)
                .withIconAnchor(IconAnchor.BOTTOM)
                .withTextField("Dropoff")
                .withTextAnchor(TextAnchor.TOP)
                .withTextOffset(listOf(0.0, 0.4))
                .withTextSize(12.0)
                .withTextColor(BUYER_RED)
                .withTextHaloColor(android.graphics.Color.WHITE)
                .withTextHaloWidth(1.5)
        )

        trackingPoints.lastOrNull()?.let { latest ->
            stopPointManager.create(
                PointAnnotationOptions()
                    .withPoint(Point.fromLngLat(latest.longitude, latest.latitude))
                    .withIconImage(trackingIcon)
                    .withIconAnchor(IconAnchor.CENTER)
                    .withTextField("Last ping")
                    .withTextAnchor(TextAnchor.BOTTOM)
                    .withTextOffset(listOf(0.0, -0.35))
                    .withTextSize(11.0)
                    .withTextColor(DRIVER_BLUE)
                    .withTextHaloColor(android.graphics.Color.WHITE)
                    .withTextHaloWidth(1.5)
            )
        }
    }

    /** Replaces route lines with preview, active and tracking paths that each contain at least two points. */
    fun drawLines(
        activeRoute: List<GeoPoint>?,
        previewRoute: List<GeoPoint>?,
        trackingPoints: List<TrackingHistoryPoint>
    ) {
        lineManager.deleteAll()
        previewRoute?.takeIf { it.size >= 2 }?.let {
            lineManager.createPolyline(it, PREVIEW_LINE, width = 3.0, opacity = 0.55)
        }
        activeRoute?.takeIf { it.size >= 2 }?.let {
            lineManager.createPolyline(it, ROUTE_LINE, width = 5.0, opacity = 0.95)
        }
        trackingPoints
            .map { GeoPoint(it.latitude, it.longitude) }
            .takeIf { it.size >= 2 }
            ?.let { lineManager.createPolyline(it, TRACKING_LINE, width = 3.0, opacity = 0.65) }
    }

    /** Releases the stop, vehicle and route annotation managers. */
    fun destroy() {
        stopPointManager.onDestroy()
        vehiclePointManager.onDestroy()
        lineManager.onDestroy()
    }

    /** Creates a route polyline with the supplied color, width and opacity, using rounded joins. */
    private fun PolylineAnnotationManager.createPolyline(
        points: List<GeoPoint>,
        color: Int,
        width: Double,
        opacity: Double
    ) {
        create(
            PolylineAnnotationOptions()
                .withPoints(points.map { it.toMapboxPoint() })
                .withLineColor(color)
                .withLineWidth(width)
                .withLineOpacity(opacity)
                .withLineJoin(LineJoin.ROUND)
        )
    }
}

/** Creates a circular marker bitmap with the supplied fill color and radius, a white border and a shadow. */
private fun createCircleBitmap(
    color: Int,
    radius: Float = 13f
): Bitmap {
    val size = 48
    val center = size / 2f
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.argb(45, 0, 0, 0)
        style = Paint.Style.FILL
    }

    canvas.drawCircle(center, center + 2f, radius + 2f, shadow)
    canvas.drawCircle(center, center, radius, fill)
    canvas.drawCircle(center, center, radius, stroke)
    return bitmap
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
                    text = "Ask the buyer for their delivery code. You'll add delivery evidence next.",
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
private fun DeliveryProofDialog(
    isSubmitting: Boolean,
    error: String?,
    onSubmit: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var recipientName by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf("") }
    var signatureUrl by remember { mutableStateOf("") }
    val hasEvidence = photoUrl.isNotBlank() || signatureUrl.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delivery evidence") },
        text = {
            Column {
                Text(
                    text = "The delivery code will be checked when you submit the proof.",
                    color = KilivanaTextMuted,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = recipientName,
                    onValueChange = { recipientName = it },
                    label = { Text("Recipient name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = photoUrl,
                    onValueChange = { photoUrl = it },
                    label = { Text("Evidence photo URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = signatureUrl,
                    onValueChange = { signatureUrl = it },
                    label = { Text("Signature URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = error, color = KilivanaNotificationRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(recipientName, photoUrl, signatureUrl) },
                enabled = !isSubmitting && hasEvidence
            ) {
                Text(
                    text = if (isSubmitting) "Submitting..." else "Submit proof",
                    color = KilivanaGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Cancel", color = KilivanaTextMuted)
            }
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

/** Previews the trip route screen with a sample job and inactive navigation callbacks. */
@Preview(showSystemUi = true)
@Composable
private fun JobRouteScreenPreview() {
    KilivanaTheme {
        JobRouteScreen(job = sampleJobs().first(), onBack = {}, onDeliveryComplete = {})
    }
}
