package com.github.phoswald.whereyoubeen.domain

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

/**
 * App-wide location tracking: reads the location and uploads it. [run] is driven by the foreground
 * service, so it continues while the UI is not visible; the UI just observes the state flows.
 */
class LocationTracker(
    private val locationSource: LocationSource,
    private val syncLocation: SyncLocationUseCase,
) {

    private val _enabled = MutableStateFlow(true)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _location = MutableStateFlow<LocationStatus>(LocationStatus.Off)
    val location: StateFlow<LocationStatus> = _location.asStateFlow()

    /** Result of uploading the latest available location. */
    private val _sync = MutableStateFlow<SyncState>(SyncState.None)
    val sync: StateFlow<SyncState> = _sync.asStateFlow()

    fun setEnabled(enabled: Boolean) {
        _enabled.value = enabled
    }

    /** Tracks until tracking is disabled (returns then) or the caller is cancelled. */
    suspend fun run() {
        try {
            coroutineScope {
                val tracking = launch { track() }
                _enabled.first { !it }
                tracking.cancel()
            }
        } finally {
            _location.value = LocationStatus.Off
        }
    }

    /**
     * Uploads in its own collector of [location], so a slow server never delays reading the
     * location; locations arriving during an upload are skipped.
     */
    private suspend fun track() = coroutineScope {
        _location.value = LocationStatus.Waiting(Instant.now())
        launch {
            locationSource.observe(INTERVAL).collect { _location.value = it }
        }
        _location.filterIsInstance<LocationStatus.Available>().collect {
            val result = syncLocation(it.location)
            if (result != null) { // null: signed out, nothing uploaded
                _sync.value = result
            }
        }
    }

    private companion object {
        val INTERVAL = 1.minutes
    }
}
