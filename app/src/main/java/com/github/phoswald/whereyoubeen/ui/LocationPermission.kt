package com.github.phoswald.whereyoubeen.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

private val locationPermissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

/**
 * Returns a function that asks the user for location permission.
 * If [requestOnStart] is true, asks right away unless the permission is already granted.
 */
@Composable
fun rememberLocationPermissionRequest(requestOnStart: Boolean): () -> Unit {
    val context = LocalContext.current
    // No result handling: the location source re-checks the permission on its next poll
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {}
    LaunchedEffect(Unit) {
        val granted = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        if (requestOnStart && !granted) {
            launcher.launch(locationPermissions)
        }
    }
    return { launcher.launch(locationPermissions) }
}
