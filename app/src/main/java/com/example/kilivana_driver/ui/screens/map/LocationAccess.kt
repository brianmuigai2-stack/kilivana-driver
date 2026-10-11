package com.example.kilivana_driver.ui.screens.map

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Everything the map needs before it can show a live position.
 *
 * [ready] is true only when the driver has granted location permission
 * AND the phone's location switch is on — the map screens show a
 * prompt card instead of the map until that's the case.
 */
class LocationAccess(
    val hasPermission: Boolean,
    val servicesOn: Boolean,
    val permanentlyDenied: Boolean,
    val requestPermission: () -> Unit,
    val openAppSettings: () -> Unit,
    val openLocationSettings: () -> Unit
) {

    val ready: Boolean
        get() = hasPermission && servicesOn
}

/**
 * Tracks location permission and the location switch, re-checking both
 * when the screen resumes so a driver who flipped the GPS switch in
 * settings doesn't come back to a stale "location is off" card.
 *
 * [permanentlyDenied] distinguishes "don't ask again" (send the driver
 * to app settings) from a normal denial (ask again).
 */
@Composable
fun rememberLocationAccess(): LocationAccess {

    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            context.hasLocationPermission()
        )
    }

    var servicesOn by remember {
        mutableStateOf(
            context.isLocationServicesOn()
        )
    }

    var permanentlyDenied by remember {
        mutableStateOf(false)
    }

    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->

            hasPermission =
                results.values.any { it }

            if (!hasPermission) {

                val activity =
                    context.findActivity()

                permanentlyDenied =
                    activity != null &&
                        !ActivityCompat.shouldShowRequestPermissionRationale(
                            activity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
            }
        }

    val lifecycleOwner =
        context.findActivity()
            as? LifecycleOwner

    DisposableEffect(lifecycleOwner) {

        val observer =
            LifecycleEventObserver { _, event ->

                if (
                    event ==
                    Lifecycle.Event.ON_RESUME
                ) {

                    hasPermission =
                        context.hasLocationPermission()

                    servicesOn =
                        context.isLocationServicesOn()
                }
            }

        lifecycleOwner
            ?.lifecycle
            ?.addObserver(observer)

        onDispose {
            lifecycleOwner
                ?.lifecycle
                ?.removeObserver(observer)
        }
    }

    return LocationAccess(
        hasPermission = hasPermission,
        servicesOn = servicesOn,
        permanentlyDenied = permanentlyDenied,
        requestPermission = {
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        },
        openAppSettings = {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts(
                        "package",
                        context.packageName,
                        null
                    )
                )
            )
        },
        openLocationSettings = {
            context.startActivity(
                Intent(
                    Settings.ACTION_LOCATION_SOURCE_SETTINGS
                )
            )
        }
    )
}
