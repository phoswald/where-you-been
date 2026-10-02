package com.github.phoswald.whereyoubeen

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Location from the GPS provider only (no network/WiFi-based locations). */
class GpsLocation(private val context: Context) {

    private val locationManager = context.getSystemService(LocationManager::class.java)

    fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    fun isEnabled(): Boolean = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

    /** Requests a fresh GPS location, returns null if none could be obtained. Requires [hasPermission]. */
    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): Location? = suspendCancellableCoroutine { cont ->
        val signal = CancellationSignal()
        cont.invokeOnCancellation { signal.cancel() }
        locationManager.getCurrentLocation(
            LocationManager.GPS_PROVIDER, signal, context.mainExecutor
        ) { location -> cont.resume(location) }
    }
}
