package com.github.phoswald.whereyoubeen.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import com.github.phoswald.whereyoubeen.domain.GeoLocation
import com.github.phoswald.whereyoubeen.domain.LocationSource
import com.github.phoswald.whereyoubeen.domain.LocationStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.TimeSource

/** Location from the GPS provider only (no network/WiFi-based locations). */
class GpsLocationSource(private val context: Context) : LocationSource {

    private val locationManager = context.getSystemService(LocationManager::class.java)

    override fun observe(interval: Duration): Flow<LocationStatus> = flow {
        while (true) {
            val start = TimeSource.Monotonic.markNow()
            emit(readOnce(timeout = interval))
            delay(interval - start.elapsedNow())
        }
    }

    private suspend fun readOnce(timeout: Duration): LocationStatus {
        if (!hasPermission()) {
            return LocationStatus.NoPermission
        }
        if (!isEnabled()) {
            return LocationStatus.Disabled
        }
        val location = withTimeoutOrNull(timeout) { currentLocation() }
        if (location == null) {
            return LocationStatus.Waiting(Instant.now())
        }
        return LocationStatus.Available(
            GeoLocation(location.latitude, location.longitude, Instant.ofEpochMilli(location.time))
        )
    }

    private fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    private fun isEnabled(): Boolean = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

    /** Requests a fresh GPS location, returns null if none could be obtained. Requires [hasPermission]. */
    @SuppressLint("MissingPermission")
    private suspend fun currentLocation(): Location? = suspendCancellableCoroutine { cont ->
        val signal = CancellationSignal()
        cont.invokeOnCancellation { signal.cancel() }
        locationManager.getCurrentLocation(
            LocationManager.GPS_PROVIDER, signal, context.mainExecutor
        ) { location -> cont.resume(location) }
    }
}
