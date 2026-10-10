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
 * Locations with a worse reported accuracy are dropped. The accuracy is a 68 % confidence radius,
 * and outliers (mostly first locations after the receiver was switched off) usually report a large
 * one.
 */
private const val MAX_ACCURACY_METERS = 10f
private const val TAG = "GpsLocationSource"

/**
 * Location from the GPS provider only (no network/WiFi-based locations).
 *
 * Subscribes to periodic updates instead of requesting single locations: the platform then
 * schedules and duty-cycles the GPS receiver for the interval and wakes the app only to deliver a
 * location, which is the most battery-friendly way to get a GPS location every [interval].
 * Locations with poor reported accuracy are dropped.
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
    private fun locations(interval: Duration): Flow<LocationStatus.Available> = callbackFlow {
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (location.hasAccuracy() && location.accuracy <= MAX_ACCURACY_METERS) {
                    trySend(LocationStatus.Available(toGeoLocation(location)))
                } else {
                    Log.d(TAG, "dropped location, accuracy ${location.accuracy} m")
                }
            }

            override fun onProviderDisabled(provider: String) {
                close()
            }
        }
        val request = LocationRequest.Builder(interval.inWholeMilliseconds)
            .setMinUpdateIntervalMillis(interval.inWholeMilliseconds)
            .setQuality(LocationRequest.QUALITY_BALANCED_POWER_ACCURACY)
            .build()
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER, request, context.mainExecutor, listener
        )
        awaitClose { locationManager.removeUpdates(listener) }
    }

    private fun toGeoLocation(location: Location) =
        GeoLocation(location.latitude, location.longitude, Instant.ofEpochMilli(location.time))

    private fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun isEnabled(): Boolean =
        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
}
