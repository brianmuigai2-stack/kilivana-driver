package com.example.kilivana_driver.ui.screens.map

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.LocalShipping
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
import com.example.kilivana_driver.BuildConfig
import com.example.kilivana_driver.data.model.Job
import com.example.kilivana_driver.ui.components.KilivanaButton
import com.example.kilivana_driver.ui.theme.KilivanaGreen
import com.example.kilivana_driver.ui.theme.KilivanaGreenTint
import com.example.kilivana_driver.ui.theme.KilivanaTextMuted
import com.example.kilivana_driver.ui.theme.KilivanaTextPrimary
import com.example.kilivana_driver.ui.theme.KilivanaWhite
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.Plugin
import com.mapbox.maps.plugin.annotation.AnnotationConfig
import com.mapbox.maps.plugin.annotation.AnnotationPlugin
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager

private const val NAIROBI_LAT = -1.286389
private const val NAIROBI_LON = 36.817223
private const val USER_ZOOM = 17.25
private const val USER_PITCH = 45.0

/** Shows the driver location on a Mapbox map, handles location access and offers resuming an active job. */
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    activeJob: Job? = null,
    onResumeActiveJob: () -> Unit = {}
) {
    val context = LocalContext.current

    var hasPermission by remember { mutableStateOf(context.hasLocationPermission()) }
    var servicesOn by remember { mutableStateOf(context.isLocationServicesOn()) }
    var permanentlyDenied by remember { mutableStateOf(false) }
    var resumeTick by remember { mutableStateOf(0) }
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
    val lifecycleOwner = context as? LifecycleOwner

    val mapView = remember {
        MapView(context, MapInitOptions(context))
    }
    var styleLoaded by remember { mutableStateOf(false) }
    var currentLocationManager by remember { mutableStateOf<PointAnnotationManager?>(null) }
    val currentLocationMarker = remember { CurrentLocationMarker() }

    DisposableEffect(mapView) {
        mapView.getMapboxMap().loadStyleUri(Style.STANDARD) {
            styleLoaded = true
            mapView.getMapboxMap().setCamera(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(NAIROBI_LON, NAIROBI_LAT))
                    .zoom(14.0)
                    .pitch(30.0)
                    .build()
            )
        }

        onDispose {
            mapView.onDestroy()
        }
    }

    LaunchedEffect(styleLoaded, mapView) {
        if (!styleLoaded || currentLocationManager != null) return@LaunchedEffect
        val annotationPlugin = mapView.getPlugin<AnnotationPlugin>(Plugin.MAPBOX_ANNOTATION_PLUGIN_ID)
            ?: return@LaunchedEffect
        currentLocationManager = annotationPlugin
            .createPointAnnotationManager(AnnotationConfig())
            .also { currentLocationMarker.bind(it) }
    }

    DisposableEffect(currentLocationManager) {
        val manager = currentLocationManager ?: return@DisposableEffect onDispose {}
        onDispose {
            currentLocationMarker.cancel()
            manager.onDestroy()
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        if (lifecycleOwner == null) return@DisposableEffect onDispose {}

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_RESUME -> {
                    resumeTick++
                }
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onStop()
        }
    }

    LaunchedEffect(resumeTick) {
        hasPermission = context.hasLocationPermission()
        servicesOn = context.isLocationServicesOn()
    }

    LaunchedEffect(location, currentLocationManager) {
        val fix = location ?: return@LaunchedEffect
        currentLocationMarker.update(Point.fromLngLat(fix.longitude, fix.latitude))
    }

    LaunchedEffect(location, activeJob, hasCentered) {
        val fix = location ?: return@LaunchedEffect
        if (!hasCentered || activeJob == null) {
            mapView.getMapboxMap().setCamera(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(fix.longitude, fix.latitude))
                    .zoom(USER_ZOOM)
                    .pitch(USER_PITCH)
                    .build()
            )
            hasCentered = true
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    top = 16.dp,
                    start = 16.dp,
                    end = 16.dp,
                    bottom = if (activeJob != null) 110.dp else 16.dp
                )
                .clip(CircleShape)
                .background(KilivanaWhite)
                .clickable {
                    val fix = location
                    if (fix != null) {
                        mapView.getMapboxMap().setCamera(
                            CameraOptions.Builder()
                                .center(Point.fromLngLat(fix.longitude, fix.latitude))
                                .zoom(USER_ZOOM)
                                .pitch(USER_PITCH)
                                .build()
                        )
                    } else if (!hasPermission) {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }
                .padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.MyLocation,
                contentDescription = "Center map on my location",
                tint = KilivanaGreen
            )
        }

        if (activeJob != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(KilivanaWhite)
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.LocalShipping,
                        contentDescription = null,
                        tint = KilivanaGreen,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeJob.customer,
                            color = KilivanaTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = activeJob.pickupPlace,
                            color = KilivanaTextMuted,
                            fontSize = 12.sp
                        )
                    }
                    KilivanaButton(
                        text = "Resume",
                        onClick = onResumeActiveJob,
                        modifier = Modifier.width(120.dp)
                    )
                }
            }
        }

        if (!hasPermission || !servicesOn) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp)
                    .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Text(
                    text = "Location access required",
                    color = KilivanaTextPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (!hasPermission) {
                        "Allow location access so the app can show your position and route status."
                    } else {
                        "Turn on location services to continue."
                    },
                    color = KilivanaTextMuted,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .clickable {
                            if (!hasPermission) {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            } else {
                                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            }
                        }
                        .fillMaxWidth()
                        .background(KilivanaGreenTint, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = KilivanaGreen
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (!hasPermission) "Enable permissions" else "Open location settings",
                        color = KilivanaTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = KilivanaTextPrimary
                    )
                }
            }
        }
    }
}
