package com.github.phoswald.whereyoubeen.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect

/** The notification shows that tracking runs in the background; tracking works without it too. */
private val permissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.POST_NOTIFICATIONS,
)

/** Whether precise location is granted, and a function that asks the user for the permissions. */
class LocationPermissionState(val granted: Boolean, val request: () -> Unit)

/** If [requestOnStart] is true, asks right away unless all permissions are already granted. */
@Composable
fun rememberLocationPermission(requestOnStart: Boolean): LocationPermissionState {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasLocationPermission(context)) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        granted = hasLocationPermission(context)
    }
    // The permission may also have been changed in the system settings meanwhile
    LifecycleResumeEffect(Unit) {
        granted = hasLocationPermission(context)
        onPauseOrDispose {}
    }
    LaunchedEffect(Unit) {
        val allGranted = permissions.all {
            context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }
        if (requestOnStart && !allGranted) {
            launcher.launch(permissions)
        }
    }
    return LocationPermissionState(granted) { launcher.launch(permissions) }
}

private fun hasLocationPermission(context: Context): Boolean =
    context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
