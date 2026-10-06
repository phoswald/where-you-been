package com.github.phoswald.whereyoubeen

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

private const val TAG = "LocationViewModel"

sealed interface LocationState {
    data object NoPermission : LocationState
    data object Disabled : LocationState
    data class Waiting(val time: Instant) : LocationState
    data class Available(val latitude: Double, val longitude: Double, val time: Instant) : LocationState
}

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val gps = GpsLocation(application)
    private val uploader = LocationUploader()

    /** Google ID token used to upload locations; null while signed out (nothing is uploaded). */
    @Volatile
    var idToken: String? = null

    private val _syncStatus = MutableStateFlow<Int?>(null)
    /** HTTP status code of the latest upload; null if no upload got a response yet. */
    val syncStatus: StateFlow<Int?> = _syncStatus.asStateFlow()

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
    }.onEach { if (it is LocationState.Available) upload(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocationState.Waiting(Instant.now()))

    /** Uploads in the background, so a slow server never delays polling. */
    private fun upload(location: LocationState.Available) {
        val idToken = idToken ?: return
        viewModelScope.launch {
            try {
                val status = uploader.upload(location, idToken)
                if (status !in 200..299) {
                    Log.w(TAG, "upload failed: HTTP $status")
                }
                _syncStatus.value = status
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "upload failed: $e")
            }
        }
    }

    private companion object {
        val interval = 10.seconds
    }
}
