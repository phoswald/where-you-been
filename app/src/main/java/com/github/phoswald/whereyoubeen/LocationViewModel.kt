package com.github.phoswald.whereyoubeen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

sealed interface LocationState {
    data object NoPermission : LocationState
    data object Disabled : LocationState
    data class Waiting(val time: Instant) : LocationState
    data class Available(val latitude: Double, val longitude: Double, val time: Instant) : LocationState
}

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val gps = GpsLocation(application)

    /** Polls a GPS location every [interval], only while collected (i.e. while the UI is visible). */
    val state: StateFlow<LocationState> = flow {
        while (true) {
            val start = TimeSource.Monotonic.markNow()
            emit(
                when {
                    !gps.hasPermission() -> LocationState.NoPermission
                    !gps.isEnabled() -> LocationState.Disabled
                    else -> withTimeoutOrNull(interval) { gps.currentLocation() }
                        ?.let { LocationState.Available(it.latitude, it.longitude, Instant.ofEpochMilli(it.time)) }
                        ?: LocationState.Waiting(Instant.now())
                }
            )
            delay(interval - start.elapsedNow())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocationState.Waiting(Instant.now()))

    private companion object {
        val interval = 10.seconds
    }
}
