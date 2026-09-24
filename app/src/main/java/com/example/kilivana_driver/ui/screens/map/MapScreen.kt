package com.example.kilivana_driver.ui.screens.map

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.net.Uri
import android.provider.Settings
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.example.kilivana_driver.ui.components.KilivanaButton
import com.example.kilivana_driver.ui.theme.KilivanaGreen
import com.example.kilivana_driver.ui.theme.KilivanaGreenCard
import com.example.kilivana_driver.ui.theme.KilivanaGreenTint
import com.example.kilivana_driver.ui.theme.KilivanaTextMuted
import com.example.kilivana_driver.ui.theme.KilivanaTextPrimary
import com.example.kilivana_driver.ui.theme.KilivanaWhite
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

private const val NAIROBI_LAT = -1.286389
private const val NAIROBI_LON = 36.817223
private const val USER_ZOOM = 16.5

@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var hasPermission by remember { mutableStateOf(context.hasLocationPermission()) }
    var servicesOn by remember { mutableStateOf(context.isLocationServicesOn()) }
    var permanentlyDenied by remember { mutableStateOf(false) }
    var resumeTick by remember { mutableStateOf(0) }
    var followUser by remember { mutableStateOf(true) }
    var hasCentered by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermission = results.values.any { it }
        if (!hasPermission) {
            val activity = context.findActivity()
            permanentlyDenied = activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
        }
    }

    val location by rememberUserLocation(enabled = hasPermission && servicesOn)

    // --- Map view and overlays (created once) ---
    val mapView = remember {
        Configuration.getInstance().apply {
            load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            userAgentValue = context.packageName
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            minZoomLevel = 4.0
            maxZoomLevel = 19.5
            controller.setZoom(14.0)
            controller.setCenter(GeoPoint(NAIROBI_LAT, NAIROBI_LON))
            setOnTouchListener { _, event ->
                // Dragging the map means the driver wants to look around
                if (event.action == MotionEvent.ACTION_MOVE) followUser = false
                false
            }
        }
    }

    val accuracyCircle = remember {
        Polygon().apply {
            fillPaint.color = 0x223B82F6
            outlinePaint.color = 0x553B82F6
            outlinePaint.strokeWidth = 2f
        }
    }

    val userMarker = remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = userDotDrawable(context)
            setOnMarkerClickListener(Marker.OnMarkerClickListener { _, _ -> true })
        }
    }

    // Map lifecycle + re-check permission / GPS whenever the app comes back
    val lifecycleOwner = context as? LifecycleOwner
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    mapView.onResume()
                    resumeTick++
                }
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose {
            lifecycleOwner?.lifecycle?.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    LaunchedEffect(resumeTick) {
        hasPermission = context.hasLocationPermission()
        servicesOn = context.isLocationServicesOn()
    }

    // Move the blue dot (and follow the driver) on every new fix
    LaunchedEffect(location) {
        val fix = location ?: return@LaunchedEffect
        val point = GeoPoint(fix.latitude, fix.longitude)

        if (!mapView.overlays.contains(accuracyCircle)) {
            mapView.overlays.add(accuracyCircle)
            mapView.overlays.add(userMarker)
        }
        userMarker.position = point
        accuracyCircle.setPoints(
            Polygon.pointsAsCircle(point, max(fix.accuracy.toDouble(), 5.0))
        )

        if (!hasCentered) {
            mapView.controller.setZoom(USER_ZOOM)
            mapView.controller.setCenter(point)
            hasCentered = true
        } else if (followUser) {
            mapView.controller.animateTo(point)
        }
        mapView.invalidate()
    }

    // --- UI ---
    val current = location

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        if (hasPermission && servicesOn) {
            LocationStatusCard(
                location = current,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(16.dp)
            )
            if (current != null) {
                RecenterButton(
                    following = followUser,
                    onClick = {
                        followUser = true
                        mapView.controller.setZoom(USER_ZOOM)
                        mapView.controller.animateTo(GeoPoint(current.latitude, current.longitude))
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                )
            }
        }

        if (!hasPermission) {
            if (permanentlyDenied) {
                LocationPromptCard(
                    title = "Location permission needed",
                    message = "Location is blocked for Kilivana. Open Settings, then Permissions, then Location, and allow it so we can show where you are.",
                    buttonText = "Open settings",
                    onClick = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                )
            } else {
                LocationPromptCard(
                    title = "Show where you are",
                    message = "Kilivana uses your phone's GPS to place you on the map. Your location is only read while the app is open.",
                    buttonText = "Allow location",
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                )
            }
        } else if (!servicesOn) {
            LocationPromptCard(
                title = "Location is turned off",
                message = "Turn on your phone's location (GPS) so we can find you on the map.",
                buttonText = "Turn on location",
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            )
        }

        // OpenStreetMap requires this attribution
        Text(
            text = "© OpenStreetMap contributors",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 6.dp)
                .background(KilivanaWhite.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp),
            color = KilivanaTextMuted,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun LocationStatusCard(
    location: Location?,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .shadow(3.dp, shape)
            .clip(shape)
            .background(KilivanaWhite)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(KilivanaGreenTint),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = KilivanaGreen,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "Your location",
                color = KilivanaTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (location == null) {
                    "Finding you…"
                } else {
                    "%.5f, %.5f · ±%d m".format(
                        Locale.US,
                        location.latitude,
                        location.longitude,
                        location.accuracy.roundToInt()
                    )
                },
                color = KilivanaTextMuted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun RecenterButton(
    following: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(52.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(if (following) KilivanaGreenCard else KilivanaWhite)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.MyLocation,
            contentDescription = "Centre on my location",
            tint = if (following) KilivanaWhite else KilivanaGreenCard,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun LocationPromptCard(
    title: String,
    message: String,
    buttonText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .shadow(6.dp, shape)
            .clip(shape)
            .background(KilivanaWhite)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(KilivanaGreenTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = KilivanaGreen,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                color = KilivanaTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = message,
            color = KilivanaTextMuted,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        KilivanaButton(text = buttonText, onClick = onClick)
    }
}

/** Blue dot with a white ring: the classic "you are here" marker. */
private fun userDotDrawable(context: Context): Drawable {
    val density = context.resources.displayMetrics.density
    val size = (26 * density).toInt()
    return GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(0xFF3B82F6.toInt())
        setStroke((4 * density).toInt(), android.graphics.Color.WHITE)
        setSize(size, size)
    }
}
