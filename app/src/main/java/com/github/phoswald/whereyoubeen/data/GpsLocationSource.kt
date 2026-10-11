package com.github.phoswald.whereyoubeen.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import android.util.Log
import com.github.phoswald.whereyoubeen.domain.GeoLocation
import com.github.phoswald.whereyoubeen.domain.LocationSource
import com.github.phoswald.whereyoubeen.domain.LocationStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import kotlin.time.Duration

/**
 * Locations with a worse reported accuracy are dropped (reported as [LocationStatus.Inaccurate]).
 * The accuracy is a 68 % confidence radius, and outliers (mostly first locations after the
 * receiver was switched off) usually report a large one.
 */
private const val MAX_ACCURACY_METERS = 10f
/** Short enough to keep the GPS receiver running continuously. */
private const val UPDATE_INTERVAL_MILLIS = 1_000L
private const val TAG = "GpsLocationSource"

/**
 * Location from the GPS provider only (no network/WiFi-based locations).
 *
 * Keeps the GPS receiver running with an update every second, and emits only the most accurate
 * location of every [interval]. With longer update intervals, the platform switches the receiver
 * off between fixes, and the first fix after restarting it is usually inaccurate (few satellites,
 * no history), especially near buildings. A continuously running receiver keeps its satellite lock
 * and converges to a much better accuracy, at the cost of more battery.
 * Locations with poor reported accuracy are dropped and reported as [LocationStatus.Inaccurate].
 */
class GpsLocationSource(private val context: Context) : LocationSource {

    private val locationManager = context.getSystemService(LocationManager::class.java)

    override fun observe(interval: Duration): Flow<LocationStatus> = flow {
        while (true) {
            if (!hasPermission()) {
                emit(LocationStatus.NoPermission)
            } else if (!isEnabled()) {
                emit(LocationStatus.Disabled)
            } else {
                emitAll(updates(interval)) // completes when GPS is switched off
                continue
            }
            delay(interval) // check again later
        }
    }

    /** Emits [LocationStatus.Waiting] whenever no location arrived for two intervals. */
    private fun updates(interval: Duration): Flow<LocationStatus> = channelFlow {
        send(LocationStatus.Waiting(Instant.now()))
        val locations = locations(interval).produceIn(this)
        while (true) {
            val result = withTimeoutOrNull(interval * 2) { locations.receiveCatching() }
            when {
                result == null -> send(LocationStatus.Waiting(Instant.now()))
                result.isClosed -> break
                else -> send(result.getOrThrow())
            }
        }
    }

    /** Requires [hasPermission]; completes when the GPS provider is disabled. */
    @SuppressLint("MissingPermission")
    private fun locations(interval: Duration): Flow<LocationStatus> = callbackFlow {
        val listener = object : LocationListener {
            // Only accessed on the main executor
            private var windowStart: Long? = null
            private var best: Location? = null

            override fun onLocationChanged(location: Location) {
                val start = windowStart ?: location.time.also { windowStart = it }
                if (location.hasAccuracy() && location.accuracy < (best?.accuracy ?: Float.MAX_VALUE)) {
                    best = location
                }
                if (location.time - start >= interval.inWholeMilliseconds) {
                    best?.let { trySend(toStatus(it)) }
                    windowStart = location.time
                    best = null
                }
            }

            override fun onProviderDisabled(provider: String) {
                close()
            }
        }
        val request = LocationRequest.Builder(UPDATE_INTERVAL_MILLIS)
            .setMinUpdateIntervalMillis(UPDATE_INTERVAL_MILLIS)
            .setQuality(LocationRequest.QUALITY_HIGH_ACCURACY)
            .build()
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER, request, context.mainExecutor, listener
        )
        awaitClose { locationManager.removeUpdates(listener) }
    }

    private fun toStatus(location: Location): LocationStatus =
        if (location.accuracy <= MAX_ACCURACY_METERS) {
            LocationStatus.Available(toGeoLocation(location))
        } else {
            Log.d(TAG, "dropped location, accuracy ${location.accuracy} m")
            LocationStatus.Inaccurate(Instant.ofEpochMilli(location.time), location.accuracy)
        }

    private fun toGeoLocation(location: Location) =
        GeoLocation(location.latitude, location.longitude, Instant.ofEpochMilli(location.time))

    private fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun isEnabled(): Boolean =
        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
}
